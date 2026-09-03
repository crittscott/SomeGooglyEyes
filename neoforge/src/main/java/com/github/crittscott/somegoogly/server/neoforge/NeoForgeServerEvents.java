package com.github.crittscott.somegoogly.server.neoforge;

import com.github.crittscott.somegoogly.command.GooglyAdminCommand;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.server.EyeItemService;
import com.github.crittscott.somegoogly.server.ServerServices;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** NeoForge event wiring for the loader-neutral authoritative server services. */
public final class NeoForgeServerEvents {

    private NeoForgeServerEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(NeoForgeServerEvents::onRegisterCommands);
        gameBus.addListener(EventPriority.LOWEST,
                (PlayerInteractEvent.EntityInteract event) -> onEntityInteract(event));
        gameBus.addListener(NeoForgeServerEvents::onRightClickItem);
        gameBus.addListener(NeoForgeServerEvents::onLivingDrops);
        gameBus.addListener(NeoForgeServerEvents::onEntityJoinLevel);
        gameBus.addListener(NeoForgeServerEvents::onPlayerLoggedIn);
        gameBus.addListener(NeoForgeServerEvents::onPlayerLoggedOut);
        gameBus.addListener(NeoForgeServerEvents::onDatapackSync);
        gameBus.addListener(NeoForgeServerEvents::onServerStopping);
        gameBus.addListener(NeoForgeServerEvents::onServerTick);
        gameBus.addListener(NeoForgeServerEvents::onStartTracking);
        gameBus.addListener(NeoForgeServerEvents::onStopTracking);
        gameBus.addListener(NeoForgeServerEvents::onLivingDamage);
        gameBus.addListener(NeoForgeServerEvents::onLivingHeal);
        gameBus.addListener(NeoForgeServerEvents::onTrade);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        GooglyAdminCommand.register(event.getDispatcher());
    }

    /**
     * Lowest priority so a land-claim or region-protection mod that cancels the entity interaction
     * gets first refusal — this handler consumes the event as soon as it acts.
     */
    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof LivingEntity living)) {
            return;
        }
        InteractionResult result = EyeItemService.interact(
                event.getEntity(), event.getLevel(), event.getHand(), living);
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        InteractionResult result = EyeItemService.selfRemoveWithShears(event.getEntity(), event.getHand());
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    private static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity living = event.getEntity();
        EyeItemService.onDeath(living, event.getSource(), stack -> {
            ItemEntity drop = new ItemEntity(
                    living.level(), living.getX(), living.getY(), living.getZ(), stack);
            drop.setDefaultPickUpDelay();
            event.getDrops().add(drop);
        });
    }

    private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            ServerServices.onLivingEntityLoaded(living);
        }
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerServices.onPlayerJoined(player);
        }
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerServices.onPlayerLeft(player);
        }
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(ServerServices::syncEyeConfigs);
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        ServerServices.onServerStopping(event.getServer());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        ServerServices.onServerTick(event.getServer());
    }

    private static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getTarget() instanceof LivingEntity living) {
            ServerServices.onStartTracking(living, player);
        }
    }

    private static void onStopTracking(PlayerEvent.StopTracking event) {
        if (event.getTarget() instanceof LivingEntity living) {
            ServerServices.onStopTracking(living);
        }
    }

    private static void onLivingDamage(LivingDamageEvent.Post event) {
        LivingEntity living = event.getEntity();
        if (!living.level().isClientSide() && event.getSource().getEntity() instanceof Player) {
            ServerBehaviorScheduler.onPlayerHurt(living);
        }
    }

    private static void onLivingHeal(LivingHealEvent event) {
        LivingEntity living = event.getEntity();
        if (!living.level().isClientSide()) {
            ServerBehaviorScheduler.onHealed(living);
        }
    }

    private static void onTrade(TradeWithVillagerEvent event) {
        ServerBehaviorScheduler.onTrade(event.getAbstractVillager());
    }
}
