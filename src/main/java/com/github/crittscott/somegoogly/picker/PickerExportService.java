package com.github.crittscott.somegoogly.picker;

import com.github.crittscott.somegoogly.config.EyeConfigJsonWriter;
import com.github.crittscott.somegoogly.config.ModVersionLookup;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Server-side half of {@code /sg export} (reached via {@code PickerExportPacket}): validates a
 * client-authored eye config, writes it as canonical datapack JSON into the world's
 * {@code somegoogly-picker} pack, and {@code /reload}s so it persists and re-syncs through the normal
 * path. Owning this on the server is what lets a remote client export.
 *
 * <p>The payload is <b>never trusted</b>: the entity id must parse and exist in the entity registry
 * (which is also the whole path-traversal defense — a valid {@link ResourceLocation} can't contain
 * {@code ..} or escape characters, so a file path built from its components stays inside the pack),
 * the ender dragon is refused (mirroring the reload listener's hard exclusion), and the config must
 * decode through {@code RuntimeConfig.CODEC} and contain at least one usable eye. The declared version
 * range is resolved from the <b>server's</b> loaded mod version — the one that matters for its
 * datapack; client and server versions may legally differ.
 *
 * <p>Each export triggers a full datapack reload, so successful exports are rate-limited to one per
 * {@link #COOLDOWN_TICKS} per player. Picker use is very intermittent, so the brief reload lag and the
 * 10-second wait are acceptable. All entry points run on the server thread.
 */
public final class PickerExportService {

    /** Ticks between successful exports per player (10 seconds; failed validation doesn't arm it). */
    public static final int COOLDOWN_TICKS = 200;

    /** Quota for the packet's encoded config; a legitimate config is a few KiB. */
    public static final long MAX_CONFIG_BYTES = 64 * 1024;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, Integer> LAST_EXPORT_TICK = new HashMap<>();
    private static final String PACK_MCMETA =
            "{\n  \"pack\": {\n    \"pack_format\": 15,\n    \"description\": \"SomeGoogly picker output\"\n  }\n}\n";
    private static final String PACK_NAME = "somegoogly-picker";

    private PickerExportService() {
    }

    /**
     * Validate and perform one export. Returns the feedback message for the requesting player; the
     * caller ({@code PickerExportPacket}) has already authorized the sender.
     */
    public static String export(MinecraftServer server, UUID playerId, ResourceLocation typeId,
                                @Nullable CompoundTag configNbt) {
        int now = server.getTickCount();
        Integer last = LAST_EXPORT_TICK.get(playerId);
        if (last != null && now - last < COOLDOWN_TICKS) {
            int seconds = (COOLDOWN_TICKS - (now - last) + 19) / 20;
            return "Export cooling down (~" + seconds + "s left).";
        }
        if (configNbt == null) {
            return "Export rejected: missing or oversized config payload.";
        }
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(typeId)) {
            return "Export rejected: unknown entity type " + typeId + ".";
        }
        if (typeId.equals(ServerEyeConfigs.ENDER_DRAGON)) {
            return "Export rejected: the ender dragon is hard-excluded from googly eyes.";
        }
        RuntimeConfig config = RuntimeConfig.CODEC.parse(NbtOps.INSTANCE, configNbt).result().orElse(null);
        if (config == null) {
            return "Export rejected: malformed eye config payload.";
        }
        // Draft tokens arrive already canonical (the picker authors in its enumeration vocabulary).
        JsonArray variants = EyeConfigJsonWriter.variantsJson(config.variants, UnaryOperator.identity());
        if (variants.isEmpty()) {
            return "Export rejected: config has no usable eyes.";
        }
        Optional<String> version = ModVersionLookup.versionForNamespace(typeId.getNamespace());
        if (version.isEmpty()) {
            return "Export rejected: no loaded mod provides namespace '" + typeId.getNamespace() + "'.";
        }

        JsonObject json = EyeConfigJsonWriter.fileJson(EyeConfigJsonWriter.entryJson(
                EyeConfigJsonWriter.versionRange(version.get()), "any", config.isEnabled(), variants));

        Path packDir = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK_NAME);
        Path file = packDir.resolve("data").resolve(typeId.getNamespace())
                .resolve("eyes").resolve(typeId.getPath() + ".json");
        try {
            Files.createDirectories(file.getParent());
            Path meta = packDir.resolve("pack.mcmeta");
            if (!Files.exists(meta)) {
                Files.writeString(meta, PACK_MCMETA);
            }
            Files.writeString(file, GSON.toJson(json) + "\n");
        } catch (IOException e) {
            return "Export failed: " + e.getMessage();
        }

        LAST_EXPORT_TICK.put(playerId, now);
        // Already on the server thread; the datapack is re-read and re-synced to every client.
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
        return "Exported " + typeId + " → " + PACK_NAME + ", reloading.";
    }

    /**
     * Drop the per-run cooldown state at server stop: {@code getTickCount} restarts from 0 with the
     * next (single-player) world, and a stale large tick would read as a far-future cooldown.
     */
    public static void onServerStopping() {
        LAST_EXPORT_TICK.clear();
    }
}
