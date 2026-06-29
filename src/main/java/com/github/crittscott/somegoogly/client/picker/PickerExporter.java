package com.github.crittscott.somegoogly.client.picker;

import com.github.crittscott.somegoogly.client.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.client.render.resolver.Resolvers;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.config.ModVersionLookup;
import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.eye.HeadInfo.HeadConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfigSet;
import com.github.crittscott.somegoogly.eye.HeadInfo.Variant;
import com.github.crittscott.somegoogly.eye.state.EyeAppearance;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Writes picker configs out as datapack JSON. Two entry points:
 * <ul>
 *   <li>{@link #export()} — the single committed mob, written into the single-player world
 *       ({@code world/datapacks/somegoogly-picker/data/<ns>/eyes/<entity>.json}) and {@code /reload}ed
 *       so it persists and re-syncs through the normal path.</li>
 *   <li>{@link #exportAll()} — a dump of <i>every</i> eye config into {@code <gameDir>/somegoogly-export/data/…}:
 *       the synced runtime state ({@link ClientEyeConfigs}) for untouched entities, overlaid with the
 *       picker's per-entity drafts ({@link PickerState#authoredConfigs()}) so a mob saved-but-never-exported
 *       is included too. For copying straight into the mod's {@code resources/}.</li>
 * </ul>
 *
 * <p>Both write the <b>complete, canonical</b> form — every field explicit, in the shipped field order,
 * with {@code age:"any"} (per-mob) and a version <i>range</i> ({@code [1.20.1,1.21)}). This is
 * deliberate: the files are meant to be dropped into the mod's source data, so they must not rely on the
 * loader's default-elision (the runtime codecs omit any field equal to its default, leaving a sparse
 * file whose meaning silently tracks whatever the code defaults later become). Writing in full pins the
 * authored values.
 */
public final class PickerExporter {

    private static final String DUMP_DIR = "somegoogly-export";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PACK_MCMETA =
            "{\n  \"pack\": {\n    \"pack_format\": 15,\n    \"description\": \"SomeGoogly picker output\"\n  }\n}\n";
    private static final String PACK_NAME = "somegoogly-picker";

    private PickerExporter() {
    }

    public static String export() {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) {
            return "Export needs single-player.";
        }
        ResourceLocation type = PickerState.targetType();
        if (type == null || PickerState.totalEyeCount() == 0) {
            return "Nothing committed to export.";
        }

        LivingEntity target = PickerState.target();
        Optional<String> version = ModVersionLookup.versionForNamespace(type.getNamespace());
        if (target == null || version.isEmpty()) {
            return "Export failed: couldn't resolve target mod version.";
        }

        // Draft tokens are already canonical (seeded/authored in the picker's enumeration vocabulary).
        JsonArray variants = variantsJson(PickerState.toConfig().variants, UnaryOperator.identity());
        if (variants.isEmpty()) {
            return "Nothing committed to export.";
        }
        JsonObject json = fileJson(entryJson(versionRange(version.get()), "any", true, variants));

        Path packDir = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK_NAME);
        Path eyesDir = packDir.resolve("data").resolve(type.getNamespace()).resolve("eyes");
        Path file = eyesDir.resolve(type.getPath() + ".json");

        try {
            Files.createDirectories(eyesDir);
            Path meta = packDir.resolve("pack.mcmeta");
            if (!Files.exists(meta)) {
                Files.writeString(meta, PACK_MCMETA);
            }
            Files.writeString(file, GSON.toJson(json) + "\n");
        } catch (IOException e) {
            return "Export failed: " + e.getMessage();
        }

        // Reload on the server thread so the datapack is re-read and re-synced to the client.
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload"));
        return "Exported " + type + " → " + PACK_NAME + ", reloading.";
    }

    /**
     * Dump every eye config to one canonical file per entity under
     * {@code <gameDir>/somegoogly-export/data/<ns>/eyes/<entity>.json}: the synced runtime state for
     * untouched entities, with each picker draft overlaid on its entity (so saved-but-not-exported mobs
     * are written too). Unlike the per-mob export this needs no committed picker target and triggers no
     * reload — it just captures what's authored/live so it can be copied into the mod's {@code resources/}.
     *
     * <p>The declared version range is re-synthesized from the currently-loaded version of each entity's
     * namespace ({@link #versionRange}); the original entry's declared range isn't preserved in the
     * runtime config, so it can't be recovered.
     */
    public static String exportAll() {
        Map<ResourceLocation, RuntimeConfigSet> synced = ClientEyeConfigs.all();
        Map<ResourceLocation, RuntimeConfig> drafts = PickerState.authoredConfigs();
        if (synced.isEmpty() && drafts.isEmpty()) {
            return "No eye configs loaded to export.";
        }

        // Every entity we have anything for: synced (shipped/loaded) state plus in-progress picker drafts.
        // A draft wins over the synced state for the same entity, so a mob saved-but-never-exported still
        // gets written — this is what makes exportall "export all", not just the last reloaded mob.
        Set<ResourceLocation> ids = new LinkedHashSet<>(synced.keySet());
        ids.addAll(drafts.keySet());

        Path root = Minecraft.getInstance().gameDirectory.toPath().resolve(DUMP_DIR);
        int files = 0;
        // Types whose model couldn't be resolved (so synced tokens were written verbatim, not canonicalized).
        int verbatim = 0;
        try {
            for (ResourceLocation id : ids) {
                Optional<String> version = ModVersionLookup.versionForNamespace(id.getNamespace());
                if (version.isEmpty()) {
                    continue; // namespace's mod isn't loaded; can't tag a version
                }
                String range = versionRange(version.get());
                RuntimeConfig draft = drafts.get(id);
                JsonObject json;
                if (draft != null) {
                    // Drafts are already canonical (seeded/authored in the picker's enumeration vocabulary).
                    json = draftFileJson(draft, range);
                } else {
                    // Synced (shipped) tokens may be legacy, so canonicalize them against the model — this
                    // migrates e.g. a reflection-model "head" to "#0". The model comes from a throwaway
                    // entity instance, so every config converts in one pass regardless of what's loaded.
                    UnaryOperator<String> canon = canonicalizer(id);
                    if (canon == null) {
                        verbatim++;
                        canon = UnaryOperator.identity();
                    }
                    json = setToConfigJson(synced.get(id), range, canon);
                }
                if (json == null) {
                    continue; // nothing usable for this entity
                }
                Path dir = root.resolve("data").resolve(id.getNamespace()).resolve("eyes");
                Files.createDirectories(dir);
                Files.writeString(dir.resolve(id.getPath() + ".json"), GSON.toJson(json) + "\n");
                files++;
            }
        } catch (IOException e) {
            return "Export-all failed: " + e.getMessage();
        }
        String note = verbatim > 0
                ? " (" + verbatim + " types' models couldn't be resolved — tokens left verbatim)"
                : "";
        return "Dumped " + files + " eye configs to " + root + note + " — copy data/ into resources/.";
    }

    /** A single-entry file for one authored draft (age {@code "any"}, like {@link #export()}), or null if empty. */
    private static JsonObject draftFileJson(RuntimeConfig draft, String versionRange) {
        JsonArray variants = variantsJson(draft.variants, UnaryOperator.identity());
        if (variants.isEmpty()) {
            return null;
        }
        return fileJson(entryJson(versionRange, "any", draft.isEnabled(), variants));
    }

    /**
     * A throwaway instance of an entity type, solely to reach its renderer/model — never added to the
     * world. Created on demand so token canonicalization doesn't depend on what's loaded near the player.
     * Players can't be constructed this way, so the local player stands in for {@code minecraft:player}.
     */
    private static LivingEntity sampleFor(ResourceLocation id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER))) {
            return mc.player;
        }
        ClientLevel level = mc.level;
        EntityType<?> type = level == null ? null : BuiltInRegistries.ENTITY_TYPE.get(id);
        if (type == null) {
            return null;
        }
        try {
            return type.create(level) instanceof LivingEntity living ? living : null;
        } catch (Throwable constructionFailed) {
            return null; // some types refuse a bare create(); their tokens stay verbatim
        }
    }

    /**
     * A token canonicalizer for one entity: resolves attach tokens to the model's enumeration vocabulary
     * via the same resolver the renderer uses. Returns {@code null} when the type's model can't be reached
     * (the caller then writes its tokens verbatim and counts it).
     */
    private static UnaryOperator<String> canonicalizer(ResourceLocation id) {
        LivingEntity sample = sampleFor(id);
        if (sample == null) {
            return null;
        }
        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(sample);
        if (!(renderer instanceof LivingEntityRenderer<?, ?> living)) {
            return null;
        }
        EntityModel<?> model = living.getModel();
        EyeAttachmentResolver resolver = Resolvers.forModel(model);
        if (resolver == null) {
            return null;
        }
        return token -> resolver.canonicalToken(model, token);
    }

    // --- Shared canonical serialization (complete fields, shipped order) ---

    private static JsonObject fileJson(JsonObject... entries) {
        JsonArray array = new JsonArray();
        for (JsonObject entry : entries) {
            array.add(entry);
        }
        JsonObject root = new JsonObject();
        root.add("entries", array);
        return root;
    }

    private static JsonObject entryJson(String versionRange, String age, boolean enabled, JsonArray variants) {
        JsonObject entry = new JsonObject();
        entry.addProperty("version", versionRange);
        entry.addProperty("age", age);
        entry.addProperty("enabled", enabled);
        entry.add("variants", variants);
        return entry;
    }

    /** Serialize a whole age-set as a multi-entry file, one entry per non-empty age config, or null. */
    private static JsonObject setToConfigJson(RuntimeConfigSet set, String versionRange, UnaryOperator<String> canon) {
        JsonArray entries = new JsonArray();
        addAgeEntry(entries, "adult", set.adult, versionRange, canon);
        addAgeEntry(entries, "baby", set.baby, versionRange, canon);
        addAgeEntry(entries, "any", set.any, versionRange, canon);
        if (entries.isEmpty()) {
            return null;
        }
        JsonObject root = new JsonObject();
        root.add("entries", entries);
        return root;
    }

    private static void addAgeEntry(JsonArray entries, String age, RuntimeConfig config, String versionRange,
                                    UnaryOperator<String> canon) {
        if (config == null) {
            return;
        }
        JsonArray variants = variantsJson(config.variants, canon);
        if (variants.isEmpty()) {
            return;
        }
        entries.add(entryJson(versionRange, age, config.isEnabled(), variants));
    }

    /** Serialize variants, mapping each head's attach token through {@code canon} (its canonical form). */
    private static JsonArray variantsJson(List<Variant> variants, UnaryOperator<String> canon) {
        JsonArray out = new JsonArray();
        if (variants == null) {
            return out;
        }
        for (Variant v : variants) {
            if (v == null || v.heads == null || v.heads.isEmpty()) {
                continue;
            }
            JsonArray heads = new JsonArray();
            for (HeadConfig h : v.heads) {
                if (h == null || h.eyes == null || h.eyes.isEmpty()) {
                    continue;
                }
                JsonArray eyes = new JsonArray();
                for (EyeDefinition def : h.eyes) {
                    eyes.add(eyeJson(def));
                }
                JsonObject head = new JsonObject();
                head.addProperty("attachPoint", canon.apply(h.attachPoint != null ? h.attachPoint : "head"));
                head.add("eyes", eyes);
                heads.add(head);
            }
            if (heads.isEmpty()) {
                continue; // skip arrangements with no usable eyes
            }
            JsonObject variant = new JsonObject();
            variant.addProperty("weight", round(v.weight()));
            variant.add("heads", heads);
            out.add(variant);
        }
        return out;
    }

    /** One eye object with every field written explicitly, in the shipped field order. */
    private static JsonObject eyeJson(EyeDefinition def) {
        EyePlacement p = def.placement();
        EyeAppearance a = def.appearance();
        JsonObject o = new JsonObject();
        o.add("position", vec3(p.position()));
        o.addProperty("eyeScale", round(p.eyeScale()));
        o.addProperty("irisScale", round(p.irisScale()));
        o.addProperty("inclination", round(p.inclination()));
        o.addProperty("azimuth", round(p.azimuth()));
        o.add("corneaColors", colors(a.cornea()));
        o.add("irisColors", colors(a.iris()));
        o.addProperty("glows", a.glow());
        o.addProperty("affectedByInvisibility", p.affectedByInvisibility());
        return o;
    }

    /**
     * Round to one part in a thousand. The {@code /sg} CLI parses inputs as {@code float}, so a typed
     * {@code 0.22} widens to {@code 0.2199999988079071} as a double; this snaps such float-widening noise
     * back to the authored value before it's written to the (human-edited) source data.
     */
    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private static JsonArray vec3(Vec3 v) {
        JsonArray array = new JsonArray();
        array.add(round(v.x));
        array.add(round(v.y));
        array.add(round(v.z));
        return array;
    }

    private static JsonArray colors(EyeColor c) {
        JsonArray array = new JsonArray();
        array.add(round(c.r()));
        array.add(round(c.g()));
        array.add(round(c.b()));
        return array;
    }

    /**
     * Turn a loaded version like {@code 1.20.1} into the range {@code [1.20.1,1.21)} — inclusive of the
     * loaded version, exclusive of the next minor — matching the shipped configs. Falls back to the exact
     * version (an exact-match entry) if it can't be parsed into at least major.minor.
     */
    private static String versionRange(String loaded) {
        String[] parts = loaded.split("\\.");
        if (parts.length >= 2) {
            try {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                return "[" + loaded + "," + major + "." + (minor + 1) + ")";
            } catch (NumberFormatException ignored) {
                // not numeric major.minor; fall through to an exact-match entry
            }
        }
        return loaded;
    }
}
