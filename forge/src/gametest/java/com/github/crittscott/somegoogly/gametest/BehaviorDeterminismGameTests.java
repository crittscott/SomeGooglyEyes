package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Forge GameTest entry points for {@link BehaviorDeterminismGameTestsLogic}; see that class for the
 * actual assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
public final class BehaviorDeterminismGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    private BehaviorDeterminismGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void blinkMaskIsSeedDeterministic(GameTestHelper helper) {
        BehaviorDeterminismGameTestsLogic.blinkMaskIsSeedDeterministic(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void everyBehaviorIsSeedDeterministicOverItsRun(GameTestHelper helper) {
        BehaviorDeterminismGameTestsLogic.everyBehaviorIsSeedDeterministicOverItsRun(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void fastForwardMatchesNaturalPlayback(GameTestHelper helper) {
        BehaviorDeterminismGameTestsLogic.fastForwardMatchesNaturalPlayback(helper);
    }
}
