package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.eye.HeadInfo.HeadConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfigSet;
import com.github.crittscott.somegoogly.eye.HeadInfo.Variant;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import com.github.crittscott.somegoogly.network.PickerMobPosePacket;
import com.github.crittscott.somegoogly.network.PickerSpawnAllPacket;
import com.github.crittscott.somegoogly.network.PickerSpawnPacket;
import com.google.gson.JsonArray;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Serialization contracts: the flat-JSON eye codecs round-trip by value (the records implement
 * {@code equals}), and the three server→client packets survive a wire round-trip. Packets carry no
 * getters, so they're checked by <b>byte idempotence</b>: {@code encode} then {@code decode} then
 * {@code encode} must reproduce the original bytes. World-less.
 */
@GameTestHolder(SomeGoogly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SerializationGameTests {

    private static final String TEMPLATE = "empty";

    private SerializationGameTests() {
    }

    private static byte[] bytes(Consumer<FriendlyByteBuf> encode) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        encode.accept(buffer);
        byte[] out = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), out);
        return out;
    }

    private static RuntimeConfigSet sampleConfigSet() {
        HeadConfig head = new HeadConfig();
        head.attachPoint = "head";
        head.eyes = List.of(EyeDefinition.DEFAULT);
        Variant variant = new Variant();
        variant.weight = 1.0;
        variant.heads = List.of(head);
        RuntimeConfig config = new RuntimeConfig();
        config.enabled = true;
        config.variants = List.of(variant);
        RuntimeConfigSet set = new RuntimeConfigSet();
        set.any = config;
        return set;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void appearanceOverrideSparseNbtRoundTrips(GameTestHelper helper) {
        helper.assertTrue(AppearanceOverride.fromNbt(null).equals(AppearanceOverride.EMPTY),
                "null tag → EMPTY");
        helper.assertTrue(AppearanceOverride.fromNbt(AppearanceOverride.EMPTY.toNbt()).equals(AppearanceOverride.EMPTY),
                "EMPTY round-trips");

        AppearanceOverride irisOnly = AppearanceOverride.EMPTY.withIrisColor(new EyeColor(0.1F, 0.2F, 0.3F));
        helper.assertTrue(AppearanceOverride.fromNbt(irisOnly.toNbt()).equals(irisOnly), "iris-only round-trips");

        AppearanceOverride glowOnly = AppearanceOverride.EMPTY.withGlow(true);
        helper.assertTrue(AppearanceOverride.fromNbt(glowOnly.toNbt()).equals(glowOnly), "glow-only round-trips");

        AppearanceOverride full = AppearanceOverride.EMPTY
                .withCorneaColor(new EyeColor(0.4F, 0.5F, 0.6F))
                .withIrisColor(new EyeColor(0.7F, 0.8F, 0.9F))
                .withGlow(false);
        helper.assertTrue(AppearanceOverride.fromNbt(full.toNbt()).equals(full), "fully-populated override round-trips");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void behaviorTriggerPacketRoundTrips(GameTestHelper helper) {
        EyeBehaviorTriggerPacket packet =
                new EyeBehaviorTriggerPacket(7, new ResourceLocation("somegoogly", "blink"), 8, 12345L, 3);
        byte[] first = bytes(buffer -> EyeBehaviorTriggerPacket.encode(packet, buffer));
        EyeBehaviorTriggerPacket decoded = EyeBehaviorTriggerPacket.decode(new FriendlyByteBuf(Unpooled.wrappedBuffer(first)));
        byte[] second = bytes(buffer -> EyeBehaviorTriggerPacket.encode(decoded, buffer));
        helper.assertTrue(Arrays.equals(first, second), "EyeBehaviorTriggerPacket should survive a wire round-trip");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void configSyncPacketRoundTrips(GameTestHelper helper) {
        EyeConfigSyncPacket packet = new EyeConfigSyncPacket(
                Map.of(new ResourceLocation("minecraft", "cow"), sampleConfigSet()));
        byte[] first = bytes(buffer -> EyeConfigSyncPacket.encode(packet, buffer));
        EyeConfigSyncPacket decoded = EyeConfigSyncPacket.decode(new FriendlyByteBuf(Unpooled.wrappedBuffer(first)));
        byte[] second = bytes(buffer -> EyeConfigSyncPacket.encode(decoded, buffer));
        helper.assertTrue(Arrays.equals(first, second), "EyeConfigSyncPacket should survive a wire round-trip");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void eyeColorRejectsWrongChannelCount(GameTestHelper helper) {
        JsonArray twoChannels = new JsonArray();
        twoChannels.add(Float.valueOf(0.5F));
        twoChannels.add(Float.valueOf(0.5F));
        boolean parsed = EyeColor.CODEC.parse(JsonOps.INSTANCE, twoChannels).result().isPresent();
        helper.assertTrue(!parsed, "a 2-channel color list must fail to parse");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void eyeDefinitionCodecRoundTripsAndDefaults(GameTestHelper helper) {
        // A fully-specified definition survives encode→decode by value.
        var encoded = EyeDefinition.CODEC.encodeStart(JsonOps.INSTANCE, EyeDefinition.DEFAULT).result().orElseThrow();
        EyeDefinition decoded = EyeDefinition.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
        helper.assertTrue(decoded.equals(EyeDefinition.DEFAULT), "EyeDefinition round-trips by value");

        // An empty object fills every (optional) field from defaults → equals DEFAULT.
        EyeDefinition fromEmpty = EyeDefinition.CODEC
                .parse(JsonOps.INSTANCE, new com.google.gson.JsonObject()).result().orElseThrow();
        helper.assertTrue(fromEmpty.equals(EyeDefinition.DEFAULT), "absent fields fall back to DEFAULT");
        helper.succeed();
    }

    /** Byte idempotence for one packet form: encode → decode → encode must reproduce the bytes. */
    private static <T> boolean roundTrips(T packet, java.util.function.BiConsumer<T, FriendlyByteBuf> encode,
                                          java.util.function.Function<FriendlyByteBuf, T> decode) {
        byte[] first = bytes(buffer -> encode.accept(packet, buffer));
        T decoded = decode.apply(new FriendlyByteBuf(Unpooled.wrappedBuffer(first)));
        byte[] second = bytes(buffer -> encode.accept(decoded, buffer));
        return Arrays.equals(first, second);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void pickerExportPacketRoundTrips(GameTestHelper helper) {
        CompoundTag config = new CompoundTag();
        config.putBoolean("enabled", true);
        helper.assertTrue(roundTrips(
                        new PickerExportPacket(new ResourceLocation("minecraft", "cow"), config),
                        PickerExportPacket::encode, PickerExportPacket::decode),
                "PickerExportPacket with a config should survive a wire round-trip");
        helper.assertTrue(roundTrips(
                        new PickerExportPacket(new ResourceLocation("minecraft", "cow"), null),
                        PickerExportPacket::encode, PickerExportPacket::decode),
                "PickerExportPacket's null-config form should survive a wire round-trip");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void pickerFreezePacketRoundTrips(GameTestHelper helper) {
        helper.assertTrue(roundTrips(PickerFreezePacket.freeze(new UUID(0x1234L, 0x5678L)),
                        PickerFreezePacket::encode, PickerFreezePacket::decode),
                "PickerFreezePacket's freeze form should survive a wire round-trip");
        helper.assertTrue(roundTrips(PickerFreezePacket.unfreeze(),
                        PickerFreezePacket::encode, PickerFreezePacket::decode),
                "PickerFreezePacket's unfreeze form should survive a wire round-trip");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void pickerMobPosePacketRoundTrips(GameTestHelper helper) {
        UUID mob = new UUID(0xABCDL, 0xEF01L);
        helper.assertTrue(roundTrips(PickerMobPosePacket.move(mob, 1.5, null, -7.25),
                        PickerMobPosePacket::encode, PickerMobPosePacket::decode),
                "PickerMobPosePacket's move form (with a ~ axis) should survive a wire round-trip");
        helper.assertTrue(roundTrips(PickerMobPosePacket.rot(mob, 270.0F),
                        PickerMobPosePacket::encode, PickerMobPosePacket::decode),
                "PickerMobPosePacket's rot form should survive a wire round-trip");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void pickerSpawnPacketsRoundTrip(GameTestHelper helper) {
        helper.assertTrue(roundTrips(new PickerSpawnPacket(new ResourceLocation("minecraft", "cow")),
                        PickerSpawnPacket::encode, PickerSpawnPacket::decode),
                "PickerSpawnPacket should survive a wire round-trip");
        helper.assertTrue(roundTrips(new PickerSpawnAllPacket("minecraft"),
                        PickerSpawnAllPacket::encode, PickerSpawnAllPacket::decode),
                "PickerSpawnAllPacket's filtered form should survive a wire round-trip");
        helper.assertTrue(roundTrips(new PickerSpawnAllPacket(null),
                        PickerSpawnAllPacket::encode, PickerSpawnAllPacket::decode),
                "PickerSpawnAllPacket's unfiltered form should survive a wire round-trip");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void eyeStatePacketRoundTrips(GameTestHelper helper) {
        CompoundTag overrides = AppearanceOverride.EMPTY.withIrisColor(new EyeColor(0.2F, 0.4F, 0.6F)).toNbt();
        EyeStatePacket withOverrides = new EyeStatePacket(42, true, 0.5F, overrides);
        byte[] a1 = bytes(buffer -> EyeStatePacket.encode(withOverrides, buffer));
        EyeStatePacket d1 = EyeStatePacket.decode(new FriendlyByteBuf(Unpooled.wrappedBuffer(a1)));
        byte[] a2 = bytes(buffer -> EyeStatePacket.encode(d1, buffer));
        helper.assertTrue(Arrays.equals(a1, a2), "EyeStatePacket with overrides should round-trip");

        // The nullable-overrides branch (no tag written) must also round-trip.
        EyeStatePacket noOverrides = new EyeStatePacket(43, false, 0.0F, null);
        byte[] b1 = bytes(buffer -> EyeStatePacket.encode(noOverrides, buffer));
        EyeStatePacket e1 = EyeStatePacket.decode(new FriendlyByteBuf(Unpooled.wrappedBuffer(b1)));
        byte[] b2 = bytes(buffer -> EyeStatePacket.encode(e1, buffer));
        helper.assertTrue(Arrays.equals(b1, b2), "EyeStatePacket without overrides should round-trip");
        helper.succeed();
    }
}
