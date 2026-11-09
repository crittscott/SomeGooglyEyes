package com.github.crittscott.somegoogly.mixin.fabric;

import com.github.crittscott.somegoogly.server.ServerServices;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric hook for a player starting to track an entity, after the entity's spawn packet has been sent.
 * Fabric API's {@code EntityTrackingEvents.START_TRACKING} fires at the head of {@code addPairing}, before the
 * spawn packet, so a packet sent from it reaches the client before the entity exists there and is dropped.
 */
@Mixin(ServerEntity.class)
abstract class ServerEntityPairingMixin {

    @Shadow
    @Final
    private Entity entity;

    @Inject(method = "addPairing", at = @At("TAIL"))
    private void somegoogly$afterAddPairing(ServerPlayer player, CallbackInfo callback) {
        ServerServices.onStartTracking(entity, player);
    }
}
