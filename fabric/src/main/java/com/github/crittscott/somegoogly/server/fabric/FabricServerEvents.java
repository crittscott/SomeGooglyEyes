package com.github.crittscott.somegoogly.server.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.command.GooglyServerCommands;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.network.fabric.FabricNetworkTransport;
import com.github.crittscott.somegoogly.server.EyeItemService;
import com.github.crittscott.somegoogly.server.ServerServices;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Fabric event wiring for the loader-neutral authoritative server services. */
public final class FabricServerEvents {

    /**
     * Late phase for the eye-item entity interaction: Fabric protection mods register their
     * {@link UseEntityCallback} in the default phase, so running after it lets them veto first.
     */
    private static final ResourceLocation LATE_PHASE =
            ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "after_protection");

    private FabricServerEvents() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                GooglyServerCommands.register(dispatcher, registryAccess));
        UseEntityCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, LATE_PHASE);
        // Fabric's use callbacks fire before vanilla's spectator check, so callers must make it themselves.
        UseEntityCallback.EVENT.register(LATE_PHASE, (player, level, hand, entity, hitResult) ->
                !player.isSpectator() && entity instanceof LivingEntity living
                        ? EyeItemService.interact(player, level, hand, living)
                        : InteractionResult.PASS);
        UseItemCallback.EVENT.register((player, level, hand) ->
                player.isSpectator() ? InteractionResult.PASS : EyeItemService.selfRemoveWithShears(player, hand));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof LivingEntity living) {
                ServerServices.onLivingEntityLoaded(living);
            }
        });
        ServerLivingEntityEvents.MOB_CONVERSION.register((previous, converted, context) ->
                EyeState.copy(previous, converted));
        // Leaving the End replaces the player; eyes applied to a player are still lost on death.
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (alive) {
                EyeState.copy(oldPlayer, newPlayer);
            }
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (source.getEntity() instanceof Player) {
                ServerBehaviorScheduler.onPlayerHurt(entity);
            }
        });
        // Refuse a client without this network version before it joins the world.
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (!ServerConfigurationNetworking.canSend(handler, FabricNetworkTransport.Handshake.TYPE)) {
                handler.disconnect(Component.translatable("somegoogly.network.required_client"));
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ServerServices.syncEyeConfigs(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ServerServices.onPlayerLeft(handler.player));
        // Once per reload, after the eye definitions and the re-read server config are both applied.
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) ->
                ServerServices.broadcastEyeConfigsIfChanged(server));
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerServices::onServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(server -> ServerBehaviorScheduler.serverTick());
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            if (entity instanceof LivingEntity living) {
                ServerServices.onStartTracking(living, player);
            }
        });
        EntityTrackingEvents.STOP_TRACKING.register((entity, player) -> {
            if (entity instanceof LivingEntity living) {
                ServerBehaviorScheduler.onStopTracking(living);
            }
        });
    }
}
