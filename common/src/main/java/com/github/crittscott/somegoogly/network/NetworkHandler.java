package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.platform.NetworkTracking;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Cross-loader packet registration, protocol negotiation, and send helpers. */
public final class NetworkHandler {

    public static final String PROTOCOL_VERSION = "9";
    public static final int MAX_PROTOCOL_VERSION_LENGTH = 32;
    public static final ResourceLocation PROTOCOL_HELLO = id("protocol_hello");
    public static final ResourceLocation PROTOCOL_ACK = id("protocol_ack");
    public static final ResourceLocation EYE_STATE = versioned("eye_state");
    public static final ResourceLocation EYE_CONFIG = versioned("eye_config");
    public static final ResourceLocation EYE_BEHAVIOR = versioned("eye_behavior");
    public static final ResourceLocation PICKER_FREEZE = versioned("picker_freeze");
    public static final ResourceLocation PICKER_SPAWN = versioned("picker_spawn");
    public static final ResourceLocation PICKER_SPAWN_ALL = versioned("picker_spawn_all");
    public static final ResourceLocation PICKER_MOB_POSE = versioned("picker_mob_pose");
    public static final ResourceLocation PICKER_EXPORT = versioned("picker_export");

    private static final int HANDSHAKE_TIMEOUT_TICKS = 1200;
    private static final Map<UUID, Integer> PENDING = new HashMap<>();
    private static final Set<UUID> READY = new HashSet<>();
    private static final Map<UUID, Long> LAST_CONFIG_GENERATION = new HashMap<>();
    private static long cachedConfigGeneration = -1L;
    private static long failedConfigGeneration = -1L;
    private static byte[] cachedConfigPayload;
    private static boolean registered;

    private NetworkHandler() {
    }

    /** Register only server-received payloads; safe on physical clients and dedicated servers. */
    public static synchronized void registerCommon() {
        if (registered) {
            return;
        }
        registered = true;
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PROTOCOL_ACK, (buffer, context) -> {
            String version = buffer.readUtf(MAX_PROTOCOL_VERSION_LENGTH);
            context.queue(() -> acknowledge(context, version));
        });
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PICKER_FREEZE,
                (buffer, context) -> PickerFreezePacket.handle(PickerFreezePacket.decode(buffer), context));
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PICKER_SPAWN,
                (buffer, context) -> PickerSpawnPacket.handle(PickerSpawnPacket.decode(buffer), context));
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PICKER_SPAWN_ALL,
                (buffer, context) -> PickerSpawnAllPacket.handle(PickerSpawnAllPacket.decode(buffer), context));
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PICKER_MOB_POSE,
                (buffer, context) -> PickerMobPosePacket.handle(PickerMobPosePacket.decode(buffer), context));
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, PICKER_EXPORT,
                (buffer, context) -> PickerExportPacket.handle(PickerExportPacket.decode(buffer), context));
    }

    /** Start the stable-channel negotiation. Gameplay payloads use versioned IDs and cannot collide. */
    public static void beginHandshake(ServerPlayer player) {
        UUID playerId = player.getUUID();
        READY.remove(playerId);
        LAST_CONFIG_GENERATION.remove(playerId);
        PENDING.put(playerId, HANDSHAKE_TIMEOUT_TICKS);
        FriendlyByteBuf buffer = newBuffer();
        buffer.writeUtf(PROTOCOL_VERSION);
        NetworkManager.sendToPlayer(player, PROTOCOL_HELLO, buffer);
        SomeGooglyCommon.LOGGER.debug(
                "Server network debug: sent protocol hello version={} to {}",
                PROTOCOL_VERSION, player.getGameProfile().getName());
    }

    /** Disconnect clients that never acknowledge the protocol hello. Called once per server tick. */
    public static void tickHandshake(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Integer>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            int ticks = entry.getValue() - 1;
            if (ticks > 0) {
                entry.setValue(ticks);
                continue;
            }
            iterator.remove();
            player.connection.disconnect(Component.translatable("somegoogly.network.handshake_timeout"));
        }
    }

    public static void playerLeft(ServerPlayer player) {
        PENDING.remove(player.getUUID());
        READY.remove(player.getUUID());
        LAST_CONFIG_GENERATION.remove(player.getUUID());
    }

    /** Clear per-server state so integrated-server sessions cannot bleed into the next world. */
    public static void serverStopped() {
        PENDING.clear();
        READY.clear();
        LAST_CONFIG_GENERATION.clear();
        cachedConfigGeneration = -1L;
        failedConfigGeneration = -1L;
        cachedConfigPayload = null;
    }

    public static boolean ready(ServerPlayer player) {
        return READY.contains(player.getUUID());
    }

    public static void sendConfig(ServerPlayer player) {
        long generation = ServerEyeConfigs.generation();
        if (Long.valueOf(generation).equals(LAST_CONFIG_GENERATION.get(player.getUUID()))) {
            return;
        }
        byte[] payload;
        try {
            payload = encodedConfigPayload(generation);
        } catch (RuntimeException error) {
            if (failedConfigGeneration != generation) {
                failedConfigGeneration = generation;
                SomeGooglyCommon.LOGGER.error(
                        "Cannot synchronize eye configs for generation {}: {}", generation, error.getMessage());
            }
            player.connection.disconnect(Component.translatable("somegoogly.network.config_encode_failed"));
            return;
        }
        SomeGooglyCommon.LOGGER.debug(
                "Server network debug: sending {} selected eye configs to {}",
                ServerEyeConfigs.all().size(), player.getGameProfile().getName());
        NetworkManager.sendToPlayer(player, EYE_CONFIG,
                new FriendlyByteBuf(Unpooled.wrappedBuffer(payload)));
        LAST_CONFIG_GENERATION.put(player.getUUID(), generation);
    }

    public static void sendEyeState(ServerPlayer player, EyeStatePacket packet) {
        sendToPlayer(player, EYE_STATE, packet, EyeStatePacket::encode);
    }

    public static void sendEyeStateTrackingAndSelf(Entity entity, EyeStatePacket packet) {
        NetworkTracking.send(entity, true, EYE_STATE, encode(packet, EyeStatePacket::encode));
    }

    public static void sendBehavior(ServerPlayer player, EyeBehaviorTriggerPacket packet) {
        sendToPlayer(player, EYE_BEHAVIOR, packet, EyeBehaviorTriggerPacket::encode);
    }

    public static void sendBehaviorTracking(Entity entity, EyeBehaviorTriggerPacket packet) {
        NetworkTracking.send(entity, false, EYE_BEHAVIOR, encode(packet, EyeBehaviorTriggerPacket::encode));
    }

    public static void sendToServer(PickerFreezePacket packet) {
        sendToServer(PICKER_FREEZE, packet, PickerFreezePacket::encode);
    }

    public static void sendToServer(PickerSpawnPacket packet) {
        sendToServer(PICKER_SPAWN, packet, PickerSpawnPacket::encode);
    }

    public static void sendToServer(PickerSpawnAllPacket packet) {
        sendToServer(PICKER_SPAWN_ALL, packet, PickerSpawnAllPacket::encode);
    }

    public static void sendToServer(PickerMobPosePacket packet) {
        sendToServer(PICKER_MOB_POSE, packet, PickerMobPosePacket::encode);
    }

    public static void sendToServer(PickerExportPacket packet) {
        sendToServer(PICKER_EXPORT, packet, PickerExportPacket::encode);
    }

    private static void acknowledge(NetworkManager.PacketContext context, String version) {
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        UUID playerId = player.getUUID();
        if (PENDING.remove(playerId) == null) {
            return;
        }
        if (!PROTOCOL_VERSION.equals(version)) {
            player.connection.disconnect(protocolMismatch(
                    Component.translatable("somegoogly.network.side.server"), PROTOCOL_VERSION, version));
            return;
        }
        READY.add(playerId);
        SomeGooglyCommon.LOGGER.debug(
                "Server network debug: accepted protocol acknowledgement from {}",
                player.getGameProfile().getName());
        sendConfig(player);
    }

    private static synchronized byte[] encodedConfigPayload(long generation) {
        if (cachedConfigPayload != null && cachedConfigGeneration == generation) {
            return cachedConfigPayload;
        }
        if (failedConfigGeneration == generation) {
            throw new IllegalStateException("Eye config generation previously failed to encode");
        }
        FriendlyByteBuf buffer = newBuffer();
        try {
            EyeConfigSyncPacket.encode(
                    new EyeConfigSyncPacket(generation, ServerEyeConfigs.all()), buffer);
            byte[] encoded = new byte[buffer.readableBytes()];
            buffer.getBytes(buffer.readerIndex(), encoded);
            cachedConfigGeneration = generation;
            failedConfigGeneration = -1L;
            cachedConfigPayload = encoded;
            return encoded;
        } finally {
            buffer.release();
        }
    }

    private static <T> void sendToPlayer(ServerPlayer player, ResourceLocation id, T packet,
                                         BiConsumer<T, FriendlyByteBuf> encoder) {
        NetworkManager.sendToPlayer(player, id, encode(packet, encoder));
    }

    private static <T> void sendToServer(ResourceLocation id, T packet,
                                         BiConsumer<T, FriendlyByteBuf> encoder) {
        if (!NetworkManager.canServerReceive(id)) {
            return;
        }
        NetworkManager.sendToServer(id, encode(packet, encoder));
    }

    public static Component protocolMismatch(Component receiver, String expected, String received) {
        return Component.translatable("somegoogly.network.protocol_mismatch", receiver, expected, received);
    }

    public static FriendlyByteBuf newBuffer() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    private static <T> FriendlyByteBuf encode(T packet, BiConsumer<T, FriendlyByteBuf> encoder) {
        FriendlyByteBuf buffer = newBuffer();
        encoder.accept(packet, buffer);
        return buffer;
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(SomeGooglyCommon.MOD_ID, path);
    }

    private static ResourceLocation versioned(String path) {
        return id("v" + PROTOCOL_VERSION + "/" + path);
    }
}
