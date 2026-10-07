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

/**
 * Applies the three server-to-client payloads on the client game thread. Loader payload registration runs on
 * both distributions, so it reaches these handlers only from inside lambda bodies, which link this class when a
 * payload arrives rather than when the handler is registered.
 */
public final class ClientNetworkHandler {

    private ClientNetworkHandler() {
    }

    /** Replace the client's eye definitions and master switch, and drop every tracker built from the old set. */
    public static void handleEyeConfigSync(EyeConfigSyncPacket packet) {
        ClientEyeConfigs.replaceAll(packet.configs(), packet.googlyEyesEnabled());
        ClientEyeRuntime.clear();
    }

    /** Apply a full eye-state snapshot to a known living entity; an unknown id is ignored. */
    public static void handleEyeState(EyeStatePacket packet) {
        LivingEntity living = living(packet.entityId());
        if (living == null) {
            return;
        }
        EyeState.applySnapshot(living, packet.snapshot());
        GooglyTracker tracker = ClientEyeRuntime.peek(living);
        if (tracker != null) {
            tracker.overrides = packet.snapshot().properties();
        }
    }

    /** Start a behavior on the entity's tracker; an unknown behavior, entity, or untracked entity is ignored. */
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

    private static LivingEntity living(int entityId) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        Entity entity = level.getEntity(entityId);
        return entity instanceof LivingEntity living ? living : null;
    }
}
