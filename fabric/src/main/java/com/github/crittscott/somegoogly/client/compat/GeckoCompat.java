package com.github.crittscott.somegoogly.client.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Fabric soft-dependency gate for the shared GeckoLib integration. */
public final class GeckoCompat {

    public static final boolean LOADED = FabricLoader.getInstance().isModLoaded("geckolib");

    private GeckoCompat() {
    }

    public static List<String> enumerate(EntityRenderer<?> renderer, LivingEntity living) {
        if (!LOADED) {
            return List.of();
        }
        try {
            return GeckoIntegration.enumerate(renderer, living);
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    public static boolean tryAddLayer(EntityRenderer<?> renderer) {
        if (!LOADED) {
            return false;
        }
        try {
            return GeckoIntegration.tryAddLayer(renderer);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
