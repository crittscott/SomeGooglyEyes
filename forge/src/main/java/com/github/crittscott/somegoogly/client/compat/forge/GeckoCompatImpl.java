package com.github.crittscott.somegoogly.client.compat.forge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import java.util.List;

/**
 * Soft-dependency gate for GeckoLib. Contains <b>no</b> GeckoLib references, so it always loads;
 * the GeckoLib-touching code in {@link GeckoIntegration} is only reached when {@link #LOADED} is true,
 * so its classes (and GeckoLib's) load only when GeckoLib is installed.
 *
 * <p>Calls are wrapped in {@code try/catch} so a GeckoLib API mismatch degrades to "no GeckoLib
 * support" rather than crashing; the first such failure is logged once.
 */
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

    /** Bone names for a GeckoLib mob (for the picker), or empty if not GeckoLib / unavailable. */
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

    /** Attach the googly-eye layer if this is a GeckoLib renderer. No-op without GeckoLib. */
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
