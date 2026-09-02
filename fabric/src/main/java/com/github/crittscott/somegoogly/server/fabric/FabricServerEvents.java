package com.github.crittscott.somegoogly.server.fabric;

import com.github.crittscott.somegoogly.command.GooglyAdminCommand;
import com.github.crittscott.somegoogly.server.EyeItemService;
import com.github.crittscott.somegoogly.server.ServerServices;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.entity.LivingEntity;

/** Fabric event wiring for the loader-neutral authoritative server services. */
public final class FabricServerEvents {

    private FabricServerEvents() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                GooglyAdminCommand.register(dispatcher));
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
                entity instanceof LivingEntity living
                        ? EyeItemService.interact(player, level, hand, living)
                        : net.minecraft.world.InteractionResult.PASS);
        ServerLivingEntityEvents.AFTER_DEATH.register((mob, source) ->
                EyeItemService.onDeath(mob, source, mob::spawnAtLocation));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof LivingEntity living) {
                ServerServices.onLivingEntityLoaded(living);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ServerServices.onPlayerJoined(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ServerServices.onPlayerLeft(handler.player));
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) ->
                ServerServices.syncEyeConfigs(player));
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerServices::onServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(ServerServices::onServerTick);
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            if (entity instanceof LivingEntity living) {
                ServerServices.onStartTracking(living, player);
            }
        });
        EntityTrackingEvents.STOP_TRACKING.register((entity, player) -> {
            if (entity instanceof LivingEntity living) {
                ServerServices.onStopTracking(living);
            }
        });
    }
}
