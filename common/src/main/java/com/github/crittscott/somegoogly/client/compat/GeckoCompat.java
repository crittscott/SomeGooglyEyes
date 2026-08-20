package com.github.crittscott.somegoogly.client.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Loader bridge for the optional GeckoLib renderer integration. */
public final class GeckoCompat {

    private GeckoCompat() {
    }

    /** Bone names for a GeckoLib mob, or an empty list when GeckoLib is unavailable. */
    @ExpectPlatform
    public static List<String> enumerate(EntityRenderer<?> renderer, LivingEntity living) {
        throw new AssertionError();
    }

    /** Attach the googly-eye layer when this is a supported GeckoLib renderer. */
    @ExpectPlatform
    public static boolean tryAddLayer(EntityRenderer<?> renderer) {
        throw new AssertionError();
    }
}
