package com.github.crittscott.somegoogly.server.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.command.GooglyServerCommands;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.server.EyeItemService;
import com.github.crittscott.somegoogly.server.ServerServices;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.functions.CopyNbtFunction;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;

/** Fabric event wiring for the loader-neutral authoritative server services. */
public final class FabricServerEvents {

    /**
     * Late phase for entity interaction and self-shearing: protection callbacks in the default
     * phase can veto item use before either service mutates player or entity state.
     */
    private static final ResourceLocation LATE_PHASE =
            new ResourceLocation(SomeGooglyCommon.MOD_ID, "after_protection");

    /** The loot tables of the heads that can have eyes, gathered on first use, once blocks are registered. */
    @Nullable
    private static Set<ResourceLocation> headLootTables;

    private FabricServerEvents() {
    }

    private static Set<ResourceLocation> headLootTables() {
        if (headLootTables == null) {
            Set<ResourceLocation> tables = new HashSet<>();
            for (Block block : BuiltInRegistries.BLOCK) {
                if (SkullEyeState.entityFor(block) != null) {
                    tables.add(block.getLootTable());
                }
            }
            headLootTables = Set.copyOf(tables);
        }
        return headLootTables;
    }

    /** Subscribe the server listeners; called once from the mod initializer. */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                GooglyServerCommands.register(dispatcher, registryAccess));
        UseEntityCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, LATE_PHASE);
        UseEntityCallback.EVENT.register(LATE_PHASE, (player, level, hand, entity, hitResult) ->
                EyeItemService.interact(player, level, hand, entity));
        UseBlockCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, LATE_PHASE);
        UseBlockCallback.EVENT.register(LATE_PHASE, (player, level, hand, hitResult) ->
                EyeItemService.useOnSkull(player, level, hand, hitResult));
        // Breaking an eyed head copies its eye state onto the dropped head item, where placing it restores it.
        // The Forge counterpart is HeadEyesLootModifier. A head without eyes saves no compound, so nothing copies.
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            if (headLootTables().contains(id)) {
                tableBuilder.modifyPools(pool -> pool.apply(CopyNbtFunction.copyData(ContextNbtProvider.BLOCK_ENTITY)
                        .copy(SkullEyeState.DATA_KEY, BlockItem.BLOCK_ENTITY_TAG + "." + SkullEyeState.DATA_KEY)));
            }
        });
        UseItemCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, LATE_PHASE);
        UseItemCallback.EVENT.register(LATE_PHASE, (player, level, hand) -> {
            if (EyeItemService.selfRemoveWithShears(player, hand) == InteractionResult.PASS) {
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }
            return InteractionResultHolder.success(player.getItemInHand(hand));
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof LivingEntity living) {
                ServerServices.onLivingEntityLoaded(living);
            }
        });
        ServerLivingEntityEvents.MOB_CONVERSION.register((previous, converted, keepEquipment) ->
                EyeState.copy(previous, converted));
        // Leaving the End replaces the player; eyes applied to a player are still lost on death.
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (alive) {
                EyeState.copy(oldPlayer, newPlayer);
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
            ServerServices.onStartTracking(entity, player);
        });
        EntityTrackingEvents.STOP_TRACKING.register((entity, player) -> {
            if (entity instanceof LivingEntity living) {
                ServerBehaviorScheduler.onStopTracking(living);
            }
        });
    }
}
