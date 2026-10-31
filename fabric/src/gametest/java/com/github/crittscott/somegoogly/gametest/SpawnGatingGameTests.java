package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link SpawnGatingGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class SpawnGatingGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void fullPercentGrantsEyesAndZeroDeniesThem(GameTestHelper helper) {
        SpawnGatingGameTestsLogic.fullPercentGrantsEyesAndZeroDeniesThem(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void spawnAlwaysAssignsAVariantRoll(GameTestHelper helper) {
        SpawnGatingGameTestsLogic.spawnAlwaysAssignsAVariantRoll(helper);
    }
}
