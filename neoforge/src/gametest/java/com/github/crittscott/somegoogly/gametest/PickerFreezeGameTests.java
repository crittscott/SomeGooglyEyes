package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * NeoForge GameTest entry points for {@link PickerFreezeGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class PickerFreezeGameTests {

    private PickerFreezeGameTests() {
    }

    public static void freezeCapturesAndUnfreezeRestores(GameTestHelper helper) {
        PickerFreezeGameTestsLogic.freezeCapturesAndUnfreezeRestores(helper);
    }

    public static void freezePreservesAlreadyForcedNoAi(GameTestHelper helper) {
        PickerFreezeGameTestsLogic.freezePreservesAlreadyForcedNoAi(helper);
    }

    public static void freezeRefusesSecondEditor(GameTestHelper helper) {
        PickerFreezeGameTestsLogic.freezeRefusesSecondEditor(helper);
    }

    public static void switchingMobsReleasesThePreviousOne(GameTestHelper helper) {
        PickerFreezeGameTestsLogic.switchingMobsReleasesThePreviousOne(helper);
    }

    public static void staleMarkerIsRestoredOnJoin(GameTestHelper helper) {
        PickerFreezeGameTestsLogic.staleMarkerIsRestoredOnJoin(helper);
    }

    public static void joinDuringLiveEditReassertsTheFreeze(GameTestHelper helper) {
        PickerFreezeGameTestsLogic.joinDuringLiveEditReassertsTheFreeze(helper);
    }
}

