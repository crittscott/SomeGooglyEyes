package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.function.Consumer;

/**
 * NeoForge GameTest entry points for the shared core and picker-spawn assertions.
 */
public final class SomeGooglyGameTests {

    private SomeGooglyGameTests() {
    }

    public static void commandSpawnFinalizesBeforeApplyingPickerState(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.commandSpawnFinalizesBeforeApplyingPickerState(helper);
    }

    public static void spawnCommandsRefuseDragonPlayersAndExcludedTypes(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.spawnCommandsRefuseDragonPlayersAndExcludedTypes(helper);
    }

    /**
     * Exercises NeoForge's native entity persistent compound through an entity save/load cycle. In game: see
     * {@link SomeGooglyGameTestsLogic#entityPersistentDataSurvivesSaveLoad}.
     */
    public static void neoForgeEntityPersistentDataSurvivesSaveLoad(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.entityPersistentDataSurvivesSaveLoad(helper, "NeoForge");
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
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void realShearsKillsHarvestAnEye(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.realShearsKillsHarvestAnEye(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    /** NeoForge's clone event; see the shared test for the End-exit and death steps. */
    public static void nativePlayerCopiesKeepEyesOnlyWhileAlive(GameTestHelper helper) {
        LoaderInteractionGameTestsLogic.playerCopiesKeepEyesOnlyWhileAlive(helper,
                FakePlayerFactory.getMinecraft(helper.getLevel()),
                (original, replacement, alive) -> NeoForge.EVENT_BUS.post(
                        new PlayerEvent.Clone(replacement, original, !alive)));
    }

    /**
     * NeoForge's interaction events, allowed and refused by a temporary highest-priority listener that cancels them
     * as a protection mod would; see the shared test for the in-game steps.
     */
    public static void nativeUseHooksHonorResults(GameTestHelper helper) {
        LoaderInteractionGameTestsLogic.useHooksHonorResults(helper, FakePlayerFactory.getMinecraft(helper.getLevel()),
                (player, target, hit, refused) -> {
                    if (hit != null) {
                        var event = new PlayerInteractEvent.RightClickBlock(
                                player, InteractionHand.MAIN_HAND, hit.getBlockPos(), hit);
                        post(PlayerInteractEvent.RightClickBlock.class, event, refused, e -> e.setCanceled(true));
                        return event.isCanceled() ? event.getCancellationResult() : InteractionResult.PASS;
                    }
                    if (target instanceof ArmorStand) {
                        var event = new PlayerInteractEvent.EntityInteractSpecific(
                                player, InteractionHand.MAIN_HAND, target, Vec3.ZERO);
                        post(PlayerInteractEvent.EntityInteractSpecific.class, event, refused, e -> e.setCanceled(true));
                        return event.isCanceled() ? event.getCancellationResult() : InteractionResult.PASS;
                    }
                    if (target != null) {
                        var event = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, target);
                        post(PlayerInteractEvent.EntityInteract.class, event, refused, e -> e.setCanceled(true));
                        return event.isCanceled() ? event.getCancellationResult() : InteractionResult.PASS;
                    }
                    var event = new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND);
                    post(PlayerInteractEvent.RightClickItem.class, event, refused, e -> e.setCanceled(true));
                    return event.isCanceled() ? event.getCancellationResult() : InteractionResult.PASS;
                }, true);
    }

    /** Post {@code event}, refused first by a temporary highest-priority listener when {@code refused}. */
    private static <T extends PlayerInteractEvent> void post(Class<T> type, T event, boolean refused,
                                                            Consumer<T> refusal) {
        Consumer<T> listener = posted -> {
            if (posted == event) {
                refusal.accept(posted);
            }
        };
        if (refused) {
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, type, listener);
        }
        try {
            NeoForge.EVENT_BUS.post(event);
        } finally {
            if (refused) {
                NeoForge.EVENT_BUS.unregister(listener);
            }
        }
    }
}
