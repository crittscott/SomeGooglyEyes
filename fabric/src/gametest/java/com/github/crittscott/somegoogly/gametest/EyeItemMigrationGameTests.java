package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Fabric GameTest entry points for {@link EyeItemMigrationGameTestsLogic}. */
public final class EyeItemMigrationGameTests implements FabricGameTest {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void fullAppearanceMigratesForBothItems(GameTestHelper helper) {
        EyeItemMigrationGameTestsLogic.fullAppearanceMigratesForBothItems(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void sparseAndGlowStatesMigrate(GameTestHelper helper) {
        EyeItemMigrationGameTestsLogic.sparseAndGlowStatesMigrate(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void cleanupPreservesUnrelatedStackData(GameTestHelper helper) {
        EyeItemMigrationGameTestsLogic.cleanupPreservesUnrelatedStackData(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void currentDataWinsAndMalformedLegacyDataSurvives(GameTestHelper helper) {
        EyeItemMigrationGameTestsLogic.currentDataWinsAndMalformedLegacyDataSurvives(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void conversionIsIdempotentAndPersists(GameTestHelper helper) {
        EyeItemMigrationGameTestsLogic.conversionIsIdempotentAndPersists(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void migratedEyesSurviveCrafting(GameTestHelper helper) {
        EyeItemMigrationGameTestsLogic.migratedEyesSurviveCrafting(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void migratedSlimyEyeAppliesToMob(GameTestHelper helper) {
        EyeItemMigrationGameTestsLogic.migratedSlimyEyeAppliesToMob(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void chestContentsMigrateOnLoad(GameTestHelper helper) {
        EyeItemMigrationGameTestsLogic.chestContentsMigrateOnLoad(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void nestedContainerContentsMigrateOnLoad(GameTestHelper helper) {
        EyeItemMigrationGameTestsLogic.nestedContainerContentsMigrateOnLoad(helper);
    }
}
