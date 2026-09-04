package com.github.crittscott.somegoogly.client.compat.fabric;

import com.github.crittscott.somegoogly.client.compat.gecko.GeckoGuard;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Fabric soft-dependency gate for the shared GeckoLib integration; body lives in {@link GeckoGuard}. */
public final class GeckoCompatImpl {

    public static final boolean LOADED = FabricLoader.getInstance().isModLoaded("geckolib");

    static {
        GeckoGuard.announce(LOADED);
    }

    private GeckoCompatImpl() {
    }

    public static List<String> enumerate(EntityRenderer<?> renderer, LivingEntity living) {
        return GeckoGuard.enumerate(LOADED, renderer, living);
    }

    public static boolean tryAddLayer(EntityRenderer<?> renderer) {
        return GeckoGuard.tryAddLayer(LOADED, renderer);
    }
}
