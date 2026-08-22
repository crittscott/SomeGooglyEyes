package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.EyeConfigLimits;
import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.config.EyeConfigModel.HeadConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeAppearance;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import com.github.crittscott.somegoogly.network.PickerMobPosePacket;
import com.github.crittscott.somegoogly.network.PickerSpawnAllPacket;
import com.github.crittscott.somegoogly.network.PickerSpawnPacket;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.google.gson.JsonArray;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtOps;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_ADULT;

/**
 * Serialization contracts: the flat-JSON eye codecs round-trip by value (the records implement
 * {@code equals}), and the three server→client packets survive a wire round-trip. Packets carry no
 * getters, so they're checked by <b>byte idempotence</b>: {@code encode} then {@code decode} then
 * {@code encode} must reproduce the original bytes. World-less.
 */
public final class SerializationGameTestsLogic {

    private SerializationGameTestsLogic() {
    }

    public static void networkProtocolIdsAreVersionedAndUnique(GameTestHelper helper) {
        helper.assertTrue(NetworkHandler.PROTOCOL_HELLO.getPath().equals("protocol_hello"),
                "the hello channel must remain stable across gameplay protocol versions");
        helper.assertTrue(NetworkHandler.PROTOCOL_ACK.getPath().equals("protocol_ack"),
                "the acknowledgement channel must remain stable across gameplay protocol versions");
        String prefix = "v" + NetworkHandler.PROTOCOL_VERSION + "/";
        List<ResourceLocation> gameplay = List.of(
                NetworkHandler.EYE_STATE, NetworkHandler.EYE_CONFIG, NetworkHandler.EYE_BEHAVIOR,
                NetworkHandler.PICKER_FREEZE, NetworkHandler.PICKER_SPAWN,
                NetworkHandler.PICKER_SPAWN_ALL, NetworkHandler.PICKER_MOB_POSE,
                NetworkHandler.PICKER_EXPORT);
        for (ResourceLocation id : gameplay) {
            helper.assertTrue(id.getPath().startsWith(prefix),
                    "gameplay channel " + id + " must carry protocol prefix " + prefix);
        }
        helper.assertTrue(Set.copyOf(gameplay).size() == gameplay.size(),
                "every gameplay payload must have a unique channel id");
        helper.succeed();
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
        AppearanceOverride invalid = AppearanceOverride.EMPTY
                .withIrisColor(new EyeColor(Float.NaN, 0.5F, 0.5F));
        helper.assertTrue(AppearanceOverride.fromNbt(invalid.toNbt()).equals(AppearanceOverride.EMPTY),
                "non-finite portable appearance data is discarded");
        helper.succeed();
    }

    public static void behaviorTriggerPacketRoundTrips(GameTestHelper helper) {
        EyeBehaviorTriggerPacket packet =
                new EyeBehaviorTriggerPacket(7, new ResourceLocation("somegoogly", "blink"), 8, 12345L, 3);
        byte[] first = bytes(buffer -> EyeBehaviorTriggerPacket.encode(packet, buffer));
        EyeBehaviorTriggerPacket decoded = EyeBehaviorTriggerPacket.decode(new FriendlyByteBuf(Unpooled.wrappedBuffer(first)));
        byte[] second = bytes(buffer -> EyeBehaviorTriggerPacket.encode(decoded, buffer));
        helper.assertTrue(Arrays.equals(first, second), "EyeBehaviorTriggerPacket should survive a wire round-trip");
        helper.succeed();
    }

    public static void configSyncPacketRoundTrips(GameTestHelper helper) {
        EyeConfigSyncPacket packet = new EyeConfigSyncPacket(17L,
                Map.of(new ResourceLocation("minecraft", "cow"), sampleConfigSet()));
        byte[] first = bytes(buffer -> EyeConfigSyncPacket.encode(packet, buffer));
        EyeConfigSyncPacket decoded = EyeConfigSyncPacket.decode(new FriendlyByteBuf(Unpooled.wrappedBuffer(first)));
        byte[] second = bytes(buffer -> EyeConfigSyncPacket.encode(decoded, buffer));
        helper.assertTrue(Arrays.equals(first, second), "EyeConfigSyncPacket should survive a wire round-trip");
        helper.succeed();
    }

    public static void configSyncRejectsOversizedAndUnsafePayloads(GameTestHelper helper) {
        FriendlyByteBuf oversized = new FriendlyByteBuf(Unpooled.buffer());
        oversized.writeLong(1L);
        oversized.writeVarInt(EyeConfigLimits.MAX_CONFIGS_PER_SYNC + 1);
        helper.assertTrue(throwsRuntime(() -> EyeConfigSyncPacket.decode(oversized)),
                "config sync must reject an oversized outer count before allocating entries");

        CompoundTag tooManyVariants = new CompoundTag();
        ListTag variants = new ListTag();
        for (int i = 0; i <= EyeConfigLimits.MAX_VARIANTS_PER_CONFIG; i++) {
            variants.add(new CompoundTag());
        }
        tooManyVariants.put("variants", variants);
        helper.assertTrue(EyeConfigLimits.validateWireRuntimeConfig(tooManyVariants).limitExceeded(),
                "raw nested lists must be budgeted before codec parsing");

        RuntimeConfigSet unsafe = sampleConfigSet();
        HeadConfig head = unsafe.any.variants.get(0).heads.get(0);
        head.eyes = List.of(new EyeDefinition(
                new EyePlacement(new Vec3(Double.NaN, 0.0, 0.0), 1.0F, 1.0F, 1.0F,
                        0.0F, 0.0F, EyePlacement.NO_CROSS_TARGET), EyeAppearance.DEFAULT));
        FriendlyByteBuf numeric = new FriendlyByteBuf(Unpooled.buffer());
        numeric.writeLong(2L);
        numeric.writeVarInt(1);
        numeric.writeResourceLocation(new ResourceLocation("minecraft", "cow"));
        numeric.writeNbt((CompoundTag) RuntimeConfigSet.CODEC.encodeStart(NbtOps.INSTANCE, unsafe)
                .result().orElseThrow());
        helper.assertTrue(throwsRuntime(() -> EyeConfigSyncPacket.decode(numeric)),
                "config sync must reject non-finite placement values");
        helper.succeed();
    }

    public static void eyeColorRejectsWrongChannelCount(GameTestHelper helper) {
        JsonArray twoChannels = new JsonArray();
        twoChannels.add(Float.valueOf(0.5F));
        twoChannels.add(Float.valueOf(0.5F));
        boolean parsed = EyeColor.CODEC.parse(JsonOps.INSTANCE, twoChannels).result().isPresent();
        helper.assertTrue(!parsed, "a 2-channel color list must fail to parse");
        helper.succeed();
    }

    /**
     * The canonical form is whatever {@code encode} produces, so this is the guard that stops a field
     * added to the record from being silently dropped on the way to disk: a value-equal round-trip can
     * only pass if every field was written. {@code DEFAULT} would round-trip even with a field elided,
     * so the sample deliberately sets each field away from its default.
     */
    public static void eyeDefinitionCodecRoundTripsEveryField(GameTestHelper helper) {
        EyeDefinition sample = new EyeDefinition(
                new EyePlacement(new Vec3(0.5, -0.25, 0.125), 0.4F, 0.3F, 2F, 45F, 135F, 1),
                new EyeAppearance(new EyeColor(0.1F, 0.2F, 0.3F), new EyeColor(0.4F, 0.5F, 0.6F), true));
        var encoded = EyeDefinition.CODEC.encodeStart(JsonOps.INSTANCE, sample).result().orElseThrow();
        EyeDefinition decoded = EyeDefinition.CODEC.parse(JsonOps.INSTANCE, encoded).result().orElseThrow();
        helper.assertTrue(decoded.equals(sample), "every eye field must survive encode→decode by value");

        // Every field is required: an empty object is a parse failure, not a tree of defaults.
        boolean parsedEmpty = EyeDefinition.CODEC
                .parse(JsonOps.INSTANCE, new com.google.gson.JsonObject()).result().isPresent();
        helper.assertTrue(!parsedEmpty, "an eye with no fields must fail to parse");
        helper.succeed();
    }

    /**
     * Float-precision serialization: a value typed as {@code 0.22} must come back out as {@code 0.22},
     * not as the {@code 0.2199999988079071} a double-widened float prints as. The datapack files are
     * hand-edited, so this is what keeps them readable.
     */
    public static void eyeFieldsSerializeAtFloatPrecision(GameTestHelper helper) {
        EyeDefinition sample = new EyeDefinition(
                new EyePlacement(new Vec3(0.22, 0.22, 0.22), 0.22F, 0.22F, 0.22F, 0.22F, 0.22F, -1),
                EyeAppearance.DEFAULT);
        String json = EyeDefinition.CODEC.encodeStart(JsonOps.INSTANCE, sample).result().orElseThrow().toString();
        helper.assertTrue(!json.contains("0.219999"),
                "float-widening noise must not reach the datapack JSON, got: " + json);
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

    public static void pickerExportPacketRoundTrips(GameTestHelper helper) {
        CompoundTag config = new CompoundTag();
        config.putBoolean("enabled", true);
        helper.assertTrue(roundTrips(
                        new PickerExportPacket(new ResourceLocation("minecraft", "cow"), AGE_ADULT, config),
                        PickerExportPacket::encode, PickerExportPacket::decode),
                "PickerExportPacket with a config should survive a wire round-trip");
        helper.assertTrue(roundTrips(
                        new PickerExportPacket(new ResourceLocation("minecraft", "cow"), AGE_ADULT, null),
                        PickerExportPacket::encode, PickerExportPacket::decode),
                "PickerExportPacket's null-config form should survive a wire round-trip");
        helper.succeed();
    }

    public static void pickerFreezePacketRoundTrips(GameTestHelper helper) {
        helper.assertTrue(roundTrips(PickerFreezePacket.freeze(new UUID(0x1234L, 0x5678L)),
                        PickerFreezePacket::encode, PickerFreezePacket::decode),
                "PickerFreezePacket's freeze form should survive a wire round-trip");
        helper.assertTrue(roundTrips(PickerFreezePacket.unfreeze(),
                        PickerFreezePacket::encode, PickerFreezePacket::decode),
                "PickerFreezePacket's unfreeze form should survive a wire round-trip");
        helper.succeed();
    }

    public static void pickerMobPosePacketRoundTrips(GameTestHelper helper) {
        UUID mob = new UUID(0xABCDL, 0xEF01L);
        helper.assertTrue(roundTrips(PickerMobPosePacket.move(mob, 1.5, 0.0, -7.25),
                        PickerMobPosePacket::encode, PickerMobPosePacket::decode),
                "PickerMobPosePacket's move form (offsets) should survive a wire round-trip");
        helper.assertTrue(roundTrips(PickerMobPosePacket.rot(mob, 270.0F),
                        PickerMobPosePacket::encode, PickerMobPosePacket::decode),
                "PickerMobPosePacket's rot form should survive a wire round-trip");
        helper.succeed();
    }

    public static void pickerMobPoseRejectsNonFiniteForms(GameTestHelper helper) {
        UUID mob = UUID.randomUUID();
        helper.assertTrue(!PickerMobPosePacket.move(mob, Double.NaN, 0.0, 0.0).isValid(),
                "NaN movement must be rejected");
        helper.assertTrue(!PickerMobPosePacket.rot(mob, Float.NaN).isValid(),
                "NaN rotation must be rejected");
        helper.assertTrue(PickerMobPosePacket.move(mob, 1.0, 2.0, 3.0).isValid(),
                "finite movement must remain valid");
        helper.succeed();
    }

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

    public static void eyeStatePacketRoundTrips(GameTestHelper helper) {
        AppearanceOverride overrides =
                AppearanceOverride.EMPTY.withIrisColor(new EyeColor(0.2F, 0.4F, 0.6F));
        EyeState.Snapshot snapshot = new EyeState.Snapshot(true, 0.5F, overrides);
        EyeStatePacket withOverrides = new EyeStatePacket(42, snapshot);
        byte[] a1 = bytes(buffer -> EyeStatePacket.encode(withOverrides, buffer));
        EyeStatePacket d1 = EyeStatePacket.decode(new FriendlyByteBuf(Unpooled.wrappedBuffer(a1)));
        helper.assertTrue(d1.snapshot().equals(snapshot), "EyeStatePacket should preserve its snapshot");
        byte[] a2 = bytes(buffer -> EyeStatePacket.encode(d1, buffer));
        helper.assertTrue(Arrays.equals(a1, a2), "EyeStatePacket with overrides should round-trip");

        EyeStatePacket noOverrides = new EyeStatePacket(43, false, 0.0F, AppearanceOverride.EMPTY);
        byte[] b1 = bytes(buffer -> EyeStatePacket.encode(noOverrides, buffer));
        EyeStatePacket e1 = EyeStatePacket.decode(new FriendlyByteBuf(Unpooled.wrappedBuffer(b1)));
        byte[] b2 = bytes(buffer -> EyeStatePacket.encode(e1, buffer));
        helper.assertTrue(Arrays.equals(b1, b2), "EyeStatePacket without overrides should round-trip");
        helper.succeed();
    }

    public static void eyeStatePacketRejectsNonFiniteValues(GameTestHelper helper) {
        FriendlyByteBuf invalid = new FriendlyByteBuf(Unpooled.buffer());
        invalid.writeInt(42);
        invalid.writeBoolean(true);
        invalid.writeFloat(Float.NaN);
        invalid.writeByte(0);
        helper.assertTrue(throwsRuntime(() -> EyeStatePacket.decode(invalid)),
                "eye-state sync must reject a non-finite variant roll");
        helper.succeed();
    }

    private static boolean throwsRuntime(Runnable action) {
        try {
            action.run();
            return false;
        } catch (RuntimeException expected) {
            return true;
        }
    }
}
