package com.github.crittscott.somegoogly.server.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.command.GooglyServerCommands;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.item.EyeItemService;
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
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;

/** Fabric event wiring for the loader-neutral authoritative server services. */
public final class FabricServerEvents {

    /**
     * Late phase for eye-item entity interactions and self-shearing. Default-phase protection
     * callbacks can veto before either action mutates state.
     */
    private static final ResourceLocation LATE_PHASE =
            SomeGooglyCommon.id("after_protection");

    /** The loot tables of the heads that can have eyes, gathered on first use, once blocks are registered. */
    @Nullable
    private static Set<ResourceKey<LootTable>> headLootTables;

    private FabricServerEvents() {
    }

    private static Set<ResourceKey<LootTable>> headLootTables() {
        if (headLootTables == null) {
            Set<ResourceKey<LootTable>> tables = new HashSet<>();
            for (Block block : BuiltInRegistries.BLOCK) {
                if (SkullEyeState.entityFor(block) != null) {
                    block.getLootTable().ifPresent(tables::add);
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
        // The Forge and NeoForge counterpart is HeadEyesLootModifier.
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (headLootTables().contains(key)) {
                tableBuilder.modifyPools(pool -> pool.apply(SkullEyeState.headLootCopy()));
            }
        });
        UseItemCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, LATE_PHASE);
        UseItemCallback.EVENT.register(LATE_PHASE, (player, level, hand) ->
                EyeItemService.selfRemoveWithShears(player, hand));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof LivingEntity living) {
                ServerServices.onLivingEntityLoaded(living);
            }
        });
        ServerLivingEntityEvents.MOB_CONVERSION.register((previous, converted, context) ->
                ServerServices.onConverted(previous, converted));
        // Respawning or leaving the End replaces the player.
        ServerPlayerEvents.COPY_FROM.register(ServerServices::onPlayerCloned);
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) ->
                ServerBehaviorScheduler.onHurt(entity, source));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ServerServices.onPlayerLeft(handler.player));
        // Sent with the rest of the joining player's game data, ahead of their own eye state.
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            if (joined) {
                ServerServices.syncJoiningPlayer(player);
            }
        });
        // Once per reload, after the eye definitions and the re-read server config are both applied.
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) ->
                ServerServices.broadcastEyeSyncIfChanged(server));
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerServices::onServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(server -> ServerBehaviorScheduler.serverTick());
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            ServerServices.onStartTracking(entity, player);
        });
        EntityTrackingEvents.STOP_TRACKING.register((entity, player) -> ServerServices.onStopTracking(entity));
    }
}
