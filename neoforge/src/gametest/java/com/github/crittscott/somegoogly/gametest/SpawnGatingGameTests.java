package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * NeoForge GameTest entry points for {@link SpawnGatingGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpawnGatingGameTests {

    private static final String TEMPLATE = "empty";

    private SpawnGatingGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fullPercentGrantsEyesAndZeroDeniesThem(GameTestHelper helper) {
        SpawnGatingGameTestsLogic.fullPercentGrantsEyesAndZeroDeniesThem(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void spawnAlwaysAssignsAVariantRoll(GameTestHelper helper) {
        SpawnGatingGameTestsLogic.spawnAlwaysAssignsAVariantRoll(helper);
    }
}

