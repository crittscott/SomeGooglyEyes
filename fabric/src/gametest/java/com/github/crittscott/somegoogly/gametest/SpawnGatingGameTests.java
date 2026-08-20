package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link SpawnGatingGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class SpawnGatingGameTests implements FabricGameTest {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fullPercentGrantsEyesAndZeroDeniesThem(GameTestHelper helper) {
        SpawnGatingGameTestsLogic.fullPercentGrantsEyesAndZeroDeniesThem(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void spawnAlwaysAssignsAVariantRoll(GameTestHelper helper) {
        SpawnGatingGameTestsLogic.spawnAlwaysAssignsAVariantRoll(helper);
    }
}
