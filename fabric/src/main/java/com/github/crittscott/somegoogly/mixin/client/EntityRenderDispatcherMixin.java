package com.github.crittscott.somegoogly.mixin.client;

import com.github.crittscott.somegoogly.client.ClientRenderLayers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Refreshes shared caches and installs layers Fabric API's living-renderer callback does not reach, such as
 * optional GeckoLib ones, after a renderer-map rebuild.
 */
@Mixin(EntityRenderDispatcher.class)
abstract class EntityRenderDispatcherMixin {

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void somegoogly$afterRendererReload(ResourceManager manager, CallbackInfo callback) {
        ClientRenderLayers.installAll((EntityRenderDispatcher) (Object) this);
    }
}
