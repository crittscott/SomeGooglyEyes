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
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Installs vanilla-model, picker, and optional GeckoLib eye layers after renderer rebuilds. */
public final class ClientRenderLayers {

    private static final Set<EntityRenderer<?>> INSTALLED =
            Collections.newSetFromMap(new WeakHashMap<>());

    private ClientRenderLayers() {
    }

    @SuppressWarnings("rawtypes")
    public static void install(EntityRenderDispatcher dispatcher) {
        Resolvers.clearCaches();
        HashSet<LivingEntityRenderer> playerRenderers = new HashSet<>();

        if (!ClientConfig.isEntityDisabled(new ResourceLocation("minecraft", "player"))) {
            Map<String, EntityRenderer<? extends Player>> skinMap = ClientRendererAccess.skinMap(dispatcher);
            for (EntityRenderer<? extends Player> renderer : skinMap.values()) {
                if (renderer instanceof PlayerRenderer playerRenderer) {
                    addLiving(playerRenderer);
                    playerRenderers.add(playerRenderer);
                }
            }
        }

        ClientRendererAccess.renderers(dispatcher).forEach((entityType, renderer) -> {
            if (playerRenderers.contains(renderer)) {
                return;
            }
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            if (ClientConfig.isEntityDisabled(id)) {
                return;
            }
            if (renderer instanceof LivingEntityRenderer livingRenderer) {
                addLiving(livingRenderer);
            } else if (INSTALLED.add(renderer)) {
                GeckoCompat.tryAddLayer(renderer);
            }
        });
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
        ClientRendererAccess.addLayer(renderer, eyes);
        ClientRendererAccess.addLayer(renderer, picker);
    }
}
