package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraftforge.gametest.GameTestHolder;

/** Forge GameTest entry points for {@link EyeItemMigrationGameTestsLogic}. */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
public final class EyeItemMigrationGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    private EyeItemMigrationGameTests() {
    }

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
                helper, helper.makeMockPlayer(GameType.SURVIVAL));
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
