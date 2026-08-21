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
import com.github.crittscott.somegoogly.platform.EntityPersistentData;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/** Physical-client-only registration and application of server-to-client payloads. */
public final class ClientNetworkHandler {

    private static final int EYE_STATE_LOG_LIMIT = 20;
    private static final Map<Integer, EyeStatePacket> PENDING_EYE_STATES = new HashMap<>();
    private static boolean registered;
    private static int eyeStatePackets;
    private static int appliedQueuedEyeStates;

    private ClientNetworkHandler() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        SomeGooglyCommon.LOGGER.info("Client network debug: registering Some Googly Eyes S2C receivers");
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, NetworkHandler.PROTOCOL_HELLO,
                ClientNetworkHandler::handleHello);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, NetworkHandler.EYE_STATE,
                (buffer, context) -> handle(EyeStatePacket.decode(buffer), context));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, NetworkHandler.EYE_CONFIG,
                (buffer, context) -> handle(EyeConfigSyncPacket.decode(buffer), context));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, NetworkHandler.EYE_BEHAVIOR,
                (buffer, context) -> handle(EyeBehaviorTriggerPacket.decode(buffer), context));
    }

    private static void handleHello(FriendlyByteBuf buffer, NetworkManager.PacketContext context) {
        String version = buffer.readUtf(NetworkHandler.MAX_PROTOCOL_VERSION_LENGTH);
        context.queue(() -> {
            SomeGooglyCommon.LOGGER.info(
                    "Client network debug: received protocol hello version={} expected={}",
                    version, NetworkHandler.PROTOCOL_VERSION);
            if (!NetworkHandler.PROTOCOL_VERSION.equals(version)) {
                disconnect(NetworkHandler.protocolMismatch(
                        Component.translatable("somegoogly.network.side.client"),
                        NetworkHandler.PROTOCOL_VERSION, version));
                return;
            }
            FriendlyByteBuf reply = NetworkHandler.newBuffer();
            reply.writeUtf(NetworkHandler.PROTOCOL_VERSION);
            NetworkManager.sendToServer(NetworkHandler.PROTOCOL_ACK, reply);
            SomeGooglyCommon.LOGGER.info("Client network debug: sent protocol acknowledgement");
        });
    }

    private static void handle(EyeStatePacket packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            LivingEntity living = living(packet.entityId());
            int ordinal = ++eyeStatePackets;
            if (ordinal <= EYE_STATE_LOG_LIMIT) {
                SomeGooglyCommon.LOGGER.info(
                        "Client eye-state debug: packet #{} entityId={} hasEyes={} resolvedEntity={}",
                        ordinal, packet.entityId(), packet.hasGooglyEyes(),
                        living == null ? "missing"
                                : BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()));
            }
            if (living == null) {
                PENDING_EYE_STATES.put(packet.entityId(), packet);
                return;
            }
            applyEyeState(living, packet);
        });
    }

    /** Apply an eye-state packet that arrived before Fabric created the corresponding client entity. */
    public static void onEntityLoaded(Entity entity) {
        EyeStatePacket packet = PENDING_EYE_STATES.remove(entity.getId());
        if (packet == null || !(entity instanceof LivingEntity living)) {
            return;
        }
        if (++appliedQueuedEyeStates <= EYE_STATE_LOG_LIMIT) {
            SomeGooglyCommon.LOGGER.info(
                    "Client eye-state debug: applying queued state #{} to entityId={} type={} hasEyes={}",
                    appliedQueuedEyeStates, entity.getId(),
                    BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), packet.hasGooglyEyes());
        }
        applyEyeState(living, packet);
    }

    public static void clearPendingEyeStates() {
        PENDING_EYE_STATES.clear();
        eyeStatePackets = 0;
        appliedQueuedEyeStates = 0;
    }

    private static void handle(EyeConfigSyncPacket packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            SomeGooglyCommon.LOGGER.info(
                    "Client eye-config debug: received {} selected entity configs", packet.configs().size());
            ClientEyeConfigs.replaceAll(packet.configs());
            ClientEyeRuntime.clear();
        });
    }

    private static void handle(EyeBehaviorTriggerPacket packet, NetworkManager.PacketContext context) {
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
        EntityPersistentData.get(living).putBoolean(EyeState.HAS_EYES, packet.hasGooglyEyes());
        EyeState.applyVariantRoll(living, packet.variantRoll());
        EyeState.applyOverridesTag(living, packet.overrides());
    }

    private static void disconnect(net.minecraft.network.chat.Component reason) {
        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().getConnection().disconnect(reason);
        }
    }
}
