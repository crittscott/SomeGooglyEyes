package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * NeoForge GameTest entry points for the shared core and picker-spawn assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SomeGooglyGameTests {

    private static final String TEMPLATE = "empty";

    private SomeGooglyGameTests() {
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

    /** Exercises NeoForge's native entity persistent compound through an entity save/load cycle. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void neoForgeEntityPersistentDataSurvivesSaveLoad(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.entityPersistentDataSurvivesSaveLoad(helper, "NeoForge");
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
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void deathHarvestRejectsNonqualifyingKills(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.deathHarvestRejectsNonqualifyingKills(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }
}
