package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * NeoForge GameTest entry points for {@link SerializationGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SerializationGameTests {

    private static final String TEMPLATE = "empty";

    private SerializationGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void networkProtocolIdsAreVersionedAndUnique(GameTestHelper helper) {
        SerializationGameTestsLogic.networkProtocolIdsAreVersionedAndUnique(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void appearanceOverrideSparseNbtRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.appearanceOverrideSparseNbtRoundTrips(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void behaviorTriggerPacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.behaviorTriggerPacketRoundTrips(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void configSyncPacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.configSyncPacketRoundTrips(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void configSyncRejectsOversizedAndUnsafePayloads(GameTestHelper helper) {
        SerializationGameTestsLogic.configSyncRejectsOversizedAndUnsafePayloads(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void eyeColorRejectsWrongChannelCount(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeColorRejectsWrongChannelCount(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void eyeDefinitionCodecRoundTripsEveryField(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeDefinitionCodecRoundTripsEveryField(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void eyeFieldsSerializeAtFloatPrecision(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeFieldsSerializeAtFloatPrecision(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void pickerExportPacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.pickerExportPacketRoundTrips(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void pickerFreezePacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.pickerFreezePacketRoundTrips(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void eyeStatePacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeStatePacketRoundTrips(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void eyeStatePacketRejectsNonFiniteValues(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeStatePacketRejectsNonFiniteValues(helper);
    }
}
