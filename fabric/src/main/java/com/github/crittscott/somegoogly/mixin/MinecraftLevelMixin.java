package com.github.crittscott.somegoogly.mixin;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clear outgoing-world eye state when Fabric's client replaces its level. */
@Mixin(Minecraft.class)
abstract class MinecraftLevelMixin {

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void somegoogly$onLevelChanged(CallbackInfo callback) {
        ClientEyeRuntime.onLevelChanged();
    }
}
