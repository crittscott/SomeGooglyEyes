package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.forge.SomeGoogly;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Forge GameTest entry points for {@link PickerFreezeGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGoogly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PickerFreezeGameTests {

    private static final String TEMPLATE = "empty";

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
