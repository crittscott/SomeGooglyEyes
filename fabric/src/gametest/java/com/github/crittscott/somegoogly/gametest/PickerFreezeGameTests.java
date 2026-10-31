package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link PickerFreezeGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class PickerFreezeGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void freezeCapturesAndUnfreezeRestores(GameTestHelper helper) {
        PickerFreezeGameTestsLogic.freezeCapturesAndUnfreezeRestores(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void freezePreservesAlreadyForcedNoAi(GameTestHelper helper) {
        PickerFreezeGameTestsLogic.freezePreservesAlreadyForcedNoAi(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void freezeRefusesSecondEditor(GameTestHelper helper) {
        PickerFreezeGameTestsLogic.freezeRefusesSecondEditor(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void switchingMobsReleasesThePreviousOne(GameTestHelper helper) {
        PickerFreezeGameTestsLogic.switchingMobsReleasesThePreviousOne(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void staleMarkerIsRestoredOnJoin(GameTestHelper helper) {
        PickerFreezeGameTestsLogic.staleMarkerIsRestoredOnJoin(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void joinDuringLiveEditReassertsTheFreeze(GameTestHelper helper) {
        PickerFreezeGameTestsLogic.joinDuringLiveEditReassertsTheFreeze(helper);
    }
}
