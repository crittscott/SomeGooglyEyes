package com.github.crittscott.somegoogly.client.compat;

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
 * support" rather than crashing.
 */
public final class GeckoCompat {

    public static final boolean LOADED = ModList.get().isLoaded("geckolib");

    private GeckoCompat() {
    }

    /** Attach the googly-eye layer if this is a GeckoLib renderer. No-op without GeckoLib. */
    public static boolean tryAddLayer(EntityRenderer<?> renderer) {
        if (!LOADED) {
            return false;
        }
        try {
            return GeckoIntegration.tryAddLayer(renderer);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Bone names for a GeckoLib mob (for the picker), or empty if not GeckoLib / unavailable. */
    public static List<String> enumerate(EntityRenderer<?> renderer, LivingEntity living) {
        if (!LOADED) {
            return List.of();
        }
        try {
            return GeckoIntegration.enumerate(renderer, living);
        } catch (Throwable t) {
            return List.of();
        }
    }
}
