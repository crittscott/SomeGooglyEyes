package com.github.crittscott.somegoogly.picker;

import com.github.crittscott.somegoogly.config.ModVersionLookup;
import com.github.crittscott.somegoogly.config.EyeConfigLimits;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.config.VersionRangeMatcher;
import com.github.crittscott.somegoogly.eye.HeadInfo.ConfigFile;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
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

import static com.github.crittscott.somegoogly.eye.HeadInfo.AGE_ANY;

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
 * decode through {@code RuntimeConfig.CODEC} — whose fields are all required, so a malformed or
 * wrong-typed payload is rejected outright rather than decoding to an empty config — and contain at
 * least one usable eye. The declared version is resolved from the <b>server's</b> loaded mod version
 * — the one that matters for its datapack; client and server versions may legally differ. Minecraft
 * is pinned to exact 1.20.1; optional-mod exports span the current minor release.
 *
 * <p>Every attempt has a short throttle applied before codec work. Each successful export triggers a
 * full datapack reload, so successes have the longer {@link #COOLDOWN_TICKS} per-player limit. Picker
 * use is very intermittent, so the brief reload lag and waits are acceptable. All entry points run on
 * the server thread.
 */
public final class PickerExportService {

    /** Ticks between successful exports per player (10 seconds; failed validation doesn't arm it). */
    public static final int COOLDOWN_TICKS = 200;

    /** Cheap attempt throttle applied before codec work, including malformed requests. */
    public static final int ATTEMPT_COOLDOWN_TICKS = 20;

    /** Quota for the packet's encoded config; a legitimate config is a few KiB. */
    public static final long MAX_CONFIG_BYTES = 64 * 1024;

    /** Datapack format for the exact Minecraft version targeted by this source tree (1.20.1). */
    private static final int GENERATED_PACK_FORMAT = 15;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, Integer> LAST_EXPORT_TICK = new HashMap<>();
    private static final Map<UUID, Integer> LAST_ATTEMPT_TICK = new HashMap<>();
    private static final String PACK_MCMETA = """
            {
              "pack": {
                "pack_format": %d,
                "description": {"translate": "somegoogly.pack.picker_description"}
              }
            }
            """.formatted(GENERATED_PACK_FORMAT);
    private static final String PACK_NAME = "somegoogly-picker";

    private PickerExportService() {
    }

    /**
     * Validate and perform one export. Returns localized feedback for the requesting player; the
     * caller ({@code PickerExportPacket}) has already authorized the sender.
     */
    public static Component export(MinecraftServer server, UUID playerId, ResourceLocation typeId,
                                   @Nullable CompoundTag configNbt) {
        int now = server.getTickCount();
        Integer lastAttempt = LAST_ATTEMPT_TICK.get(playerId);
        if (lastAttempt != null && now - lastAttempt < ATTEMPT_COOLDOWN_TICKS) {
            return Component.translatable("somegoogly.command.picker.export_attempt_cooldown");
        }
        LAST_ATTEMPT_TICK.put(playerId, now);
        Integer last = LAST_EXPORT_TICK.get(playerId);
        if (last != null && now - last < COOLDOWN_TICKS) {
            int seconds = (COOLDOWN_TICKS - (now - last) + 19) / 20;
            return Component.translatable(seconds == 1
                    ? "somegoogly.command.picker.export_cooldown_one_second"
                    : "somegoogly.command.picker.export_cooldown_many_seconds", seconds);
        }
        if (configNbt == null) {
            return Component.translatable("somegoogly.command.picker.export_rejected_missing_payload");
        }
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(typeId)) {
            return Component.translatable("somegoogly.command.picker.export_rejected_unknown_type", typeId);
        }
        if (typeId.equals(ServerEyeConfigs.ENDER_DRAGON)) {
            return Component.translatable("somegoogly.command.picker.export_rejected_ender_dragon");
        }
        EyeConfigLimits.WireValidation wireValidation = EyeConfigLimits.validateWireRuntimeConfig(configNbt);
        if (wireValidation.error() != null) {
            return wireValidation.limitExceeded()
                    ? Component.translatable(
                            "somegoogly.command.picker.export_rejected_unsafe_payload", wireValidation.error())
                    : Component.translatable("somegoogly.command.picker.export_rejected_malformed_payload");
        }
        RuntimeConfig config = RuntimeConfig.CODEC.parse(NbtOps.INSTANCE, configNbt).result().orElse(null);
        if (config == null) {
            return Component.translatable("somegoogly.command.picker.export_rejected_malformed_payload");
        }
        // Draft tokens arrive already canonical (the picker authors in its enumeration vocabulary).
        RuntimeConfig pruned = RuntimeConfig.pruned(config, UnaryOperator.identity());
        if (pruned == null) {
            return Component.translatable("somegoogly.command.picker.export_rejected_no_usable_eyes");
        }
        String limitsError = EyeConfigLimits.validateRuntimeConfig(pruned);
        if (limitsError != null) {
            return Component.translatable("somegoogly.command.picker.export_rejected_unsafe_payload", limitsError);
        }
        Optional<String> version = ModVersionLookup.versionForNamespace(typeId.getNamespace());
        if (version.isEmpty()) {
            return Component.translatable(
                    "somegoogly.command.picker.export_rejected_unknown_namespace", typeId.getNamespace());
        }

        String versionDeclaration = typeId.getNamespace().equals("minecraft")
                ? version.get()
                : VersionRangeMatcher.rangeFor(version.get());
        ConfigFile file = ConfigFile.single(versionDeclaration, AGE_ANY, pruned);
        JsonElement json = ConfigFile.CODEC.encodeStart(JsonOps.INSTANCE, file).result().orElse(null);
        if (json == null) {
            return Component.translatable("somegoogly.command.picker.export_rejected_encode_failed");
        }

        Path packDir = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK_NAME);
        Path target = packDir.resolve("data").resolve(typeId.getNamespace())
                .resolve("eyes").resolve(typeId.getPath() + ".json");
        try {
            Files.createDirectories(target.getParent());
            Path meta = packDir.resolve("pack.mcmeta");
            if (!Files.exists(meta)) {
                Files.writeString(meta, PACK_MCMETA);
            }
            Files.writeString(target, GSON.toJson(json) + "\n");
        } catch (IOException e) {
            return Component.translatable("somegoogly.command.picker.export_failed", e.getMessage());
        }

        LAST_EXPORT_TICK.put(playerId, now);
        // Already on the server thread; the datapack is re-read and re-synced to every client.
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
        return Component.translatable("somegoogly.command.picker.export_success", typeId, PACK_NAME);
    }

    /**
     * Drop the per-run cooldown state at server stop: {@code getTickCount} restarts from 0 with the
     * next (single-player) world, and a stale large tick would read as a far-future cooldown.
     */
    public static void onServerStopping() {
        LAST_EXPORT_TICK.clear();
        LAST_ATTEMPT_TICK.clear();
    }

    public static void onPlayerLeft(UUID playerId) {
        LAST_EXPORT_TICK.remove(playerId);
        LAST_ATTEMPT_TICK.remove(playerId);
    }
}
