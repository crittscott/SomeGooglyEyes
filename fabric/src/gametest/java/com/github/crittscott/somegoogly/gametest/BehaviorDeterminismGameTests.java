package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link BehaviorDeterminismGameTestsLogic}; see that class for
 * the actual assertions.
 */
public final class BehaviorDeterminismGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 60)
    public void everyBehaviorIsSeedDeterministicOverItsRun(GameTestHelper helper) {
        BehaviorDeterminismGameTestsLogic.everyBehaviorIsSeedDeterministicOverItsRun(helper);
    }
}
