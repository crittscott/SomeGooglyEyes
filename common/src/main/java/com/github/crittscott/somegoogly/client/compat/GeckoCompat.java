package com.github.crittscott.somegoogly.client.compat;

import com.github.crittscott.somegoogly.client.compat.gecko.GeckoIntegration;
import com.github.crittscott.somegoogly.config.ModVersionLookup;
import java.util.List;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Soft-dependency gate for the optional GeckoLib renderer integration.
 *
 * <p>{@link GeckoIntegration} — and the GeckoLib types it references — is touched only after the
 * {@link #LOADED} check, so it never loads when GeckoLib is absent. Bone enumeration runs other mods' model
 * code against a GeckoLib that may be newer than the build's, so a failure there is logged once and leaves
 * the picker without that mob's bones. Rendering through the GeckoLib layer is not guarded.
 */
public final class GeckoCompat {

    private static final boolean LOADED = ModVersionLookup.versionForNamespace("geckolib").isPresent();

    private GeckoCompat() {
    }

    /** Drop the GeckoLib model caches, if GeckoLib is present; called whenever the models are replaced. */
    public static void clearCaches() {
        if (LOADED) {
            GeckoIntegration.clearCaches();
        }
    }

    /** Bone names for a GeckoLib mob, or an empty list when GeckoLib is unavailable. */
    public static List<String> enumerate(EntityRenderer<?> renderer, LivingEntity living) {
        if (!LOADED) {
            return List.of();
        }
        try {
            return GeckoIntegration.enumerate(renderer, living);
        } catch (RuntimeException | LinkageError failure) {
            ClientIntegrationFailures.warnOnce(
                    "GeckoLib", "model-part enumeration", renderer.getClass().getName(), failure);
            return List.of();
        }
    }

    /**
     * Whether GeckoLib is present. Loader code checks this before registering its GeckoLib render-layer
     * listener, whose class names GeckoLib's loader-specific event type.
     */
    public static boolean isLoaded() {
        return LOADED;
    }
}
