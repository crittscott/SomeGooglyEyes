package com.github.crittscott.somegoogly.mixin.fabric;

import com.github.crittscott.somegoogly.client.fabric.FabricClientCommands;
import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Give chat parsing and completion the {@code /sg} picker verbs each time the server's command tree
 * arrives. Fabric API's own merge of client commands into that tree loses a client subtree whose
 * root the server also sends, and {@code /sg} always is one; Fabric API has no event after the tree
 * is replaced.
 */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerCommandsMixin {

    @Shadow
    public abstract CommandDispatcher<SharedSuggestionProvider> getCommands();

    @Inject(method = "handleCommands", at = @At("RETURN"))
    private void somegoogly$addPickerCommands(CallbackInfo callback) {
        FabricClientCommands.addPickerToServerTree(getCommands());
    }
}
