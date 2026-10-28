package com.github.crittscott.somegoogly.mixin;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/** Clears world-owned eye simulations when Fabric replaces or unloads the client level. */
@Mixin(Minecraft.class)
abstract class ClientLevelLifecycleMixin {

    @Inject(method = "updateLevelInEngines", at = @At("HEAD"))
    private void somegoogly$onLevelChanged(@Nullable ClientLevel level, CallbackInfo callback) {
        ClientEyeRuntime.onLevelChanged();
    }
}
