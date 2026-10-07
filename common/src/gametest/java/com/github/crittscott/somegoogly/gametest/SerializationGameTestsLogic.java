package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.EyeConfigLimits;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
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
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.google.gson.JsonArray;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtOps;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_ADULT;

/**
 * Serialization contracts: the flat-JSON eye codecs round-trip by value, and all five native
 * payloads preserve their wire representation through encode/decode. World-less.
 */
public final class SerializationGameTestsLogic {

    private SerializationGameTestsLogic() {
    }

    public static void networkProtocolIdsAreVersionedAndUnique(GameTestHelper helper) {
        String prefix = "v" + NetworkHandler.NETWORK_VERSION + "/";
        List<ResourceLocation> gameplay = List.of(
                NetworkHandler.EYE_STATE, NetworkHandler.EYE_CONFIG, NetworkHandler.EYE_BEHAVIOR,
                NetworkHandler.PICKER_FREEZE, NetworkHandler.PICKER_EXPORT);
        for (ResourceLocation id : gameplay) {
            helper.assertTrue(id.getPath().startsWith(prefix),
                    "gameplay channel " + id + " must carry protocol prefix " + prefix);
        }
        helper.assertTrue(Set.copyOf(gameplay).size() == gameplay.size(),
                "every gameplay payload must have a unique channel id");
        List<ResourceLocation> typedPayloads = List.of(
                EyeStatePacket.TYPE.id(), EyeConfigSyncPacket.TYPE.id(), EyeBehaviorTriggerPacket.TYPE.id(),
                PickerFreezePacket.TYPE.id(), PickerExportPacket.TYPE.id());
        helper.assertTrue(typedPayloads.equals(gameplay),
                "the native payload types must preserve every established wire id");
        helper.succeed();
    }

    private static RegistryFriendlyByteBuf buffer(GameTestHelper helper) {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
    }

    private static <T> byte[] bytes(GameTestHelper helper, StreamCodec<RegistryFriendlyByteBuf, T> codec, T packet) {
        RegistryFriendlyByteBuf buffer = buffer(helper);
        codec.encode(buffer, packet);
        byte[] out = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), out);
        return out;
    }

    /**
     * Byte idempotence for one packet form through its {@code STREAM_CODEC}: encode → decode → encode must
     * reproduce the bytes. Returns the decoded packet.
     */
    private static <T> T roundTrip(GameTestHelper helper, StreamCodec<RegistryFriendlyByteBuf, T> codec, T packet,
                                   String description) {
        byte[] first = bytes(helper, codec, packet);
        T decoded = codec.decode(new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(first),
                helper.getLevel().registryAccess()));
        byte[] second = bytes(helper, codec, decoded);
        helper.assertTrue(Arrays.equals(first, second), description + " should survive a wire round-trip");
        return decoded;
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
                new EyeBehaviorTriggerPacket(7,
                        ResourceLocation.fromNamespaceAndPath("somegoogly", "blink"), 8, 12345L, 3);
        roundTrip(helper, EyeBehaviorTriggerPacket.STREAM_CODEC, packet, "EyeBehaviorTriggerPacket");
        helper.succeed();
    }

    public static void configSyncPacketRoundTrips(GameTestHelper helper) {
        Map<ResourceLocation, RuntimeConfigSet> configs =
                Map.of(ResourceLocation.fromNamespaceAndPath("minecraft", "cow"), sampleConfigSet());
        EyeConfigSyncPacket packet = new EyeConfigSyncPacket(configs, ServerEyeConfigs.encode(configs), false);
        EyeConfigSyncPacket decoded = roundTrip(helper, EyeConfigSyncPacket.STREAM_CODEC, packet, "EyeConfigSyncPacket");
        helper.assertTrue(!decoded.googlyEyesEnabled(), "the googlyEyesEnabled flag should survive the round-trip");
        helper.succeed();
    }

    public static void configSyncRejectsOversizedAndUnsafePayloads(GameTestHelper helper) {
        RegistryFriendlyByteBuf oversized = buffer(helper);
        oversized.writeVarInt(EyeConfigLimits.MAX_CONFIGS_PER_SYNC + 1);
        helper.assertTrue(throwsRuntime(() -> EyeConfigSyncPacket.STREAM_CODEC.decode(oversized)),
                "config sync must reject an oversized outer count before allocating entries");

        RuntimeConfigSet tooManyVariants = sampleConfigSet();
        tooManyVariants.any.variants = Collections.nCopies(EyeConfigLimits.MAX_VARIANTS_PER_CONFIG + 1,
                tooManyVariants.any.variants.get(0));
        RegistryFriendlyByteBuf counted = singleConfigPayload(helper, tooManyVariants);
        helper.assertTrue(throwsRuntime(() -> EyeConfigSyncPacket.STREAM_CODEC.decode(counted)),
                "config sync must reject a config over the variant limit");

        RuntimeConfigSet unsafe = sampleConfigSet();
        HeadConfig head = unsafe.any.variants.get(0).heads.get(0);
        head.eyes = List.of(new EyeDefinition(
                new EyePlacement(new Vec3(Double.NaN, 0.0, 0.0), 1.0F, 1.0F, 1.0F,
                        0.0F, 0.0F, EyePlacement.NO_CROSS_TARGET), EyeAppearance.DEFAULT));
        RegistryFriendlyByteBuf numeric = singleConfigPayload(helper, unsafe);
        helper.assertTrue(throwsRuntime(() -> EyeConfigSyncPacket.STREAM_CODEC.decode(numeric)),
                "config sync must reject non-finite placement values");
        helper.succeed();
    }

    /** A config-sync payload carrying {@code set} for one entity, written without the encoder's own limit check. */
    private static RegistryFriendlyByteBuf singleConfigPayload(GameTestHelper helper, RuntimeConfigSet set) {
        RegistryFriendlyByteBuf buffer = buffer(helper);
        buffer.writeVarInt(1);
        buffer.writeResourceLocation(ResourceLocation.fromNamespaceAndPath("minecraft", "cow"));
        buffer.writeNbt((CompoundTag) RuntimeConfigSet.CODEC.encodeStart(NbtOps.INSTANCE, set)
                .result().orElseThrow());
        buffer.writeBoolean(true);
        return buffer;
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

    public static void pickerExportPacketRoundTrips(GameTestHelper helper) {
        CompoundTag config = new CompoundTag();
        config.putBoolean("enabled", true);
        roundTrip(helper, PickerExportPacket.STREAM_CODEC,
                new PickerExportPacket(ResourceLocation.fromNamespaceAndPath("minecraft", "cow"), AGE_ADULT, config),
                "PickerExportPacket with a config");
        roundTrip(helper, PickerExportPacket.STREAM_CODEC,
                new PickerExportPacket(ResourceLocation.fromNamespaceAndPath("minecraft", "cow"), AGE_ADULT, null),
                "PickerExportPacket's null-config form");
        helper.succeed();
    }

    public static void pickerFreezePacketRoundTrips(GameTestHelper helper) {
        roundTrip(helper, PickerFreezePacket.STREAM_CODEC, PickerFreezePacket.freeze(new UUID(0x1234L, 0x5678L)),
                "PickerFreezePacket's freeze form");
        roundTrip(helper, PickerFreezePacket.STREAM_CODEC, PickerFreezePacket.unfreeze(),
                "PickerFreezePacket's unfreeze form");
        helper.succeed();
    }

    public static void eyeStatePacketRoundTrips(GameTestHelper helper) {
        AppearanceOverride overrides =
                AppearanceOverride.EMPTY.withIrisColor(new EyeColor(0.2F, 0.4F, 0.6F));
        EyeState.Snapshot snapshot = new EyeState.Snapshot(true, 0.5F, overrides);
        EyeStatePacket withOverrides = new EyeStatePacket(42, new UUID(1L, 2L), snapshot);
        EyeStatePacket d1 = roundTrip(helper, EyeStatePacket.STREAM_CODEC, withOverrides, "EyeStatePacket with overrides");
        helper.assertTrue(d1.snapshot().equals(snapshot), "EyeStatePacket should preserve its snapshot");

        EyeStatePacket noOverrides = new EyeStatePacket(
                43, new UUID(3L, 4L), new EyeState.Snapshot(false, 0.0F, AppearanceOverride.EMPTY));
        roundTrip(helper, EyeStatePacket.STREAM_CODEC, noOverrides, "EyeStatePacket without overrides");
        helper.succeed();
    }

    public static void eyeStatePacketRejectsNonFiniteValues(GameTestHelper helper) {
        RegistryFriendlyByteBuf invalid = buffer(helper);
        invalid.writeInt(42);
        invalid.writeUUID(new UUID(1L, 2L));
        invalid.writeBoolean(true);
        invalid.writeFloat(Float.NaN);
        invalid.writeByte(0);
        helper.assertTrue(throwsRuntime(() -> EyeStatePacket.STREAM_CODEC.decode(invalid)),
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
