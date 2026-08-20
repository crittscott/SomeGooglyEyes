package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGoogly;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Forge GameTest entry points for {@link PickerExportGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGoogly.MOD_ID)
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
    public static void versionRangeSynthesis(GameTestHelper helper) {
        PickerExportGameTestsLogic.versionRangeSynthesis(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void canonicalJsonWritesDefaultValuedFields(GameTestHelper helper) {
        PickerExportGameTestsLogic.canonicalJsonWritesDefaultValuedFields(helper);
    }
}
