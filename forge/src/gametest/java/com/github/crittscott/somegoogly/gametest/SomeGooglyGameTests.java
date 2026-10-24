package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
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

    /**
     * A survival-mode server player for tests whose item and interaction paths run server-side: Forge's fake
     * player. {@code makeMockPlayer} is not a {@code ServerPlayer}, and {@code makeMockServerPlayerInLevel}
     * always reports creative mode.
     */
    static ServerPlayer survivalPlayer(GameTestHelper helper) {
        return FakePlayerFactory.getMinecraft(helper.getLevel());
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void commandSpawnFinalizesBeforeApplyingPickerState(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.commandSpawnFinalizesBeforeApplyingPickerState(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void spawnCommandsRefuseDragonPlayersAndExcludedTypes(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.spawnCommandsRefuseDragonPlayersAndExcludedTypes(helper);
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
    public static void conversionKeepsEyeState(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.conversionKeepsEyeState(helper, survivalPlayer(helper));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void googlyEyeItemStoresAppearanceOverride(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.googlyEyeItemStoresAppearanceOverride(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void optometristAcceptsOnlyShears(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.optometristAcceptsOnlyShears(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void deathHarvestUsesTheSuppliedDropSink(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.deathHarvestUsesTheSuppliedDropSink(
                helper, survivalPlayer(helper));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void deathHarvestRejectsNonqualifyingKills(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.deathHarvestRejectsNonqualifyingKills(
                helper, survivalPlayer(helper));
    }
}
