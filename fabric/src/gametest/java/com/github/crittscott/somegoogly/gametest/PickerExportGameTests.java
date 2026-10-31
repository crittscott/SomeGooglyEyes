package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link PickerExportGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class PickerExportGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void canonicalJsonWritesDefaultValuedFields(GameTestHelper helper) {
        PickerExportGameTestsLogic.canonicalJsonWritesDefaultValuedFields(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void draftsRetainSpeciesAndAge(GameTestHelper helper) {
        PickerExportGameTestsLogic.draftsRetainSpeciesAndAge(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void exportOverlayCombinesDraftAges(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportOverlayCombinesDraftAges(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void exportOverlayPreservesOtherAges(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportOverlayPreservesOtherAges(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void exportPacketRefusesOversizedConfig(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportPacketRefusesOversizedConfig(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void exportRejectsConfigWithNoUsableEyes(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsConfigWithNoUsableEyes(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void exportRejectsEmptyPayloadAsMalformed(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsEmptyPayloadAsMalformed(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void exportRejectsEnderDragon(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsEnderDragon(helper);
    }
    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void exportRejectsGarbageTypedField(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsGarbageTypedField(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void exportRejectsUnknownEntityType(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsUnknownEntityType(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void exportRejectsUnsafeNumericConfig(GameTestHelper helper) {
        PickerExportGameTestsLogic.exportRejectsUnsafeNumericConfig(helper);
    }

}
