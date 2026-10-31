package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * NeoForge GameTest entry points for {@link BehaviorDeterminismGameTestsLogic}; see that class for the
 * actual assertions.
 */
public final class BehaviorDeterminismGameTests {

    private BehaviorDeterminismGameTests() {
    }

    public static void everyBehaviorIsSeedDeterministicOverItsRun(GameTestHelper helper) {
        BehaviorDeterminismGameTestsLogic.everyBehaviorIsSeedDeterministicOverItsRun(helper);
    }
}

