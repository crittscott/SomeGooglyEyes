package com.github.crittscott.somegoogly.client.compat.neoforge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

import java.util.List;

/** NeoForge soft-dependency gate for the shared GeckoLib integration. */
public final class GeckoCompatImpl {

    public static final boolean LOADED = ModList.get().isLoaded("geckolib");

    private static boolean warnedIntegrationFailure;

    static {
        if (LOADED) {
            SomeGooglyCommon.LOGGER.info("GeckoLib detected; enabling googly-eye support on GeckoLib renderers");
        }
    }

    private GeckoCompatImpl() {
    }

    public static List<String> enumerate(EntityRenderer<?> renderer, LivingEntity living) {
        if (!LOADED) {
            return List.of();
        }
        try {
            return GeckoIntegration.enumerate(renderer, living);
        } catch (Throwable failure) {
            warnIntegrationFailure(failure);
            return List.of();
        }
    }

    public static boolean tryAddLayer(EntityRenderer<?> renderer) {
        if (!LOADED) {
            return false;
        }
        try {
            return GeckoIntegration.tryAddLayer(renderer);
        } catch (Throwable failure) {
            warnIntegrationFailure(failure);
            return false;
        }
    }

    private static void warnIntegrationFailure(Throwable failure) {
        if (warnedIntegrationFailure) {
            return;
        }
        warnedIntegrationFailure = true;
        SomeGooglyCommon.LOGGER.warn(
                "GeckoLib is installed but its integration failed; GeckoLib mobs will render without googly eyes",
                failure);
    }
}
