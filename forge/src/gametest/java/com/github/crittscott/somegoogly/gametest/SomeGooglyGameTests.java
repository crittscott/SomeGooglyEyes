package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Forge GameTest entry points for the shared core and picker-spawn assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SomeGooglyGameTests {

    private static final String TEMPLATE = "empty";

    private SomeGooglyGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void commandSpawnFinalizesBeforeApplyingPickerState(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.commandSpawnFinalizesBeforeApplyingPickerState(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void conversionKeepsEyeState(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.conversionKeepsEyeState(helper, survivalPlayer(helper));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void deathHarvestRejectsNonqualifyingKills(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.deathHarvestRejectsNonqualifyingKills(
                helper, survivalPlayer(helper));
    }

    /**
     * Exercises Forge's native entity persistent compound through an entity save/load cycle. In game: see
     * {@link SomeGooglyGameTestsLogic#entityPersistentDataSurvivesSaveLoad}.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void forgeEntityPersistentDataSurvivesSaveLoad(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.entityPersistentDataSurvivesSaveLoad(helper, "Forge");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void googlyEyeItemStoresAppearanceOverride(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.googlyEyeItemStoresAppearanceOverride(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void lightningConversionKeepsEyeState(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.lightningConversionKeepsEyeState(helper);
    }

    /** See the shared test for End exit and death reproduction steps. */
    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void nativePlayerCopiesKeepEyesOnlyWhileAlive(GameTestHelper helper) {
        LoaderInteractionGameTestsLogic.playerCopiesKeepEyesOnlyWhileAlive(helper, survivalPlayer(helper), (original, replacement, alive) -> MinecraftForge.EVENT_BUS.post(new PlayerEvent.Clone(replacement, original, !alive)));
    }
    /** See the shared test for normal use and protection refusal reproduction steps. */
    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void nativeUseHooksHonorResults(GameTestHelper helper) {
        LoaderInteractionGameTestsLogic.useHooksHonorResults(helper, survivalPlayer(helper), (player, target, hit, refused) -> {
                    Consumer<PlayerInteractEvent> refusal = event -> {
                        if (event.getEntity() == player) event.setCanceled(true);
                    };
                    if (refused) MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, PlayerInteractEvent.class, refusal);
                    try {
                        PlayerInteractEvent event = hit != null
                                ? new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, hit.getBlockPos(), hit)
                                : target == null ? new PlayerInteractEvent.RightClickItem(player, InteractionHand.MAIN_HAND)
                                : target instanceof ArmorStand
                                        ? new PlayerInteractEvent.EntityInteractSpecific(player, InteractionHand.MAIN_HAND, target, Vec3.ZERO)
                                        : new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, target);
                        MinecraftForge.EVENT_BUS.post(event);
                        return event.isCanceled() ? event.getCancellationResult() : InteractionResult.PASS;
                    } finally {
                        if (refused) MinecraftForge.EVENT_BUS.unregister(refusal);
                    }
                }, true);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void optometristAcceptsOnlyShears(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.optometristAcceptsOnlyShears(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void realShearsKillsHarvestAnEye(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.realShearsKillsHarvestAnEye(helper, survivalPlayer(helper));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void spawnCommandsRefuseDragonPlayersAndExcludedTypes(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.spawnCommandsRefuseDragonPlayersAndExcludedTypes(helper);
    }

    /**
     * A survival-mode server player for tests whose item and interaction paths run server-side: Forge's fake
     * player. {@code makeMockPlayer} is not a {@code ServerPlayer}, and {@code makeMockServerPlayerInLevel}
     * always reports creative mode.
     */
    static ServerPlayer survivalPlayer(GameTestHelper helper) {
        return FakePlayerFactory.getMinecraft(helper.getLevel());
    }
}
