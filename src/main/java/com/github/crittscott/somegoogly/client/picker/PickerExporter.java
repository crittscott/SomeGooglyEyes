package com.github.crittscott.somegoogly.client.picker;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Writes picker configs out as datapack JSON. Two entry points:
 * <ul>
 *   <li>{@link #export()} — the single committed mob, written into the single-player world
 *       ({@code world/datapacks/somegoogly-picker/data/<ns>/eyes/<entity>.json}) and {@code /reload}ed
 *       so it persists and re-syncs through the normal path.</li>
 *   <li>{@link #exportAll()} — a plain dump of <i>every</i> eye config currently live in the running
 *       game (the synced {@link ClientEyeConfigs}), into {@code <gameDir>/somegoogly-export/data/…}.
 *       This captures the assembled runtime state regardless of the picker's one-mob draft, for copying
 *       straight into the mod's {@code resources/}.</li>
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

        JsonArray variants = variantsJson(PickerState.toConfig().variants);
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
     * Dump every eye config the client currently has (the synced runtime state) to one canonical file
     * per entity under {@code <gameDir>/somegoogly-export/data/<ns>/eyes/<entity>.json}. Unlike the
     * per-mob export this needs no committed picker target and triggers no reload — it just captures
     * what is live so it can be copied into the mod's {@code resources/}.
     *
     * <p>The declared version range is re-synthesized from the currently-loaded version of each entity's
     * namespace ({@link #versionRange}); the original entry's declared range isn't preserved in the
     * runtime config, so it can't be recovered.
     */
    public static String exportAll() {
        Map<ResourceLocation, RuntimeConfigSet> all = ClientEyeConfigs.all();
        if (all.isEmpty()) {
            return "No eye configs loaded to export.";
        }

        Path root = Minecraft.getInstance().gameDirectory.toPath().resolve(DUMP_DIR);
        int files = 0;
        try {
            for (Map.Entry<ResourceLocation, RuntimeConfigSet> e : all.entrySet()) {
                ResourceLocation id = e.getKey();
                Optional<String> version = ModVersionLookup.versionForNamespace(id.getNamespace());
                if (version.isEmpty()) {
                    continue; // namespace's mod isn't loaded; can't tag a version
                }
                JsonObject json = setToConfigJson(e.getValue(), versionRange(version.get()));
                if (json == null) {
                    continue; // nothing usable in this set
                }
                Path dir = root.resolve("data").resolve(id.getNamespace()).resolve("eyes");
                Files.createDirectories(dir);
                Files.writeString(dir.resolve(id.getPath() + ".json"), GSON.toJson(json) + "\n");
                files++;
            }
        } catch (IOException e) {
            return "Export-all failed: " + e.getMessage();
        }
        return "Dumped " + files + " eye configs to " + root + " — copy data/ into resources/.";
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
    private static JsonObject setToConfigJson(RuntimeConfigSet set, String versionRange) {
        JsonArray entries = new JsonArray();
        addAgeEntry(entries, "adult", set.adult, versionRange);
        addAgeEntry(entries, "baby", set.baby, versionRange);
        addAgeEntry(entries, "any", set.any, versionRange);
        if (entries.isEmpty()) {
            return null;
        }
        JsonObject root = new JsonObject();
        root.add("entries", entries);
        return root;
    }

    private static void addAgeEntry(JsonArray entries, String age, RuntimeConfig config, String versionRange) {
        if (config == null) {
            return;
        }
        JsonArray variants = variantsJson(config.variants);
        if (variants.isEmpty()) {
            return;
        }
        entries.add(entryJson(versionRange, age, config.isEnabled(), variants));
    }

    private static JsonArray variantsJson(List<Variant> variants) {
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
                head.addProperty("attachPoint", h.attachPoint != null ? h.attachPoint : "head");
                head.add("eyes", eyes);
                heads.add(head);
            }
            if (heads.isEmpty()) {
                continue; // skip arrangements with no usable eyes
            }
            JsonObject variant = new JsonObject();
            variant.addProperty("weight", v.weight());
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
        o.addProperty("eyeScale", p.eyeScale());
        o.addProperty("irisScale", p.irisScale());
        o.addProperty("inclination", p.inclination());
        o.addProperty("azimuth", p.azimuth());
        o.add("corneaColors", colors(a.cornea()));
        o.add("irisColors", colors(a.iris()));
        o.addProperty("glows", a.glow());
        o.addProperty("affectedByInvisibility", p.affectedByInvisibility());
        return o;
    }

    private static JsonArray vec3(Vec3 v) {
        JsonArray array = new JsonArray();
        array.add(v.x);
        array.add(v.y);
        array.add(v.z);
        return array;
    }

    private static JsonArray colors(EyeColor c) {
        JsonArray array = new JsonArray();
        array.add(c.r());
        array.add(c.g());
        array.add(c.b());
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
