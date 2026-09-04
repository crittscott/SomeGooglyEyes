package com.github.crittscott.somegoogly.server.forge;

import com.github.crittscott.somegoogly.command.GooglyAdminCommand;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.server.EyeItemService;
import com.github.crittscott.somegoogly.server.ServerServices;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

/** Forge event wiring for the loader-neutral authoritative server services. */
public final class ForgeServerEvents {

    private ForgeServerEvents() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ForgeServerEvents::onRegisterCommands);
        gameBus.addListener(EventPriority.LOWEST, ForgeServerEvents::onEntityInteract);
        gameBus.addListener(ForgeServerEvents::onRightClickItem);
        gameBus.addListener(ForgeServerEvents::onLivingDrops);
        gameBus.addListener(ForgeServerEvents::onEntityJoinLevel);
        gameBus.addListener(ForgeServerEvents::onPlayerLoggedIn);
        gameBus.addListener(ForgeServerEvents::onPlayerLoggedOut);
        gameBus.addListener(ForgeServerEvents::onDatapackSync);
        gameBus.addListener(ForgeServerEvents::onServerStopping);
        gameBus.addListener(ForgeServerEvents::onServerTick);
        gameBus.addListener(ForgeServerEvents::onStartTracking);
        gameBus.addListener(ForgeServerEvents::onStopTracking);
        gameBus.addListener(ForgeServerEvents::onLivingDamage);
        gameBus.addListener(ForgeServerEvents::onLivingHeal);
        gameBus.addListener(ForgeServerEvents::onTrade);
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
        event.getPlayers().forEach(ServerServices::syncEyeConfigs);
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        ServerServices.onServerStopping(event.getServer());
    }

    private static void onServerTick(TickEvent.ServerTickEvent.Post event) {
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

    private static void onLivingDamage(LivingDamageEvent event) {
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
