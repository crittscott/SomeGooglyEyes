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

    public static final String PROTOCOL_VERSION = "7";
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

    private static final int HANDSHAKE_TIMEOUT_TICKS = 100;
    private static final Map<UUID, Integer> PENDING = new HashMap<>();
    private static final Set<UUID> READY = new HashSet<>();
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
            String version = buffer.readUtf(32);
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
        PENDING.put(playerId, HANDSHAKE_TIMEOUT_TICKS);
        FriendlyByteBuf buffer = newBuffer();
        buffer.writeUtf(PROTOCOL_VERSION);
        NetworkManager.sendToPlayer(player, PROTOCOL_HELLO, buffer);
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
            player.connection.disconnect(Component.literal(
                    "Some Googly Eyes network protocol handshake failed. Both sides must use compatible builds."));
        }
    }

    public static void playerLeft(ServerPlayer player) {
        PENDING.remove(player.getUUID());
        READY.remove(player.getUUID());
    }

    public static boolean ready(ServerPlayer player) {
        return READY.contains(player.getUUID());
    }

    public static void sendConfig(ServerPlayer player) {
        sendToPlayer(player, EYE_CONFIG, new EyeConfigSyncPacket(ServerEyeConfigs.all()),
                EyeConfigSyncPacket::encode);
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
        if (!PROTOCOL_VERSION.equals(version)) {
            player.connection.disconnect(protocolMismatch("server", PROTOCOL_VERSION, version));
            return;
        }
        UUID playerId = player.getUUID();
        PENDING.remove(playerId);
        READY.add(playerId);
        sendConfig(player);
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

    public static Component protocolMismatch(String receiver, String expected, String received) {
        return Component.literal("Some Googly Eyes network protocol mismatch on " + receiver
                + " (expected " + expected + ", received " + received + ").");
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
