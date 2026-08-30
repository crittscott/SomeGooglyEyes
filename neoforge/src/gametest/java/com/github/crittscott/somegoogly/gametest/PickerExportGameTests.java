package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * NeoForge GameTest entry points for {@link PickerExportGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PickerExportGameTests {

    private static final String TEMPLATE = "empty";

    private PickerExportGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void exportRejectsUnknownEntityType(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsUnknownEntityType(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void exportRejectsEnderDragon(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsEnderDragon(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void exportRejectsMissingPayload(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsMissingPayload(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void exportRejectsGarbageTypedField(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsGarbageTypedField(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void exportRejectsEmptyPayloadAsMalformed(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsEmptyPayloadAsMalformed(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void exportRejectsConfigWithNoUsableEyes(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsConfigWithNoUsableEyes(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void exportRejectsUnsafeNumericConfig(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsUnsafeNumericConfig(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void optionalModVersionRangeSynthesis(GameTestHelper helper) {
        PickerExportGameTestsLogic.optionalModVersionRangeSynthesis(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void canonicalJsonWritesDefaultValuedFields(GameTestHelper helper) {
        PickerExportGameTestsLogic.canonicalJsonWritesDefaultValuedFields(helper);
    }
}

