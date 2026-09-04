package com.github.crittscott.somegoogly.client.compat.forge;

import com.github.crittscott.somegoogly.client.compat.gecko.GeckoGuard;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;

import java.util.List;

/** Forge soft-dependency gate for the shared GeckoLib integration; body lives in {@link GeckoGuard}. */
public final class GeckoCompatImpl {

    public static final boolean LOADED = ModList.get().isLoaded("geckolib");

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
