package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link BehaviorDeterminismGameTestsLogic}; see that class for
 * the actual assertions.
 */
public final class BehaviorDeterminismGameTests implements FabricGameTest {

    private static final String TEMPLATE = "somegoogly:empty";

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
