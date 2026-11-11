package com.github.crittscott.somegoogly.server.neoforge;

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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
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

    private static void onLivingDamage(LivingDamageEvent.Post event) {
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
        if (event.getUseItem() == TriState.FALSE) {
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

    private static void onServerTick(ServerTickEvent.Post event) {
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
        gameBus.addListener(NeoForgeServerEvents::onRegisterCommands);
        gameBus.addListener(EventPriority.LOWEST,
                (PlayerInteractEvent.EntityInteract event) -> onEntityInteract(event));
        gameBus.addListener(EventPriority.LOWEST, NeoForgeServerEvents::onEntityInteractSpecific);
        gameBus.addListener(EventPriority.LOWEST, NeoForgeServerEvents::onRightClickBlock);
        gameBus.addListener(EventPriority.LOWEST, NeoForgeServerEvents::onRightClickItem);
        // Late enough to see the heads other mods add to the drops.
        gameBus.addListener(EventPriority.LOWEST, NeoForgeServerEvents::onLivingDrops);
        gameBus.addListener(NeoForgeServerEvents::onEntityJoinLevel);
        gameBus.addListener(NeoForgeServerEvents::onPlayerLoggedOut);
        gameBus.addListener(NeoForgeServerEvents::onDatapackSync);
        gameBus.addListener(NeoForgeServerEvents::onServerStopping);
        gameBus.addListener(NeoForgeServerEvents::onServerTick);
        gameBus.addListener(NeoForgeServerEvents::onStartTracking);
        gameBus.addListener(NeoForgeServerEvents::onStopTracking);
        gameBus.addListener(NeoForgeServerEvents::onLivingDamage);
        // Lowest priority so eyes react only to healing no other mod cancelled; the damage event above is past that.
        gameBus.addListener(EventPriority.LOWEST, NeoForgeServerEvents::onLivingHeal);
        gameBus.addListener(NeoForgeServerEvents::onTrade);
        gameBus.addListener(NeoForgeServerEvents::onPlayerClone);
        gameBus.addListener(NeoForgeServerEvents::onLivingConversion);
    }
}
