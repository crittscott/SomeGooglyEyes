package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link ConfigGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class ConfigGameTests implements FabricGameTest {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void percentForResolvesExactBeforeWildcard(GameTestHelper helper) {
        ConfigGameTestsLogic.percentForResolvesExactBeforeWildcard(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void spawnAllDefaultsOff(GameTestHelper helper) {
        ConfigGameTestsLogic.spawnAllDefaultsOff(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void shippedConfigsLoadForKnownEntities(GameTestHelper helper) {
        ConfigGameTestsLogic.shippedConfigsLoadForKnownEntities(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void everyShippedConfigHasNonBlankAttachTokens(GameTestHelper helper) {
        ConfigGameTestsLogic.everyShippedConfigHasNonBlankAttachTokens(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void staleConfigFallsBackToNewestGeneration(GameTestHelper helper) {
        ConfigGameTestsLogic.staleConfigFallsBackToNewestGeneration(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void shippedPigHasOneVariant(GameTestHelper helper) {
        ConfigGameTestsLogic.shippedPigHasOneVariant(helper);
    }
}
