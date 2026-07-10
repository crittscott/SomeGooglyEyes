package com.github.crittscott.somegoogly.client.picker;

import com.github.crittscott.somegoogly.client.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.client.render.resolver.Resolvers;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.config.ModVersionLookup;
import com.github.crittscott.somegoogly.config.VersionRangeMatcher;
import com.github.crittscott.somegoogly.eye.HeadInfo.ConfigFile;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfigSet;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Turns picker drafts into datapack JSON. Every config field is a required codec field, so
 * {@code ConfigFile.CODEC}'s own encode already produces the canonical, every-field-explicit form the
 * shipped data uses. Two entry points:
 * <ul>
 *   <li>{@link #export()} — the single committed mob, sent to the server as a
 *       {@code PickerExportPacket}; the creative-gated server handler validates it, writes
 *       {@code world/datapacks/somegoogly-picker/data/<ns>/eyes/<entity>.json}, and {@code /reload}s
 *       so it persists and re-syncs through the normal path. Works from a remote client; the result
 *       arrives as a server chat message (rate-limited server-side to one export per 10 seconds).</li>
 *   <li>{@link #exportAll()} — a purely client-side dump of <i>every</i> eye config into
 *       {@code <gameDir>/somegoogly-export/data/…}: the synced runtime state ({@link ClientEyeConfigs})
 *       for untouched entities, overlaid with the picker's per-entity drafts
 *       ({@link PickerState#authoredConfigs()}) so a mob saved-but-never-exported is included too.
 *       For copying straight into the mod's {@code resources/}.</li>
 * </ul>
 */
public final class PickerExporter {

    private static final String DUMP_DIR = "somegoogly-export";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private PickerExporter() {
    }

    /**
     * Send the committed draft for the chosen mob to the server to be written and reloaded. Client-side
     * we only guard the obvious (something committed) and codec-encode the draft; all real validation —
     * and the authoritative feedback — is the server's ({@code PickerExportService}).
     */
    public static String export() {
        ResourceLocation type = PickerState.targetType();
        if (type == null || PickerState.totalEyeCount() == 0) {
            return "Nothing committed to export.";
        }
        RuntimeConfig config = PickerState.toConfig();
        if (config.variants.isEmpty()) {
            return "Nothing committed to export.";
        }
        // Draft tokens are already canonical (seeded/authored in the picker's enumeration vocabulary).
        Tag encoded = RuntimeConfig.CODEC.encodeStart(NbtOps.INSTANCE, config).result().orElse(null);
        if (!(encoded instanceof CompoundTag tag)) {
            return "Export failed: couldn't encode the draft config.";
        }
        NetworkHandler.INSTANCE.sendToServer(new PickerExportPacket(type, tag));
        return "Export of " + type + " sent to the server…";
    }

    /**
     * Dump every eye config to one canonical file per entity under
     * {@code <gameDir>/somegoogly-export/data/<ns>/eyes/<entity>.json}: the synced runtime state for
     * untouched entities, with each picker draft overlaid on its entity (so saved-but-not-exported mobs
     * are written too). Unlike the per-mob export this is entirely client-side (no server involvement,
     * no reload) — it just captures what's authored/live so it can be copied into the mod's
     * {@code resources/}.
     *
     * <p>The declared version range is re-synthesized from the currently-loaded version of each entity's
     * namespace ({@link VersionRangeMatcher#rangeFor}); the original entry's declared range isn't
     * preserved in the runtime config, so it can't be recovered.
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
                String range = VersionRangeMatcher.rangeFor(version.get());
                RuntimeConfig draft = drafts.get(id);
                ConfigFile file;
                if (draft != null) {
                    // Drafts are already canonical (seeded/authored in the picker's enumeration vocabulary).
                    RuntimeConfig pruned = RuntimeConfig.pruned(draft, UnaryOperator.identity());
                    file = pruned == null ? null : ConfigFile.single(range, "any", pruned);
                } else {
                    // Canonicalize each token against the model so the dump speaks the resolver's own
                    // vocabulary (e.g. a differently-spelled name snaps to its enumerated path). The model
                    // comes from a throwaway entity instance, so every config converts regardless of what's
                    // loaded near the player.
                    UnaryOperator<String> canon = canonicalizer(id);
                    if (canon == null) {
                        verbatim++;
                        canon = UnaryOperator.identity();
                    }
                    file = ConfigFile.ofSet(synced.get(id), range, canon);
                }
                if (file == null) {
                    continue; // nothing usable for this entity
                }
                JsonElement json = ConfigFile.CODEC.encodeStart(JsonOps.INSTANCE, file).result().orElse(null);
                if (json == null) {
                    continue;
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
}
