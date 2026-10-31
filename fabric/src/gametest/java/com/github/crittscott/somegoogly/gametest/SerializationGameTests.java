package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link SerializationGameTestsLogic}; see that class for the
 * actual assertions.
 */
public final class SerializationGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void networkProtocolIdsAreVersionedAndUnique(GameTestHelper helper) {
        SerializationGameTestsLogic.networkProtocolIdsAreVersionedAndUnique(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void appearanceOverrideSparseNbtRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.appearanceOverrideSparseNbtRoundTrips(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void behaviorTriggerPacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.behaviorTriggerPacketRoundTrips(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void angerPacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.angerPacketRoundTrips(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void configSyncPacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.configSyncPacketRoundTrips(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void configSyncRejectsOversizedAndUnsafePayloads(GameTestHelper helper) {
        SerializationGameTestsLogic.configSyncRejectsOversizedAndUnsafePayloads(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void eyeColorRejectsWrongChannelCount(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeColorRejectsWrongChannelCount(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void eyeColorRejectsOutOfRangeChannels(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeColorRejectsOutOfRangeChannels(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void eyeDefinitionCodecRoundTripsEveryField(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeDefinitionCodecRoundTripsEveryField(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void eyeFieldsSerializeAtFloatPrecision(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeFieldsSerializeAtFloatPrecision(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void pickerExportPacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.pickerExportPacketRoundTrips(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void pickerFreezePacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.pickerFreezePacketRoundTrips(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void eyeStatePacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeStatePacketRoundTrips(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void eyeStatePacketRejectsNonFiniteValues(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeStatePacketRejectsNonFiniteValues(helper);
    }
}
