package com.github.crittscott.somegoogly.client.neoforge;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

/** NeoForge renderer access through the mod's 1.21.1 Access Transformer. */
public final class ClientRendererAccessImpl {

    private ClientRendererAccessImpl() {
    }

    public static Map<EntityType<?>, EntityRenderer<?>> renderers(EntityRenderDispatcher dispatcher) {
        return dispatcher.renderers;
    }

    public static Map<?, EntityRenderer<? extends Player>> skinMap(EntityRenderDispatcher dispatcher) {
        return dispatcher.playerRenderers;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addLayer(LivingEntityRenderer<?, ?> renderer, RenderLayer<?, ?> layer) {
        ((LivingEntityRenderer) renderer).addLayer((RenderLayer) layer);
    }
}
