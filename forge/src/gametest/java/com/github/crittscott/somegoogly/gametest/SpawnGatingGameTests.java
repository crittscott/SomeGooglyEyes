package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Forge GameTest entry points for {@link SpawnGatingGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class SpawnGatingGameTests {

    private SpawnGatingGameTests() {
    }

    public static void fullPercentGrantsEyesAndZeroDeniesThem(GameTestHelper helper) {
        SpawnGatingGameTestsLogic.fullPercentGrantsEyesAndZeroDeniesThem(helper);
    }

    public static void spawnAlwaysAssignsAVariantRoll(GameTestHelper helper) {
        SpawnGatingGameTestsLogic.spawnAlwaysAssignsAVariantRoll(helper);
    }
}
