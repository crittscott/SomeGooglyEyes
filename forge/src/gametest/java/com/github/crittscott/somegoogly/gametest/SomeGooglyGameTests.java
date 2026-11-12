package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.Priority;

/**
 * Forge GameTest entry points for the shared core and picker-spawn assertions.
 */
public final class SomeGooglyGameTests {

    private SomeGooglyGameTests() {
    }

    /**
     * A survival-mode server player for tests whose item and interaction paths run server-side. Forge's
     * {@code makeMockPlayer} is not a {@code ServerPlayer}, so those paths need the in-level mock instead.
     */
    @SuppressWarnings("removal")
    static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    public static void commandSpawnFinalizesBeforeApplyingPickerState(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.commandSpawnFinalizesBeforeApplyingPickerState(helper);
    }

    public static void spawnCommandsRefuseDragonPlayersAndExcludedTypes(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.spawnCommandsRefuseDragonPlayersAndExcludedTypes(helper);
    }

    /**
     * Exercises Forge's native entity persistent compound through an entity save/load cycle. In game: see
     * {@link SomeGooglyGameTestsLogic#entityPersistentDataSurvivesSaveLoad}.
     */
    public static void forgeEntityPersistentDataSurvivesSaveLoad(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.entityPersistentDataSurvivesSaveLoad(helper, "Forge");
    }

    public static void conversionKeepsEyeState(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.conversionKeepsEyeState(helper);
    }

    public static void googlyEyeItemStoresAppearanceOverride(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.googlyEyeItemStoresAppearanceOverride(helper);
    }

    public static void optometristAcceptsOnlyShears(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.optometristAcceptsOnlyShears(helper);
    }

    public static void deathHarvestRejectsNonqualifyingKills(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.deathHarvestRejectsNonqualifyingKills(
                helper, survivalPlayer(helper));
    }

    public static void realShearsKillsHarvestAnEye(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.realShearsKillsHarvestAnEye(helper, survivalPlayer(helper));
    }

    /** Forge's clone event; see the shared test for the End-exit and death steps. */
    public static void nativePlayerCopiesKeepEyesOnlyWhileAlive(GameTestHelper helper) {
        LoaderInteractionGameTestsLogic.playerCopiesKeepEyesOnlyWhileAlive(helper, survivalPlayer(helper),
                (original, replacement, alive) -> PlayerEvent.Clone.BUS.post(
                        new PlayerEvent.Clone(replacement, original, !alive)));
    }

    /**
     * Forge's interaction events, allowed and refused by a temporary highest-priority listener that cancels them as a
     * protection mod would; see the shared test for the in-game steps.
     */
    public static void nativeUseHooksHonorResults(GameTestHelper helper) {
        LoaderInteractionGameTestsLogic.useHooksHonorResults(helper, survivalPlayer(helper), (player, target, hit, refused) -> {
            if (hit != null) {
                return post(PlayerInteractEvent.RightClickBlock.BUS, new PlayerInteractEvent.RightClickBlock(
                        player, InteractionHand.MAIN_HAND, hit.getBlockPos(), hit), refused);
            }
            if (target != null) {
                // Forge has no plain EntityInteract event; every entity click posts EntityInteractSpecific.
                return post(PlayerInteractEvent.EntityInteractSpecific.BUS, new PlayerInteractEvent.EntityInteractSpecific(
                        player, InteractionHand.MAIN_HAND, target, Vec3.ZERO), refused);
            }
            return post(PlayerInteractEvent.RightClickItem.BUS,
                    new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND), refused);
        }, true);
    }

    /** Post {@code event} on {@code bus}, cancelled first by a temporary highest-priority listener when {@code refused}. */
    private static <T extends PlayerInteractEvent & Cancellable> InteractionResult post(CancellableEventBus<T> bus, T event,
                                                                         boolean refused) {
        EventListener refusal = refused ? bus.addListener(Priority.HIGHEST, posted -> posted == event) : null;
        try {
            return bus.post(event) ? event.getCancellationResult() : InteractionResult.PASS;
        } finally {
            if (refusal != null) {
                bus.removeListener(refusal);
            }
        }
    }
}
