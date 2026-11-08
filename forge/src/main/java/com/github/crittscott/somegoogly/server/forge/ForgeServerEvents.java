package com.github.crittscott.somegoogly.server.forge;

import com.github.crittscott.somegoogly.command.GooglyServerCommands;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.item.EyeItemService;
import com.github.crittscott.somegoogly.server.ServerServices;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

/** Forge event wiring for the loader-neutral authoritative server services. */
public final class ForgeServerEvents {

    private ForgeServerEvents() {
    }

    /** A joining player gets the current view; a reload broadcasts once, only if something changed. */
    private static void onDatapackSync(OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player != null) {
            ServerServices.syncJoiningPlayer(player);
        } else {
            ServerServices.broadcastEyeSyncIfChanged(event.getPlayerList().getServer());
        }
    }

    /**
     * Lowest priority so a land-claim or region-protection mod that cancels the entity interaction
     * gets first refusal — this handler consumes the event as soon as it acts.
     */
    private static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        InteractionResult result = EyeItemService.interact(
                event.getEntity(), event.getLevel(), event.getHand(), event.getTarget());
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    /**
     * An armor stand takes the held item in its own {@code interactAt}, which ends the click before
     * {@link #onEntityInteract} can see it, so a stand is handled at this earlier event. Other entities stay
     * with the later one: their click goes on to {@code interact} whatever happens here, and a mob just
     * harvested here would then be sheared or otherwise used by the same click.
     */
    private static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getTarget() instanceof ArmorStand)) {
            return;
        }
        InteractionResult result = EyeItemService.interact(
                event.getEntity(), event.getLevel(), event.getHand(), event.getTarget());
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            ServerServices.onLivingEntityLoaded(living);
        }
    }

    private static void onLivingConversion(LivingConversionEvent.Post event) {
        ServerServices.onConverted(event.getEntity(), event.getOutcome());
    }

    private static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player) {
            ServerBehaviorScheduler.onPlayerHurt(event.getEntity());
        }
    }

    private static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity living = event.getEntity();
        for (ItemEntity drop : event.getDrops()) {
            EyeItemService.markHeadDrop(living, drop.getItem());
        }
        EyeItemService.onDeath(living, event.getSource(), stack -> {
            ItemEntity drop = new ItemEntity(
                    living.level(), living.getX(), living.getY(), living.getZ(), stack);
            drop.setDefaultPickUpDelay();
            event.getDrops().add(drop);
        });
    }

    private static void onLivingHeal(LivingHealEvent event) {
        LivingEntity living = event.getEntity();
        if (!living.level().isClientSide()) {
            ServerBehaviorScheduler.onHealed(living);
        }
    }

    /** Respawning or leaving the End replaces the player. */
    private static void onPlayerClone(PlayerEvent.Clone event) {
        ServerServices.onPlayerCloned((ServerPlayer) event.getOriginal(), (ServerPlayer) event.getEntity(),
                !event.isWasDeath());
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerServices.onPlayerLeft((ServerPlayer) event.getEntity());
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        GooglyServerCommands.register(event.getDispatcher(), event.getBuildContext());
    }

    /** Lowest priority, like entity interaction, so protection mods that cancel block use get first refusal. */
    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getUseItem() == Event.Result.DENY) {
            return;
        }
        InteractionResult result = EyeItemService.useOnSkull(
                event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
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

    private static void onServerStopping(ServerStoppingEvent event) {
        ServerServices.onServerStopping(event.getServer());
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ServerBehaviorScheduler.serverTick();
    }

    private static void onStartTracking(PlayerEvent.StartTracking event) {
        ServerServices.onStartTracking(event.getTarget(), (ServerPlayer) event.getEntity());
    }

    private static void onStopTracking(PlayerEvent.StopTracking event) {
        ServerServices.onStopTracking(event.getTarget());
    }

    private static void onTrade(TradeWithVillagerEvent event) {
        ServerBehaviorScheduler.onTrade(event.getAbstractVillager());
    }

    /** Subscribe the server listeners to the game bus; called once from the mod constructor. */
    public static void register(IEventBus gameBus) {
        gameBus.addListener(ForgeServerEvents::onRegisterCommands);
        gameBus.addListener(EventPriority.LOWEST, ForgeServerEvents::onEntityInteract);
        gameBus.addListener(EventPriority.LOWEST, ForgeServerEvents::onEntityInteractSpecific);
        gameBus.addListener(EventPriority.LOWEST, ForgeServerEvents::onRightClickBlock);
        gameBus.addListener(EventPriority.LOWEST, ForgeServerEvents::onRightClickItem);
        // Late enough to see the heads other mods add to the drops, such as the Heads mod's.
        gameBus.addListener(EventPriority.LOWEST, ForgeServerEvents::onLivingDrops);
        gameBus.addListener(ForgeServerEvents::onEntityJoinLevel);
        gameBus.addListener(ForgeServerEvents::onPlayerLoggedOut);
        gameBus.addListener(ForgeServerEvents::onDatapackSync);
        gameBus.addListener(ForgeServerEvents::onServerStopping);
        gameBus.addListener(ForgeServerEvents::onServerTick);
        gameBus.addListener(ForgeServerEvents::onStartTracking);
        gameBus.addListener(ForgeServerEvents::onStopTracking);
        // Lowest priority so eyes react only to damage and healing no other mod cancelled.
        gameBus.addListener(EventPriority.LOWEST, ForgeServerEvents::onLivingDamage);
        gameBus.addListener(EventPriority.LOWEST, ForgeServerEvents::onLivingHeal);
        gameBus.addListener(ForgeServerEvents::onTrade);
        gameBus.addListener(ForgeServerEvents::onPlayerClone);
        gameBus.addListener(ForgeServerEvents::onLivingConversion);
    }
}
