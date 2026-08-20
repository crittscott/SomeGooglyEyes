package com.github.crittscott.somegoogly.event;

import com.github.crittscott.somegoogly.command.GooglyAdminCommand;
import com.github.crittscott.somegoogly.config.EyeConfigReloadListener;
import com.github.crittscott.somegoogly.server.ServerServices;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Connects server lifecycle and entity-tracking events to the mod's server-owned systems: datapack
 * definition loading/sync, one-time entity eye initialization, command registration, behavior
 * scheduling, and picker cleanup. Gameplay-specific item and reaction events live in their dedicated
 * handlers.
 */
public class ServerEventHandler {

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new EyeConfigReloadListener());
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            ServerServices.syncEyeConfigs(event.getPlayer());
        } else {
            for (ServerPlayer player : event.getPlayerList().getPlayers()) {
                ServerServices.syncEyeConfigs(player);
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        ServerServices.onLivingEntityLoaded(living);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        GooglyAdminCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // A disappearing picker client must never strand a frozen mob; the server releases it.
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerServices.onPlayerLeft(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerServices.onPlayerJoined(player);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        ServerServices.onServerStopping(event.getServer());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ServerServices.onServerTick(event.getServer());
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof LivingEntity living) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerServices.onStartTracking(living, player);
    }

    @SubscribeEvent
    public void onStopTracking(PlayerEvent.StopTracking event) {
        if (event.getTarget() instanceof LivingEntity living) {
            ServerServices.onStopTracking(living);
        }
    }
}
