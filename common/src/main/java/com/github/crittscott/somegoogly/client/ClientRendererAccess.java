package com.github.crittscott.somegoogly.client;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

/** Loader bridge for renderer members that Forge patches public and Fabric leaves private/protected. */
public final class ClientRendererAccess {

    private ClientRendererAccess() {
    }

    @ExpectPlatform
    public static Map<EntityType<?>, EntityRenderer<?>> renderers(EntityRenderDispatcher dispatcher) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Map<?, EntityRenderer<? extends Player>> skinMap(EntityRenderDispatcher dispatcher) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void addLayer(LivingEntityRenderer<?, ?> renderer, RenderLayer<?, ?> layer) {
        throw new AssertionError();
    }
}
