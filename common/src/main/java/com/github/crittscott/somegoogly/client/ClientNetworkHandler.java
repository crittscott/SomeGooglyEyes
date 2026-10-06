package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Applies the three server-to-client payloads on the client game thread. Loader payload registration runs on
 * both distributions, so it reaches these handlers only from inside lambda bodies, which link this class when a
 * payload arrives rather than when the handler is registered.
 */
public final class ClientNetworkHandler {

    private static final int MAX_PENDING_EYE_STATES = 1_024;
    private static final Map<UUID, EyeStatePacket> PENDING_EYE_STATES = new LinkedHashMap<>();

    private ClientNetworkHandler() {
    }

    public static void handleEyeConfigSync(EyeConfigSyncPacket packet) {
        ClientEyeConfigs.replaceAll(packet.configs(), packet.googlyEyesEnabled());
        ClientEyeRuntime.clear();
    }

    public static void handleEyeState(EyeStatePacket packet) {
        LivingEntity living = living(packet.entityId());
        if (living == null || !living.getUUID().equals(packet.entityUuid())) {
            queueEyeState(packet);
            return;
        }
        applyEyeState(living, packet);
    }

    public static void handleBehavior(EyeBehaviorTriggerPacket packet) {
        EyeBehavior behavior = EyeBehavior.byId(packet.behaviorId());
        LivingEntity living = living(packet.entityId());
        if (behavior == null || living == null) {
            return;
        }
        GooglyTracker tracker = ClientEyeRuntime.peek(living);
        if (tracker != null) {
            tracker.startBehavior(behavior, packet.duration(), packet.seed(), packet.elapsed());
        }
    }

    /** Apply eye state that reached the client before its entity was created. */
    public static void onEntityLoaded(Entity entity) {
        EyeStatePacket packet = PENDING_EYE_STATES.remove(entity.getUUID());
        if (packet != null && entity instanceof LivingEntity living) {
            applyEyeState(living, packet);
        }
    }

    public static void clearPendingEyeStates() {
        PENDING_EYE_STATES.clear();
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
            tracker.overrides = packet.snapshot().properties();
        }
    }

    private static void queueEyeState(EyeStatePacket packet) {
        PENDING_EYE_STATES.remove(packet.entityUuid());
        while (PENDING_EYE_STATES.size() >= MAX_PENDING_EYE_STATES) {
            Iterator<UUID> iterator = PENDING_EYE_STATES.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
        PENDING_EYE_STATES.put(packet.entityUuid(), packet);
    }
}
