package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.forge.SomeGoogly;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Forge GameTest entry points for {@link BehaviorDeterminismGameTestsLogic}; see that class for the
 * actual assertions.
 */
@GameTestHolder(SomeGoogly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BehaviorDeterminismGameTests {

    private static final String TEMPLATE = "empty";

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
