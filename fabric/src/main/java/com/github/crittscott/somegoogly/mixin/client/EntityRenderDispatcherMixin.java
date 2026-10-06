package com.github.crittscott.somegoogly.mixin.client;

import com.github.crittscott.somegoogly.client.ClientRenderLayers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Refreshes shared caches and installs layers Fabric API's living-renderer callback does not reach, such as
 * optional GeckoLib ones, after a renderer-map rebuild.
 */
@Mixin(EntityRenderDispatcher.class)
abstract class EntityRenderDispatcherMixin {

    @Shadow
    private Map<EntityType<?>, EntityRenderer<?, ?>> renderers;

    @Shadow
    private Map<PlayerSkin.Model, EntityRenderer<? extends Player, ?>> playerRenderers;

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void somegoogly$afterRendererReload(ResourceManager manager, CallbackInfo callback) {
        ClientRenderLayers.clearCaches();
        playerRenderers.values().forEach(ClientRenderLayers::install);
        renderers.values().forEach(ClientRenderLayers::install);
    }
}
