package com.github.crittscott.somegoogly.eye.state;

import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import net.minecraft.world.entity.LivingEntity;

/**
 * Broadcasts a mob's current eye state to its tracking clients through the cross-loader network.
 */
public final class EyeStateSync {

    private EyeStateSync() {
    }

    public static void sync(LivingEntity entity, boolean hasEyes, float variantRoll, AppearanceOverride overrides) {
        NetworkHandler.sendEyeStateTrackingAndSelf(entity,
                new EyeStatePacket(entity.getId(), hasEyes, variantRoll, overrides));
    }
}
