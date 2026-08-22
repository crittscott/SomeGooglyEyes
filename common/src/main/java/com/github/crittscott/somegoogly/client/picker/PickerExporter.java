package com.github.crittscott.somegoogly.client.picker;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.config.ModVersionLookup;
import com.github.crittscott.somegoogly.config.VersionRangeMatcher;
import com.github.crittscott.somegoogly.config.EyeConfigModel.ConfigFile;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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
    public static Component export() {
        ResourceLocation type = PickerState.targetType();
        if (type == null || PickerState.totalEyeCount() == 0) {
            return Component.translatable("somegoogly.command.picker.export_nothing_committed");
        }
        RuntimeConfig config = PickerState.toConfig();
        if (config.variants.isEmpty()) {
            return Component.translatable("somegoogly.command.picker.export_nothing_committed");
        }
        // Draft tokens are already canonical (seeded/authored in the picker's enumeration vocabulary).
        Tag encoded = RuntimeConfig.CODEC.encodeStart(NbtOps.INSTANCE, config).result().orElse(null);
        if (!(encoded instanceof CompoundTag tag)) {
            return Component.translatable("somegoogly.command.picker.export_encode_failed");
        }
        NetworkHandler.sendToServer(new PickerExportPacket(type, PickerState.currentDraftAge(), tag));
        return Component.translatable("somegoogly.command.picker.export_sent", type);
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
    public static Component exportAll() {
        Map<ResourceLocation, RuntimeConfigSet> synced = ClientEyeConfigs.all();
        Map<ResourceLocation, PickerState.AuthoredExport> drafts = PickerState.authoredConfigs();
        if (synced.isEmpty() && drafts.isEmpty()) {
            return Component.translatable("somegoogly.command.picker.export_all_none_loaded");
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
                PickerState.AuthoredExport draft = drafts.get(id);
                ConfigFile file;
                if (draft != null) {
                    // Drafts are already canonical (seeded/authored in the picker's enumeration vocabulary).
                    RuntimeConfig pruned = RuntimeConfig.pruned(draft.config(), UnaryOperator.identity());
                    file = pruned == null ? null : ConfigFile.single(range, draft.age(), pruned);
                } else {
                    // Canonicalize each token against the model so the dump speaks its discovered
                    // vocabulary (e.g. a differently-spelled name snaps to its enumerated path). The model
                    // comes from a throwaway entity instance, so every config converts regardless of what's
                    // loaded near the player.
                    ModelPartVocabulary vocabulary = ModelPartVocabulary.forType(id);
                    UnaryOperator<String> canon;
                    if (vocabulary == null) {
                        verbatim++;
                        canon = UnaryOperator.identity();
                    } else {
                        canon = vocabulary::canonicalize;
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
            return Component.translatable("somegoogly.command.picker.export_all_failed", e.getMessage());
        }
        Component note = verbatim > 0
                ? Component.translatable(verbatim == 1
                        ? "somegoogly.command.picker.export_all_one_verbatim"
                        : "somegoogly.command.picker.export_all_many_verbatim", verbatim)
                : Component.empty();
        return Component.translatable(files == 1
                        ? "somegoogly.command.picker.export_all_one_result"
                        : "somegoogly.command.picker.export_all_many_result",
                files, root, note);
    }

}
