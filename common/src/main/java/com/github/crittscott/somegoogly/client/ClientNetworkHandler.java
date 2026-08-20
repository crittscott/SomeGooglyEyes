package com.github.crittscott.somegoogly.client;

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
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Physical-client-only registration and application of server-to-client payloads. */
public final class ClientNetworkHandler {

    private static boolean registered;

    private ClientNetworkHandler() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
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
        String version = buffer.readUtf(32);
        context.queue(() -> {
            if (!NetworkHandler.PROTOCOL_VERSION.equals(version)) {
                disconnect(NetworkHandler.protocolMismatch("client", NetworkHandler.PROTOCOL_VERSION, version));
                return;
            }
            FriendlyByteBuf reply = NetworkHandler.newBuffer();
            reply.writeUtf(NetworkHandler.PROTOCOL_VERSION);
            NetworkManager.sendToServer(NetworkHandler.PROTOCOL_ACK, reply);
        });
    }

    private static void handle(EyeStatePacket packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            LivingEntity living = living(packet.entityId());
            if (living == null) {
                return;
            }
            EntityPersistentData.get(living).putBoolean(EyeState.HAS_EYES, packet.hasGooglyEyes());
            EyeState.applyVariantRoll(living, packet.variantRoll());
            EyeState.applyOverridesTag(living, packet.overrides());
        });
    }

    private static void handle(EyeConfigSyncPacket packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
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

    private static void disconnect(net.minecraft.network.chat.Component reason) {
        if (Minecraft.getInstance().getConnection() != null) {
            Minecraft.getInstance().getConnection().getConnection().disconnect(reason);
        }
    }
}
