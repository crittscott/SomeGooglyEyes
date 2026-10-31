package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Forge GameTest entry points for {@link SerializationGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class SerializationGameTests {

    private SerializationGameTests() {
    }

    public static void networkProtocolIdsAreVersionedAndUnique(GameTestHelper helper) {
        SerializationGameTestsLogic.networkProtocolIdsAreVersionedAndUnique(helper);
    }

    public static void appearanceOverrideSparseNbtRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.appearanceOverrideSparseNbtRoundTrips(helper);
    }

    public static void behaviorTriggerPacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.behaviorTriggerPacketRoundTrips(helper);
    }

    public static void angerPacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.angerPacketRoundTrips(helper);
    }

    public static void configSyncPacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.configSyncPacketRoundTrips(helper);
    }

    public static void configSyncRejectsOversizedAndUnsafePayloads(GameTestHelper helper) {
        SerializationGameTestsLogic.configSyncRejectsOversizedAndUnsafePayloads(helper);
    }

    public static void eyeColorRejectsWrongChannelCount(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeColorRejectsWrongChannelCount(helper);
    }

    public static void eyeColorRejectsOutOfRangeChannels(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeColorRejectsOutOfRangeChannels(helper);
    }

    public static void eyeDefinitionCodecRoundTripsEveryField(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeDefinitionCodecRoundTripsEveryField(helper);
    }

    public static void eyeFieldsSerializeAtFloatPrecision(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeFieldsSerializeAtFloatPrecision(helper);
    }

    public static void pickerExportPacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.pickerExportPacketRoundTrips(helper);
    }

    public static void pickerFreezePacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.pickerFreezePacketRoundTrips(helper);
    }

    public static void eyeStatePacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeStatePacketRoundTrips(helper);
    }

    public static void eyeStatePacketRejectsNonFiniteValues(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeStatePacketRejectsNonFiniteValues(helper);
    }
}
