package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Forge GameTest entry points for the shared core and picker-spawn assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
public final class SomeGooglyGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

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

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void configuredCowHasServerGeometry(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.configuredCowHasServerGeometry(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void spawnInitializesEyePersistentData(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.spawnInitializesEyePersistentData(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void commandSpawnFinalizesBeforeApplyingPickerState(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.commandSpawnFinalizesBeforeApplyingPickerState(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void spawnCommandsRefuseDragonPlayersAndExcludedTypes(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.spawnCommandsRefuseDragonPlayersAndExcludedTypes(helper);
    }

    /** Exercises Forge's native entity persistent compound through an entity save/load cycle. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void forgeEntityPersistentDataSurvivesSaveLoad(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.entityPersistentDataSurvivesSaveLoad(helper, "Forge");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void conversionKeepsEyeState(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.conversionKeepsEyeState(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void eyeStateAppearanceOverridesRoundTrip(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.eyeStateAppearanceOverridesRoundTrip(helper);
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
