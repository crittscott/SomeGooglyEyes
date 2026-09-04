package com.github.crittscott.somegoogly.client.compat.gecko;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Loader-neutral body of the per-loader {@code GeckoCompatImpl} soft-dependency gates. Each loader's
 * {@code GeckoCompatImpl} supplies only its own "is GeckoLib installed" probe (the one API that differs
 * between Fabric's {@code FabricLoader} and the Forge-family {@code ModList}) and delegates the rest here.
 *
 * <p>{@link GeckoIntegration} — and the GeckoLib types it references — is touched only inside a
 * {@code loaded} guard, so it never loads when GeckoLib is absent. A GeckoLib present but broken warns
 * once and renders GeckoLib mobs without eyes rather than failing mod load.
 */
public final class GeckoGuard {

    private static boolean warnedIntegrationFailure;

    private GeckoGuard() {
    }

    /** One-time startup note that GeckoLib support is active. */
    public static void announce(boolean loaded) {
        if (loaded) {
            SomeGooglyCommon.LOGGER.info(
                    "GeckoLib detected; enabling googly-eye support on GeckoLib renderers");
        }
    }

    public static List<String> enumerate(boolean loaded, EntityRenderer<?> renderer, LivingEntity living) {
        if (!loaded) {
            return List.of();
        }
        try {
            return GeckoIntegration.enumerate(renderer, living);
        } catch (Throwable failure) {
            warnIntegrationFailure(failure);
            return List.of();
        }
    }

    public static boolean tryAddLayer(boolean loaded, EntityRenderer<?> renderer) {
        if (!loaded) {
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
