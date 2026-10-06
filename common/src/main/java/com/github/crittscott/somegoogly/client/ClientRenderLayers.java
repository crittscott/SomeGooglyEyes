package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.client.render.LayerGooglyEyes;
import com.github.crittscott.somegoogly.client.render.resolver.Resolvers;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Installs the vanilla-model eye layer on living renderers after renderer rebuilds. GeckoLib renderers are
 * not living renderers; they receive their layer from GeckoLib's own compile-render-layers event.
 */
public final class ClientRenderLayers {

    private static final Set<EntityRenderer<?, ?>> INSTALLED =
            Collections.newSetFromMap(new WeakHashMap<>());

    private ClientRenderLayers() {
    }

    /** Clear the attachment caches once per renderer rebuild; they are keyed by model, so before or after it. */
    public static void clearCaches() {
        Resolvers.clearCaches();
    }

    /**
     * Install the vanilla-model eye layer on one renderer; any renderer that is not a living renderer is
     * ignored. Duplicate-safe through the weak {@code INSTALLED} set. Client-hidden entities still get the
     * layer; the per-frame render gate hides them, so a config change applies without a renderer rebuild.
     */
    @SuppressWarnings("rawtypes")
    public static void install(EntityRenderer<?, ?> renderer) {
        if (renderer instanceof LivingEntityRenderer livingRenderer) {
            addLiving(livingRenderer);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addLiving(LivingEntityRenderer renderer) {
        if (!INSTALLED.add(renderer)) {
            return;
        }
        LayerGooglyEyes eyes = new LayerGooglyEyes<>(renderer);
        List<RenderLayer> layers = renderer.layers;
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i) instanceof SlimeOuterLayer) {
                layers.add(i, eyes);
                return;
            }
        }
        renderer.addLayer(eyes);
    }
}
