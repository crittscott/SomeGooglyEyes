package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.BoatEyeLayout;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.network.EyeAngerPacket;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeSettingsPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Applies the server-to-client payloads on the client game thread. Loader payload registration runs on
 * both distributions, so it reaches these handlers only from inside lambda bodies, which link this class when a
 * payload arrives rather than when the handler is registered.
 */
public final class ClientNetworkHandler {

    private ClientNetworkHandler() {
    }

    /** Record a server-reported anger change; an unknown entity is ignored. */
    public static void handleAnger(EyeAngerPacket packet) {
        LivingEntity living = living(packet.entityId());
        if (living != null) {
            ClientEyeRuntime.setAngry(living, packet.angry());
        }
    }

    /** Retain a behavior for a known entity, including one that has not rendered yet. */
    public static void handleBehavior(EyeBehaviorTriggerPacket packet) {
        LivingEntity living = living(packet.entityId());
        if (living != null) {
            if (packet.target() == EyeBehaviorTriggerPacket.Target.ENTITY) {
                ClientEyeRuntime.setBehavior(living, packet.behavior(), packet.seed(), packet.elapsed());
            } else {
                ClientEyeRuntime.setHandBehavior(living, packet.target().hand(), packet.behavior(), packet.seed(), packet.elapsed());
            }
        }
    }

    /** Replace the client's eye definitions and drop every tracker built from the old set. */
    public static void handleEyeConfigSync(EyeConfigSyncPacket packet) {
        ClientEyeConfigs.replaceAll(packet.configs());
        ClientEyeRuntime.clear();
    }

    /** Take the server's master switch and behavior settings; trackers read them afresh each tick. */
    public static void handleEyeSettings(EyeSettingsPacket packet) {
        ClientEyeConfigs.applySettings(packet.googlyEyesEnabled(), packet.behaviors());
    }

    /** Apply snapshots only to living entities and supported vanilla boats; unknown ids are ignored. */
    public static void handleEyeState(EyeStatePacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        Entity entity = level == null ? null : level.getEntity(packet.entityId());
        if (!(entity instanceof LivingEntity) && !BoatEyeLayout.supports(entity)) {
            return;
        }
        EyeState.applySnapshot(entity, packet.snapshot());
        if (entity instanceof LivingEntity living) {
            GooglyTracker tracker = ClientEyeRuntime.peek(living);
            if (tracker != null) {
                tracker.overrides = packet.snapshot().properties();
            }
        } else {
            ClientEyeRuntime.onBoatState(entity, packet.snapshot());
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
