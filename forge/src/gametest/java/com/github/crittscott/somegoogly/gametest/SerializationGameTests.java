package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.forge.SomeGoogly;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Forge GameTest entry points for {@link SerializationGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGoogly.MOD_ID)
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
    public static void pickerMobPosePacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.pickerMobPosePacketRoundTrips(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void pickerSpawnPacketsRoundTrip(GameTestHelper helper) {
        SerializationGameTestsLogic.pickerSpawnPacketsRoundTrip(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void eyeStatePacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeStatePacketRoundTrips(helper);
    }
}
