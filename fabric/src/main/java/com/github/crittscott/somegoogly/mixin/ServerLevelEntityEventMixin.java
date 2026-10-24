package com.github.crittscott.somegoogly.mixin;

import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Passes every vanilla entity event the server broadcasts to its viewers (love hearts, a golem's flower,
 * and the like) to the behavior scheduler, which reacts to the few it knows. Fabric API has no event for
 * these broadcasts.
 */
@Mixin(ServerLevel.class)
abstract class ServerLevelEntityEventMixin {

    @Inject(method = "broadcastEntityEvent", at = @At("HEAD"))
    private void somegoogly$onEntityEvent(Entity entity, byte eventId, CallbackInfo callback) {
        ServerBehaviorScheduler.onEntityEvent(entity, eventId);
    }
}
