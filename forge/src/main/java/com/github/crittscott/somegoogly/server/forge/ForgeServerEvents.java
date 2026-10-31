package com.github.crittscott.somegoogly.server.forge;

import com.github.crittscott.somegoogly.command.GooglyServerCommands;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.server.EyeItemService;
import com.github.crittscott.somegoogly.server.ServerServices;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.Result;
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
import net.minecraftforge.eventbus.api.listener.Priority;

/** Forge event wiring for the loader-neutral authoritative server services. */
public final class ForgeServerEvents {

    private ForgeServerEvents() {
    }

    /** Subscribe the server listeners to their game events' buses; called once from the mod constructor. */
    public static void register() {
        RegisterCommandsEvent.BUS.addListener(ForgeServerEvents::onRegisterCommands);
        PlayerInteractEvent.EntityInteract.BUS.addListener(Priority.LOWEST, ForgeServerEvents::onEntityInteract);
        PlayerInteractEvent.RightClickBlock.BUS.addListener(Priority.LOWEST, ForgeServerEvents::onRightClickBlock);
        PlayerInteractEvent.RightClickItem.BUS.addListener(Priority.LOWEST, ForgeServerEvents::onRightClickItem);
        // Late enough to see the heads other mods add to the drops.
        LivingDropsEvent.BUS.addListener(Priority.LOWEST, ForgeServerEvents::onLivingDrops);
        EntityJoinLevelEvent.BUS.addListener(ForgeServerEvents::onEntityJoinLevel);
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(ForgeServerEvents::onPlayerLoggedOut);
        OnDatapackSyncEvent.BUS.addListener(ForgeServerEvents::onDatapackSync);
        ServerStoppingEvent.BUS.addListener(ForgeServerEvents::onServerStopping);
        TickEvent.ServerTickEvent.Post.BUS.addListener(ForgeServerEvents::onServerTick);
        PlayerEvent.StartTracking.BUS.addListener(ForgeServerEvents::onStartTracking);
        PlayerEvent.StopTracking.BUS.addListener(ForgeServerEvents::onStopTracking);
        LivingDamageEvent.BUS.addListener(ForgeServerEvents::onLivingDamage);
        LivingHealEvent.BUS.addListener(ForgeServerEvents::onLivingHeal);
        TradeWithVillagerEvent.BUS.addListener(ForgeServerEvents::onTrade);
        PlayerEvent.Clone.BUS.addListener(ForgeServerEvents::onPlayerClone);
        LivingConversionEvent.Post.BUS.addListener(ForgeServerEvents::onLivingConversion);
    }

    /**
     * Record a handled interaction's result and report whether to cancel the event, so the vanilla interaction
     * does not run after ours.
     */
    private static boolean consumed(PlayerInteractEvent event, InteractionResult result) {
        if (result == InteractionResult.PASS) {
            return false;
        }
        event.setCancellationResult(result);
        return true;
    }

    /** Leaving the End replaces the player; eyes applied to a player are still lost on death. */
    private static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            EyeState.copy(event.getOriginal(), event.getEntity());
        }
    }

    private static void onLivingConversion(LivingConversionEvent.Post event) {
        EyeState.copy(event.getEntity(), event.getOutcome());
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        GooglyServerCommands.register(event.getDispatcher(), event.getBuildContext());
    }

    /**
     * Lowest priority so a land-claim or region-protection mod that cancels the entity interaction
     * gets first refusal — this handler cancels the event as soon as it acts.
     */
    private static boolean onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof LivingEntity living)) {
            return false;
        }
        InteractionResult result = EyeItemService.interact(
                event.getEntity(), event.getLevel(), event.getHand(), living);
        return consumed(event, result);
    }

    /** Lowest priority, like entity interaction, so protection mods that cancel block use get first refusal. */
    private static boolean onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getUseItem() == Result.DENY) {
            return false;
        }
        InteractionResult result = EyeItemService.useOnSkull(
                event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
        return consumed(event, result);
    }

    private static boolean onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        InteractionResult result = EyeItemService.selfRemoveWithShears(event.getEntity(), event.getHand());
        return consumed(event, result);
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

    private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            ServerServices.onLivingEntityLoaded(living);
        }
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerServices.onPlayerLeft((ServerPlayer) event.getEntity());
    }

    /** A joining player gets the current view; a reload broadcasts once, only if something changed. */
    private static void onDatapackSync(OnDatapackSyncEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player != null) {
            ServerServices.syncEyeConfigs(player);
        } else {
            ServerServices.broadcastEyeConfigsIfChanged(event.getPlayerList().getServer());
        }
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        ServerServices.onServerStopping(event.getServer());
    }

    private static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        ServerBehaviorScheduler.serverTick();
    }

    private static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof LivingEntity living) {
            ServerServices.onStartTracking(living, (ServerPlayer) event.getEntity());
        }
    }

    private static void onStopTracking(PlayerEvent.StopTracking event) {
        if (event.getTarget() instanceof LivingEntity living) {
            ServerBehaviorScheduler.onStopTracking(living);
        }
    }

    private static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().getEntity() instanceof Player) {
            ServerBehaviorScheduler.onPlayerHurt(event.getEntity());
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
