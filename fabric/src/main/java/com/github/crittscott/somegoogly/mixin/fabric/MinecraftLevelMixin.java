package com.github.crittscott.somegoogly.mixin.fabric;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clear outgoing-world eye state when the client replaces its level, for which Fabric API has no
 * event.
 */
@Mixin(Minecraft.class)
abstract class MinecraftLevelMixin {

    // Mixin invokes this handler; unused parameters preserve its injection signature.
    @SuppressWarnings({"UnusedMethod", "UnusedVariable"})
    @Inject(method = "setLevel", at = @At("HEAD"))
    private void somegoogly$onLevelChanged(CallbackInfo callback) {
        ClientEyeRuntime.onLevelChanged();
    }
}
