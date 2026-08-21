package com.github.crittscott.somegoogly.server.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.command.GooglyAdminCommand;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.platform.EntityPersistentData;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.Set;

/** Fabric event wiring for the loader-neutral authoritative server services. */
public final class FabricServerEvents {

    private static final int ENTITY_TYPE_LOG_LIMIT = 30;
    private static final Set<String> LOGGED_LOAD_TYPES = new HashSet<>();
    private static final Set<String> LOGGED_TRACKING_TYPES = new HashSet<>();

    private FabricServerEvents() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                GooglyAdminCommand.register(dispatcher));
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) ->
                entity instanceof LivingEntity living
                        ? EyeItemService.interact(player, level, hand, living)
                        : net.minecraft.world.InteractionResult.PASS);
        ServerLivingEntityEvents.AFTER_DEATH.register(EyeItemService::onDeath);
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof LivingEntity living) {
                onLivingEntityLoaded(living);
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
                logStartTracking(living, player);
                ServerServices.onStartTracking(living, player);
            }
        });
        EntityTrackingEvents.STOP_TRACKING.register((entity, player) -> {
            if (entity instanceof LivingEntity living) {
                ServerServices.onStopTracking(living);
            }
        });
    }

    private static void onLivingEntityLoaded(LivingEntity living) {
        boolean previouslyInitialized = EntityPersistentData.get(living).contains(EyeState.HAS_EYES);
        boolean eligible = ServerEyeConfigs.canEverWearEyes(living);
        ServerServices.onLivingEntityLoaded(living);

        String type = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString();
        if (LOGGED_LOAD_TYPES.size() < ENTITY_TYPE_LOG_LIMIT && LOGGED_LOAD_TYPES.add(type)) {
            SomeGooglyCommon.LOGGER.info(
                    "Fabric entity-load debug: type={}, entityId={}, previouslyInitialized={}, configuredEligibility={}, percent={}, hasEyes={}",
                    type, living.getId(), previouslyInitialized, eligible,
                    ServerConfig.percentFor(BuiltInRegistries.ENTITY_TYPE.getKey(living.getType())),
                    EyeState.hasEyes(living));
        }
    }

    private static void logStartTracking(LivingEntity living, ServerPlayer player) {
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString();
        if (LOGGED_TRACKING_TYPES.size() < ENTITY_TYPE_LOG_LIMIT && LOGGED_TRACKING_TYPES.add(type)) {
            SomeGooglyCommon.LOGGER.info(
                    "Fabric tracking debug: player={}, type={}, entityId={}, hasEyes={}, protocolReady={}",
                    player.getGameProfile().getName(), type, living.getId(), EyeState.hasEyes(living),
                    NetworkHandler.ready(player));
        }
    }
}
