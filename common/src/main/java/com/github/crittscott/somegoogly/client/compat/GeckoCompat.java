package com.github.crittscott.somegoogly.client.compat;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.compat.gecko.GeckoIntegration;
import com.github.crittscott.somegoogly.config.ModVersionLookup;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Soft-dependency gate for the optional GeckoLib renderer integration.
 *
 * <p>{@link GeckoIntegration} — and the GeckoLib types it references — is touched only after the
 * {@link #LOADED} check, so it never loads when GeckoLib is absent. A GeckoLib present but broken logs
 * each affected operation once and renders its mobs without eyes rather than failing mod load.
 */
public final class GeckoCompat {

    private static final boolean LOADED = ModVersionLookup.versionForNamespace("geckolib").isPresent();

    static {
        if (LOADED) {
            SomeGooglyCommon.LOGGER.info("GeckoLib detected; enabling googly-eye support on GeckoLib renderers");
        }
    }

    private GeckoCompat() {
    }

    /** Bone names for a GeckoLib mob, or an empty list when GeckoLib is unavailable. */
    public static List<String> enumerate(EntityRenderer<?, ?> renderer, LivingEntity living) {
        if (!LOADED) {
            return List.of();
        }
        try {
            return GeckoIntegration.enumerate(renderer, living);
        } catch (Throwable failure) {
            ClientIntegrationFailures.warnOnce(
                    "GeckoLib", "model-part enumeration", renderer.getClass().getName(), failure);
            return List.of();
        }
    }

    /** Attach the googly-eye layer when this is a supported GeckoLib renderer. */
    public static boolean tryAddLayer(EntityRenderer<?, ?> renderer) {
        if (!LOADED) {
            return false;
        }
        try {
            return GeckoIntegration.tryAddLayer(renderer);
        } catch (Throwable failure) {
            ClientIntegrationFailures.warnOnce(
                    "GeckoLib", "render-layer installation", renderer.getClass().getName(), failure);
            return false;
        }
    }
}
