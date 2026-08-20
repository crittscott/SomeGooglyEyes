package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.forge.SomeGoogly;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Forge GameTest entry points for {@link SpawnGatingGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGoogly.MOD_ID)
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
