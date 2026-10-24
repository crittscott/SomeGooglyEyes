package com.github.crittscott.somegoogly.client.compat;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ModVersionLookup;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Soft-dependency gate for the optional GeckoLib renderer integration.
 *
 * <p>The GeckoLib-typed code lives in the shared {@code gecko} source folder each loader compiles, reached
 * through {@link #enumerateBones} and the loaders' render-layer listeners. It is touched only after the
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
    public static List<String> enumerate(EntityRenderer<?> renderer, LivingEntity living) {
        if (!LOADED) {
            return List.of();
        }
        try {
            return enumerateBones(renderer, living);
        } catch (Throwable failure) {
            ClientIntegrationFailures.warnOnce(
                    "GeckoLib", "model-part enumeration", renderer.getClass().getName(), failure);
            return List.of();
        }
    }

    /** The loader's call into the shared GeckoLib bone enumeration; only called once GeckoLib is known present. */
    @ExpectPlatform
    public static List<String> enumerateBones(EntityRenderer<?> renderer, LivingEntity living) {
        throw new AssertionError();
    }

    /**
     * Whether GeckoLib is present. Loader code checks this before registering its GeckoLib render-layer
     * listener, whose class names GeckoLib's loader-specific event type.
     */
    public static boolean isLoaded() {
        return LOADED;
    }
}
