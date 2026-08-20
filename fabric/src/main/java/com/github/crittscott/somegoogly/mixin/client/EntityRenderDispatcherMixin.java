package com.github.crittscott.somegoogly.mixin.client;

import com.github.crittscott.somegoogly.client.ClientRenderLayers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Installs eye layers after Minecraft has rebuilt the client entity renderer map. */
@Mixin(EntityRenderDispatcher.class)
abstract class EntityRenderDispatcherMixin {

    @Inject(method = "onResourceManagerReload", at = @At("TAIL"))
    private void somegoogly$afterRendererReload(ResourceManager manager, CallbackInfo callback) {
        ClientRenderLayers.install((EntityRenderDispatcher) (Object) this);
    }
}
