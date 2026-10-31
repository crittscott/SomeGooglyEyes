package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Forge GameTest entry points for {@link PickerExportGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class PickerExportGameTests {

    private PickerExportGameTests() {
    }

    public static void canonicalJsonWritesDefaultValuedFields(GameTestHelper helper) {
        PickerExportGameTestsLogic.canonicalJsonWritesDefaultValuedFields(helper);
    }

    public static void draftsRetainSpeciesAndAge(GameTestHelper helper) {
        PickerExportGameTestsLogic.draftsRetainSpeciesAndAge(helper);
    }

    public static void exportOverlayCombinesDraftAges(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportOverlayCombinesDraftAges(helper);
    }

    public static void exportOverlayPreservesOtherAges(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportOverlayPreservesOtherAges(helper);
    }

    public static void exportPacketRefusesOversizedConfig(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportPacketRefusesOversizedConfig(helper);
    }

    public static void exportRejectsConfigWithNoUsableEyes(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsConfigWithNoUsableEyes(helper);
    }

    public static void exportRejectsEmptyPayloadAsMalformed(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsEmptyPayloadAsMalformed(helper);
    }

    public static void exportRejectsEnderDragon(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsEnderDragon(helper);
    }
    public static void exportRejectsGarbageTypedField(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsGarbageTypedField(helper);
    }

    public static void exportRejectsUnknownEntityType(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsUnknownEntityType(helper);
    }

    public static void exportRejectsUnsafeNumericConfig(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsUnsafeNumericConfig(helper);
    }

}
