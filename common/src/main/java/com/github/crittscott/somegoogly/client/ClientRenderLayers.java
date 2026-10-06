package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.client.compat.GeckoCompat;
import com.github.crittscott.somegoogly.client.picker.PickerLayer;
import com.github.crittscott.somegoogly.client.render.LayerGooglyEyes;
import com.github.crittscott.somegoogly.client.render.resolver.Resolvers;
import com.github.crittscott.somegoogly.config.ClientConfig;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

/** Installs vanilla-model, picker, and optional GeckoLib eye layers after renderer rebuilds. */
public final class ClientRenderLayers {

    private static final Set<EntityRenderer<?, ?>> INSTALLED =
            Collections.newSetFromMap(new WeakHashMap<>());

    private ClientRenderLayers() {
    }

    /** Clear the attachment caches; call once before a renderer rebuild installs layers again. */
    public static void clearCaches() {
        Resolvers.clearCaches();
    }

    /**
     * Reinstall every eye layer across the whole dispatcher after a renderer rebuild: clear the attachment
     * caches, then {@link #install} each player skin renderer and each per-type renderer.
     */
    public static void installAll(EntityRenderDispatcher dispatcher) {
        clearCaches();
        dispatcher.playerRenderers.values().forEach(renderer -> install(EntityType.PLAYER, renderer));
        dispatcher.renderers.forEach(ClientRenderLayers::install);
    }

    /**
     * Install the eye layers on one renderer: the vanilla-model and picker layers on a living renderer,
     * the optional GeckoLib layer on any other. Duplicate-safe through the weak {@code INSTALLED} set, and
     * skips any entity hidden by {@link ClientConfig}.
     */
    @SuppressWarnings("rawtypes")
    public static void install(EntityType<?> entityType, EntityRenderer<?, ?> renderer) {
        if (ClientConfig.isEntityDisabled(BuiltInRegistries.ENTITY_TYPE.getKey(entityType))) {
            return;
        }
        if (renderer instanceof LivingEntityRenderer livingRenderer) {
            addLiving(livingRenderer);
        } else if (INSTALLED.add(renderer)) {
            GeckoCompat.tryAddLayer(renderer);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addLiving(LivingEntityRenderer renderer) {
        if (!INSTALLED.add(renderer)) {
            return;
        }
        LayerGooglyEyes eyes = new LayerGooglyEyes<>(renderer);
        PickerLayer picker = new PickerLayer<>(renderer);
        List<RenderLayer> layers = renderer.layers;
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i) instanceof SlimeOuterLayer) {
                layers.add(i, eyes);
                layers.add(i + 1, picker);
                return;
            }
        }
        renderer.addLayer(eyes);
        renderer.addLayer(picker);
    }
}
