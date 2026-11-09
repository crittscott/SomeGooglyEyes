package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.Payload;
import com.github.crittscott.somegoogly.network.fabric.FabricNetworkTransport;
import com.github.crittscott.somegoogly.network.fabric.FabricNetworkTransport.FabricPayload;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Fabric GameTest entry points for {@link SerializationGameTestsLogic}; see that class for the
 * actual assertions.
 */
public final class SerializationGameTests implements FabricGameTest {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void networkProtocolIdsAreUnique(GameTestHelper helper) {
        for (Payload.Clientbound<?> payload : Payload.CLIENTBOUND) {
            helper.assertTrue(FabricNetworkTransport.type(payload.id()).getId().equals(payload.id()),
                    "each clientbound Fabric type must use its shared payload id");
        }
        for (Payload.Serverbound<?> payload : Payload.SERVERBOUND) {
            helper.assertTrue(FabricNetworkTransport.type(payload.id()).getId().equals(payload.id()),
                    "each serverbound Fabric type must use its shared payload id");
        }
        SerializationGameTestsLogic.networkProtocolIdsAreUnique(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void appearanceOverrideSparseNbtRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.appearanceOverrideSparseNbtRoundTrips(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void behaviorTriggerPacketRoundTrips(GameTestHelper helper) {
        for (EyeBehavior behavior : EyeBehavior.values()) {
            EyeBehaviorTriggerPacket body = new EyeBehaviorTriggerPacket(7, behavior, 12345L, 3);
            FabricPayload packet = new FabricPayload(body);
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                packet.write(buffer);
                FabricPayload decoded = packet.getType().read(buffer);
                helper.assertTrue(decoded.body().equals(body) && decoded.getType() == packet.getType(),
                        "Fabric's typed adapter must preserve the shared body and registered type");
            } finally {
                buffer.release();
            }
        }
        SerializationGameTestsLogic.behaviorTriggerPacketRoundTrips(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void pickerNumbersIgnoreTheComputersLanguage(GameTestHelper helper) {
        SerializationGameTestsLogic.pickerNumbersIgnoreTheComputersLanguage(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void angerPacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.angerPacketRoundTrips(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void settingsPacketRoundTrips(GameTestHelper helper) {
        SerializationGameTestsLogic.settingsPacketRoundTrips(helper);
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
    public static void eyeColorRejectsOutOfRangeChannels(GameTestHelper helper) {
        SerializationGameTestsLogic.eyeColorRejectsOutOfRangeChannels(helper);
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
