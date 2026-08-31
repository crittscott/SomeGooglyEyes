package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Forge GameTest entry points for {@link PickerFreezeGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
public final class PickerFreezeGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    private PickerFreezeGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void freezeCapturesAndUnfreezeRestores(GameTestHelper helper) {
        PickerFreezeGameTestsLogic.freezeCapturesAndUnfreezeRestores(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void freezePreservesAlreadyForcedNoAi(GameTestHelper helper) {
        PickerFreezeGameTestsLogic.freezePreservesAlreadyForcedNoAi(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void freezeRefusesSecondEditor(GameTestHelper helper) {
        PickerFreezeGameTestsLogic.freezeRefusesSecondEditor(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void switchingMobsReleasesThePreviousOne(GameTestHelper helper) {
        PickerFreezeGameTestsLogic.switchingMobsReleasesThePreviousOne(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void staleMarkerIsRestoredOnJoin(GameTestHelper helper) {
        PickerFreezeGameTestsLogic.staleMarkerIsRestoredOnJoin(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void joinDuringLiveEditReassertsTheFreeze(GameTestHelper helper) {
        PickerFreezeGameTestsLogic.joinDuringLiveEditReassertsTheFreeze(helper);
    }
}
