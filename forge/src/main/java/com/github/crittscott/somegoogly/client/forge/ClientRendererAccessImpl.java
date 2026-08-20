package com.github.crittscott.somegoogly.client.forge;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

/** Forge renderer access through the public members supplied by Forge's patches. */
public final class ClientRendererAccessImpl {

    private ClientRendererAccessImpl() {
    }

    public static Map<EntityType<?>, EntityRenderer<?>> renderers(EntityRenderDispatcher dispatcher) {
        return dispatcher.renderers;
    }

    public static Map<String, EntityRenderer<? extends Player>> skinMap(EntityRenderDispatcher dispatcher) {
        return dispatcher.getSkinMap();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addLayer(LivingEntityRenderer<?, ?> renderer, RenderLayer<?, ?> layer) {
        ((LivingEntityRenderer) renderer).addLayer((RenderLayer) layer);
    }
}
