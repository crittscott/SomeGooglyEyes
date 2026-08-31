package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.platform.NetworkTracking;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

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

    public static final PayloadType<String> PROTOCOL_HELLO_PAYLOAD = payloadType(
            PROTOCOL_HELLO, Direction.CLIENTBOUND,
            buffer -> buffer.readUtf(MAX_PROTOCOL_VERSION_LENGTH),
            (version, buffer) -> buffer.writeUtf(version));
    public static final PayloadType<String> PROTOCOL_ACK_PAYLOAD = payloadType(
            PROTOCOL_ACK, Direction.SERVERBOUND,
            buffer -> buffer.readUtf(MAX_PROTOCOL_VERSION_LENGTH),
            (version, buffer) -> buffer.writeUtf(version));
    public static final PayloadType<EyeStatePacket> EYE_STATE_PAYLOAD = payloadType(
            EYE_STATE, Direction.CLIENTBOUND, EyeStatePacket::decode, EyeStatePacket::encode);
    public static final PayloadType<byte[]> EYE_CONFIG_PAYLOAD = payloadType(
            EYE_CONFIG, Direction.CLIENTBOUND,
            NetworkHandler::decodeConfigPayload, NetworkHandler::encodeConfigPayload);
    public static final PayloadType<EyeBehaviorTriggerPacket> EYE_BEHAVIOR_PAYLOAD = payloadType(
            EYE_BEHAVIOR, Direction.CLIENTBOUND,
            EyeBehaviorTriggerPacket::decode, EyeBehaviorTriggerPacket::encode);
    public static final PayloadType<PickerFreezePacket> PICKER_FREEZE_PAYLOAD = payloadType(
            PICKER_FREEZE, Direction.SERVERBOUND, PickerFreezePacket::decode, PickerFreezePacket::encode);
    public static final PayloadType<PickerSpawnPacket> PICKER_SPAWN_PAYLOAD = payloadType(
            PICKER_SPAWN, Direction.SERVERBOUND, PickerSpawnPacket::decode, PickerSpawnPacket::encode);
    public static final PayloadType<PickerSpawnAllPacket> PICKER_SPAWN_ALL_PAYLOAD = payloadType(
            PICKER_SPAWN_ALL, Direction.SERVERBOUND,
            PickerSpawnAllPacket::decode, PickerSpawnAllPacket::encode);
    public static final PayloadType<PickerMobPosePacket> PICKER_MOB_POSE_PAYLOAD = payloadType(
            PICKER_MOB_POSE, Direction.SERVERBOUND,
            PickerMobPosePacket::decode, PickerMobPosePacket::encode);
    public static final PayloadType<PickerExportPacket> PICKER_EXPORT_PAYLOAD = payloadType(
            PICKER_EXPORT, Direction.SERVERBOUND, PickerExportPacket::decode, PickerExportPacket::encode);

    private static final List<PayloadType<?>> PAYLOAD_TYPES = List.of(
            PROTOCOL_HELLO_PAYLOAD, PROTOCOL_ACK_PAYLOAD, EYE_STATE_PAYLOAD, EYE_CONFIG_PAYLOAD,
            EYE_BEHAVIOR_PAYLOAD, PICKER_FREEZE_PAYLOAD, PICKER_SPAWN_PAYLOAD,
            PICKER_SPAWN_ALL_PAYLOAD, PICKER_MOB_POSE_PAYLOAD, PICKER_EXPORT_PAYLOAD);

    private static final int HANDSHAKE_TIMEOUT_TICKS = 6000;
    private static final Map<UUID, Integer> PENDING = new HashMap<>();
    private static final Set<UUID> READY = new HashSet<>();
    private static final Map<UUID, Long> LAST_CONFIG_GENERATION = new HashMap<>();
    private static long cachedConfigGeneration = -1L;
    private static long failedConfigGeneration = -1L;
    private static byte[] cachedConfigPayload;
    private static boolean registered;

    private NetworkHandler() {
    }

    /** Bind the direction-specific common server handlers before a loader registers native payloads. */
    public static synchronized void registerCommon() {
        if (registered) {
            return;
        }
        registered = true;
        PROTOCOL_ACK_PAYLOAD.bindReceiver(
                (version, context) -> context.queue(() -> acknowledge(context, version)));
        PICKER_FREEZE_PAYLOAD.bindReceiver(PickerFreezePacket::handle);
        PICKER_SPAWN_PAYLOAD.bindReceiver(PickerSpawnPacket::handle);
        PICKER_SPAWN_ALL_PAYLOAD.bindReceiver(PickerSpawnAllPacket::handle);
        PICKER_MOB_POSE_PAYLOAD.bindReceiver(PickerMobPosePacket::handle);
        PICKER_EXPORT_PAYLOAD.bindReceiver(PickerExportPacket::handle);
    }

    /** Supply every typed payload to one loader-native play registration lifecycle. */
    public static void registerPayloads(NetworkTransport.Registrar registrar) {
        for (PayloadType<?> payloadType : PAYLOAD_TYPES) {
            registerPayload(registrar, payloadType);
        }
    }

    /** Register physical-client receivers after Fabric has registered their clientbound codecs. */
    public static void registerClientReceivers(NetworkTransport.ClientReceiverRegistrar registrar) {
        for (PayloadType<?> payloadType : PAYLOAD_TYPES) {
            if (payloadType.direction == Direction.CLIENTBOUND) {
                registerClientReceiver(registrar, payloadType);
            }
        }
    }

    /** Start the stable-channel negotiation. Gameplay payloads use versioned IDs and cannot collide. */
    public static void beginHandshake(ServerPlayer player) {
        UUID playerId = player.getUUID();
        READY.remove(playerId);
        LAST_CONFIG_GENERATION.remove(playerId);
        PENDING.put(playerId, HANDSHAKE_TIMEOUT_TICKS);
        PROTOCOL_HELLO_PAYLOAD.sendToPlayer(player, PROTOCOL_VERSION);
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
        EYE_CONFIG_PAYLOAD.sendToPlayer(player, payload);
        LAST_CONFIG_GENERATION.put(player.getUUID(), generation);
    }

    public static void sendEyeState(ServerPlayer player, EyeStatePacket packet) {
        EYE_STATE_PAYLOAD.sendToPlayer(player, packet);
    }

    public static void sendEyeStateTrackingAndSelf(Entity entity, EyeStatePacket packet) {
        NetworkTracking.send(entity, true, EYE_STATE_PAYLOAD.payload(packet));
    }

    public static void sendBehavior(ServerPlayer player, EyeBehaviorTriggerPacket packet) {
        EYE_BEHAVIOR_PAYLOAD.sendToPlayer(player, packet);
    }

    public static void sendBehaviorTracking(Entity entity, EyeBehaviorTriggerPacket packet) {
        NetworkTracking.send(entity, false, EYE_BEHAVIOR_PAYLOAD.payload(packet));
    }

    public static void sendToServer(PickerFreezePacket packet) {
        PICKER_FREEZE_PAYLOAD.sendToServer(packet);
    }

    public static void sendToServer(PickerSpawnPacket packet) {
        PICKER_SPAWN_PAYLOAD.sendToServer(packet);
    }

    public static void sendToServer(PickerSpawnAllPacket packet) {
        PICKER_SPAWN_ALL_PAYLOAD.sendToServer(packet);
    }

    public static void sendToServer(PickerMobPosePacket packet) {
        PICKER_MOB_POSE_PAYLOAD.sendToServer(packet);
    }

    public static void sendToServer(PickerExportPacket packet) {
        PICKER_EXPORT_PAYLOAD.sendToServer(packet);
    }

    private static void acknowledge(NetworkTransport.Context context, String version) {
        ServerPlayer player = context.player();
        if (player == null) {
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

    public static Component protocolMismatch(Component receiver, String expected, String received) {
        return Component.translatable("somegoogly.network.protocol_mismatch", receiver, expected, received);
    }

    public static FriendlyByteBuf newBuffer() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    private static byte[] decodeConfigPayload(FriendlyByteBuf buffer) {
        int length = buffer.readableBytes();
        if (length > EyeConfigSyncPacket.MAX_PAYLOAD_BYTES) {
            throw new DecoderException("Eye config sync payload exceeds protocol limit");
        }
        byte[] payload = new byte[length];
        buffer.readBytes(payload);
        return payload;
    }

    private static void encodeConfigPayload(byte[] payload, FriendlyByteBuf buffer) {
        if (payload.length > EyeConfigSyncPacket.MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Eye config sync payload exceeds protocol limit");
        }
        buffer.writeBytes(payload);
    }

    private static <T> PayloadType<T> payloadType(ResourceLocation id, Direction direction,
                                                   Function<FriendlyByteBuf, T> decoder,
                                                   BiConsumer<T, FriendlyByteBuf> encoder) {
        return new PayloadType<>(id, direction, decoder, encoder);
    }

    private static <T> void registerPayload(NetworkTransport.Registrar registrar, PayloadType<T> payloadType) {
        if (payloadType.direction == Direction.CLIENTBOUND) {
            registrar.registerClientbound(payloadType);
        } else {
            registrar.registerServerbound(payloadType);
        }
    }

    private static <T> void registerClientReceiver(
            NetworkTransport.ClientReceiverRegistrar registrar, PayloadType<T> payloadType) {
        registrar.register(payloadType);
    }

    /** One typed custom-payload channel whose body codec remains owned by its packet class. */
    public static final class PayloadType<T> {
        private final CustomPacketPayload.Type<Payload<T>> type;
        private final StreamCodec<RegistryFriendlyByteBuf, Payload<T>> codec;
        private final Direction direction;
        private NetworkTransport.Receiver<T> receiver;

        private PayloadType(ResourceLocation id, Direction direction, Function<FriendlyByteBuf, T> decoder,
                            BiConsumer<T, FriendlyByteBuf> encoder) {
            type = new CustomPacketPayload.Type<>(id);
            this.direction = direction;
            codec = new StreamCodec<>() {
                @Override
                public Payload<T> decode(RegistryFriendlyByteBuf buffer) {
                    return new Payload<>(PayloadType.this, decoder.apply(buffer));
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, Payload<T> payload) {
                    encoder.accept(payload.value, buffer);
                }
            };
        }

        public CustomPacketPayload.Type<Payload<T>> type() {
            return type;
        }

        public StreamCodec<RegistryFriendlyByteBuf, Payload<T>> codec() {
            return codec;
        }

        public ResourceLocation id() {
            return type.id();
        }

        public synchronized void bindReceiver(NetworkTransport.Receiver<T> receiver) {
            if (this.receiver != null) {
                throw new IllegalStateException("Receiver is already bound for " + id());
            }
            this.receiver = receiver;
        }

        public void receive(Payload<T> payload, NetworkTransport.Context context) {
            NetworkTransport.Receiver<T> boundReceiver = receiver;
            if (boundReceiver == null) {
                throw new IllegalStateException("No receiver is bound for " + id());
            }
            boundReceiver.receive(payload.value, context);
        }

        public void sendToPlayer(ServerPlayer player, T value) {
            NetworkTransport.sendToPlayer(player, payload(value));
        }

        public void sendToServer(T value) {
            if (NetworkTransport.canServerReceive(type)) {
                sendToServerUnchecked(value);
            }
        }

        public void sendToServerUnchecked(T value) {
            NetworkTransport.sendToServer(payload(value));
        }

        public Payload<T> payload(T value) {
            return new Payload<>(this, value);
        }
    }

    public static final class Payload<T> implements CustomPacketPayload {
        private final PayloadType<T> payloadType;
        private final T value;

        private Payload(PayloadType<T> payloadType, T value) {
            this.payloadType = payloadType;
            this.value = value;
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return payloadType.type;
        }
    }

    private enum Direction {
        CLIENTBOUND,
        SERVERBOUND
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, path);
    }

    private static ResourceLocation versioned(String path) {
        return id("v" + PROTOCOL_VERSION + "/" + path);
    }
}
