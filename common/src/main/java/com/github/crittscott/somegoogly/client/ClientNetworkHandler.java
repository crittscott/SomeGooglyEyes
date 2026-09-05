package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehaviors;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.network.NetworkTransport;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Physical-client-only registration and application of server-to-client payloads. */
public final class ClientNetworkHandler {

    private static final int EYE_STATE_LOG_LIMIT = 20;
    private static final int MAX_PENDING_EYE_STATES = 1_024;
    private static final int PENDING_EYE_STATE_TTL_TICKS = 100;
    private static final int MIN_CONFIG_SYNC_INTERVAL_TICKS = 20;
    private static final long MIN_CONFIG_DECODE_NANOS = 1_000_000_000L;
    private static final Map<Integer, PendingEyeState> PENDING_EYE_STATES = new LinkedHashMap<>();

    // Thread ownership of the mutable statics below:
    //   - handleConfigPayload runs on the netty thread (under this class's monitor) and owns
    //     lastWireConfigGeneration and lastConfigDecodeNanos.
    //   - every context.queue(...) body runs on the client main thread and owns the rest:
    //     protocolAccepted, networkTicks, lastConfigSyncTick, lastConfigGeneration, the pending map,
    //     the counters.
    //   - clearPendingEyeStates (disconnect) resets all of them, including the netty-owned pair
    //     without holding the monitor; tolerated because the connection is already gone.
    // The partition is otherwise clean, and a stale read's worst case is a spurious self-disconnect,
    // never corrupted state.
    private static boolean protocolAccepted;
    private static int networkTicks;
    private static int lastConfigSyncTick = Integer.MIN_VALUE;
    private static long lastConfigGeneration = -1L;
    private static long lastWireConfigGeneration = -1L;
    private static long lastConfigDecodeNanos;
    private static int eyeStatePackets;
    private static int appliedQueuedEyeStates;

    private record PendingEyeState(EyeStatePacket packet, int expiresAt) {
    }

    private ClientNetworkHandler() {
    }

    public static void register() {
        SomeGooglyCommon.LOGGER.debug("Registering client-bound receivers");
        NetworkHandler.PROTOCOL_HELLO_PAYLOAD.bindClientReceiver(ClientNetworkHandler::handleHello);
        NetworkHandler.EYE_STATE_PAYLOAD.bindClientReceiver(ClientNetworkHandler::handle);
        NetworkHandler.EYE_CONFIG_PAYLOAD.bindClientReceiver(ClientNetworkHandler::handleConfigPayload);
        NetworkHandler.EYE_BEHAVIOR_PAYLOAD.bindClientReceiver(ClientNetworkHandler::handle);
    }

    private static void handleHello(String version, NetworkTransport.Context context) {
        context.queue(() -> {
            SomeGooglyCommon.LOGGER.debug(
                    "Received protocol hello version={} expected={}",
                    version, NetworkHandler.PROTOCOL_VERSION);
            if (protocolAccepted) {
                return;
            }
            if (!NetworkHandler.PROTOCOL_VERSION.equals(version)) {
                disconnect(NetworkHandler.protocolMismatch(
                        Component.translatable("somegoogly.network.side.client"),
                        NetworkHandler.PROTOCOL_VERSION, version));
                return;
            }
            protocolAccepted = true;
            NetworkHandler.PROTOCOL_ACK_PAYLOAD.sendToServerUnchecked(NetworkHandler.PROTOCOL_VERSION);
            SomeGooglyCommon.LOGGER.debug("Sent protocol acknowledgement");
        });
    }

    private static void handle(EyeStatePacket packet, NetworkTransport.Context context) {
        context.queue(() -> {
            LivingEntity living = living(packet.entityId());
            int ordinal = ++eyeStatePackets;
            if (ordinal <= EYE_STATE_LOG_LIMIT) {
                SomeGooglyCommon.LOGGER.debug(
                        "Eye-state packet #{} entityId={} hasEyes={} resolvedEntity={}",
                        ordinal, packet.entityId(), packet.hasGooglyEyes(),
                        living == null ? "missing"
                                : BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()));
            }
            if (living == null) {
                queueEyeState(packet);
                return;
            }
            applyEyeState(living, packet);
        });
    }

    /** Apply an eye-state packet that arrived before the client created the corresponding entity. */
    public static void onEntityLoaded(Entity entity) {
        PendingEyeState pending = PENDING_EYE_STATES.remove(entity.getId());
        if (pending == null || pending.expiresAt() < networkTicks || !(entity instanceof LivingEntity living)) {
            return;
        }
        EyeStatePacket packet = pending.packet();
        if (++appliedQueuedEyeStates <= EYE_STATE_LOG_LIMIT) {
            SomeGooglyCommon.LOGGER.debug(
                    "Applying queued eye-state #{} to entityId={} type={} hasEyes={}",
                    appliedQueuedEyeStates, entity.getId(),
                    BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), packet.hasGooglyEyes());
        }
        applyEyeState(living, packet);
    }

    public static void clearPendingEyeStates() {
        PENDING_EYE_STATES.clear();
        protocolAccepted = false;
        networkTicks = 0;
        lastConfigSyncTick = Integer.MIN_VALUE;
        lastConfigGeneration = -1L;
        lastWireConfigGeneration = -1L;
        lastConfigDecodeNanos = 0L;
        eyeStatePackets = 0;
        appliedQueuedEyeStates = 0;
    }

    /** Advance bounded pending-state expiry from each loader's end-client-tick hook. */
    public static void tick() {
        networkTicks++;
        Iterator<PendingEyeState> iterator = PENDING_EYE_STATES.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().expiresAt() < networkTicks) {
                iterator.remove();
            }
        }
    }

    private static synchronized void handleConfigPayload(byte[] payload, NetworkTransport.Context context) {
        if (payload.length < Long.BYTES) {
            context.queue(() -> disconnect(Component.translatable("somegoogly.network.invalid_eye_config")));
            return;
        }
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(payload));
        try {
            long generation = buffer.getLong(buffer.readerIndex());
            if (generation == lastWireConfigGeneration) {
                return;
            }
            long now = System.nanoTime();
            if (lastConfigDecodeNanos != 0L && now - lastConfigDecodeNanos < MIN_CONFIG_DECODE_NANOS) {
                context.queue(() -> disconnect(Component.translatable(
                        "somegoogly.network.config_sync_too_frequent")));
                return;
            }
            lastWireConfigGeneration = generation;
            lastConfigDecodeNanos = now;
            handle(EyeConfigSyncPacket.decode(buffer), context);
        } catch (RuntimeException invalid) {
            SomeGooglyCommon.LOGGER.error("Malformed eye-config sync payload from server", invalid);
            context.queue(() -> disconnect(Component.translatable("somegoogly.network.invalid_eye_config")));
        } finally {
            buffer.release();
        }
    }

    private static void handle(EyeConfigSyncPacket packet, NetworkTransport.Context context) {
        context.queue(() -> {
            if (!protocolAccepted) {
                disconnect(Component.translatable("somegoogly.network.config_before_handshake"));
                return;
            }
            if (packet.generation() == lastConfigGeneration) {
                return;
            }
            if (packet.generation() < lastConfigGeneration
                    || (lastConfigSyncTick != Integer.MIN_VALUE
                    && networkTicks - lastConfigSyncTick < MIN_CONFIG_SYNC_INTERVAL_TICKS)) {
                disconnect(Component.translatable("somegoogly.network.config_sync_too_frequent"));
                return;
            }
            lastConfigGeneration = packet.generation();
            lastConfigSyncTick = networkTicks;
            SomeGooglyCommon.LOGGER.debug(
                    "Received {} selected entity eye configs", packet.configs().size());
            ClientEyeConfigs.replaceAll(packet.configs());
            ClientEyeRuntime.clear();
        });
    }

    private static void handle(EyeBehaviorTriggerPacket packet, NetworkTransport.Context context) {
        context.queue(() -> {
            EyeBehavior behavior = EyeBehaviors.byId(packet.behaviorId());
            LivingEntity living = living(packet.entityId());
            if (behavior == null || living == null) {
                return;
            }
            GooglyTracker tracker = ClientEyeRuntime.peek(living);
            if (tracker != null) {
                tracker.startBehavior(behavior, packet.duration(), packet.seed(), packet.elapsed());
            }
        });
    }

    private static LivingEntity living(int entityId) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        Entity entity = level.getEntity(entityId);
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void applyEyeState(LivingEntity living, EyeStatePacket packet) {
        EyeState.applySnapshot(living, packet.snapshot());
        GooglyTracker tracker = ClientEyeRuntime.peek(living);
        if (tracker != null) {
            tracker.overrides = packet.overrides();
        }
    }

    private static void queueEyeState(EyeStatePacket packet) {
        PENDING_EYE_STATES.remove(packet.entityId());
        while (PENDING_EYE_STATES.size() >= MAX_PENDING_EYE_STATES) {
            Iterator<Integer> iterator = PENDING_EYE_STATES.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
        PENDING_EYE_STATES.put(packet.entityId(),
                new PendingEyeState(packet, networkTicks + PENDING_EYE_STATE_TTL_TICKS));
    }

    private static void disconnect(Component reason) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            SomeGooglyCommon.LOGGER.warn("Disconnecting from server: {}", reason.getString());
            connection.getConnection().disconnect(reason);
        }
    }
}
