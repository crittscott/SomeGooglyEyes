package com.github.crittscott.somegoogly.client.compat.forge;

import com.github.crittscott.somegoogly.client.compat.gecko.GeckoIntegration;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Forge implementation of the GeckoLib bridge, compiled with the shared GeckoLib source folder. */
public final class GeckoCompatImpl {

    private GeckoCompatImpl() {
    }

    public static List<String> enumerateBones(EntityRenderer<?> renderer, LivingEntity living) {
        return GeckoIntegration.enumerate(renderer, living);
    }
}
