package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.command.MaybeFloatArgumentType;
import com.github.crittscott.somegoogly.config.EyeConfigLimits;
import com.github.crittscott.somegoogly.config.EyeConfigModel.HeadConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.OngoingBehavior;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeAppearance;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.network.EyeAngerPacket;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.network.EyeConfigSyncPacket;
import com.github.crittscott.somegoogly.network.EyeSettingsPacket;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.Payload;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.github.crittscott.somegoogly.network.PickerFreezePacket;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import java.util.UUID;
import java.util.function.Function;

import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_ADULT;
import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_BABY;

/**
 * Serialization contracts: the flat-JSON eye codecs round-trip by value, and all seven native
 * payloads preserve their wire representation through encode/decode. World-less.
 */
public final class SerializationGameTestsLogic {

    private SerializationGameTestsLogic() {
    }

    /**
     * Every payload id is unique. No in-game form; guards that no two payloads are misread as each other.
     */
    public static void networkProtocolIdsAreUnique(GameTestHelper helper) {
        List<ResourceLocation> typedPayloads = List.of(
                EyeStatePacket.ID, EyeConfigSyncPacket.ID, EyeSettingsPacket.ID, EyeBehaviorTriggerPacket.ID,
                EyeAngerPacket.ID, PickerFreezePacket.ID, PickerExportPacket.ID);
        helper.assertTrue(Set.copyOf(typedPayloads).size() == typedPayloads.size(),
                "every gameplay payload must have a unique channel id");
        List<ResourceLocation> established = List.of(
                SomeGooglyCommon.id("eye_state"), SomeGooglyCommon.id("eye_config"), SomeGooglyCommon.id("eye_settings"),
                SomeGooglyCommon.id("eye_behavior"), SomeGooglyCommon.id("eye_anger"),
                SomeGooglyCommon.id("picker_freeze"), SomeGooglyCommon.id("picker_export"));
        helper.assertTrue(typedPayloads.equals(established),
                "the native payload types must preserve every established wire id");
        helper.succeed();
    }

    private static FriendlyByteBuf buffer(GameTestHelper helper) {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    private static byte[] bytes(GameTestHelper helper, Payload packet) {
        FriendlyByteBuf buffer = buffer(helper);
        packet.write(buffer);
        byte[] out = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), out);
        return out;
    }

    /**
     * Byte idempotence for one packet form through its {@code write} and {@code read}: encode → decode → encode must
     * reproduce the bytes. Returns the decoded packet.
     */
    private static <T extends Payload> T roundTrip(GameTestHelper helper, Function<FriendlyByteBuf, T> reader,
                                                   T packet, String description) {
        byte[] first = bytes(helper, packet);
        T decoded = reader.apply(new FriendlyByteBuf(Unpooled.wrappedBuffer(first)));
        byte[] second = bytes(helper, decoded);
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

    /**
     * Appearance overrides save only the fields that are set, and reject invalid colors. In game: aim at an
     * eyed cow and run {@code /sg admin tint iris 1 0 0}; on Forge, {@code /data get entity <cow>}
     * shows {@code somegoogly:eyeOverrides} in {@code ForgeData} holding only
     * {@code irisColor}, and after save and reload the cow still has red irises.
     */
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
        // /give and /data write decimals as doubles.
        ListTag doubles = new ListTag();
        doubles.add(DoubleTag.valueOf(0.25D));
        doubles.add(DoubleTag.valueOf(0.5D));
        doubles.add(DoubleTag.valueOf(0.75D));
        CompoundTag typed = new CompoundTag();
        typed.put("irisColor", doubles);
        helper.assertTrue(AppearanceOverride.fromNbt(typed).equals(
                AppearanceOverride.EMPTY.withIrisColor(new EyeColor(0.25F, 0.5F, 0.75F))),
                "a hand-typed color list reads whatever its number type");
        helper.succeed();
    }

    /**
     * The behavior trigger survives the wire, and every behavior has its own wire id. No in-game form; guards
     * that every client sees the expression the server started and refuses an unknown number or an elapsed
     * time outside that expression's lifetime.
     */
    public static void behaviorTriggerPacketRoundTrips(GameTestHelper helper) {
        for (EyeBehavior behavior : EyeBehavior.values()) {
            for (int elapsed : new int[]{0, 3, behavior.defaultDuration()}) {
                EyeBehaviorTriggerPacket decoded = roundTrip(helper, EyeBehaviorTriggerPacket::read,
                        new EyeBehaviorTriggerPacket(7, behavior, 12345L, elapsed), "EyeBehaviorTriggerPacket");
                helper.assertTrue(decoded.behavior() == behavior && decoded.elapsed() == elapsed
                                && decoded.seed() == 12345L,
                        behavior + " and its elapsed time should survive the round-trip");
            }
            for (int elapsed : new int[]{-1, behavior.defaultDuration() + 1}) {
                FriendlyByteBuf invalid = buffer(helper);
                try {
                    new EyeBehaviorTriggerPacket(7, behavior, 12345L, elapsed).write(invalid);
                    helper.assertTrue(throwsRuntime(() -> EyeBehaviorTriggerPacket.read(invalid)),
                            behavior + " must reject elapsed time outside its default duration");
                } finally {
                    invalid.release();
                }
            }
        }
        FriendlyByteBuf unknown = buffer(helper);
        unknown.writeVarInt(7);
        unknown.writeVarInt(EyeBehavior.values().length);
        boolean rejected = false;
        try {
            EyeBehaviorTriggerPacket.read(unknown);
        } catch (DecoderException e) {
            rejected = e.getMessage().contains("Unknown eye behavior");
        } finally {
            unknown.release();
        }
        helper.assertTrue(rejected, "a trigger with an unassigned behavior number should fail to decode");
        helper.succeed();
    }

    /**
     * Picker numbers print the way {@code /sg} parses them on any computer. In game, on a system set to German:
     * {@code /sg list eyes} and the picker HUD show {@code 1.250}, not {@code 1,250}.
     */
    public static void pickerNumbersIgnoreTheComputersLanguage(GameTestHelper helper) {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            helper.assertTrue(MaybeFloatArgumentType.position(1.25).equals("1.250"), "positions use a dot");
            helper.assertTrue(MaybeFloatArgumentType.compact(1.25).equals("1.25"), "scales use a dot");
            helper.assertTrue(MaybeFloatArgumentType.angle(90.0).equals("90"), "angles are whole");
        } finally {
            Locale.setDefault(original);
        }
        helper.succeed();
    }

    /**
     * The anger report survives the wire. No in-game form; guards that every client sees the anger the
     * server shares.
     */
    public static void angerPacketRoundTrips(GameTestHelper helper) {
        EyeAngerPacket decoded = roundTrip(helper, EyeAngerPacket::read, new EyeAngerPacket(7, true), "EyeAngerPacket");
        helper.assertTrue(decoded.entityId() == 7 && decoded.angry(), "EyeAngerPacket should preserve its fields");
        helper.succeed();
    }

    /**
     * The eye-definition sync survives the wire, and a full modpack's set fits its budgets. No in-game form;
     * guards that joining clients draw the same eyes the server's datapacks define.
     */
    public static void configSyncPacketRoundTrips(GameTestHelper helper) {
        Map<ResourceLocation, RuntimeConfigSet> configs =
                Map.of(new ResourceLocation("minecraft", "cow"), sampleConfigSet());
        Map<ResourceLocation, CompoundTag> encoded = ServerEyeConfigs.encode(configs);
        EyeConfigSyncPacket packet = new EyeConfigSyncPacket(configs, encoded);
        EyeConfigSyncPacket decoded = roundTrip(helper, EyeConfigSyncPacket::read, packet, "EyeConfigSyncPacket");
        helper.assertTrue(decoded.configs().size() == 1, "the definitions should survive the round-trip");

        Map<ResourceLocation, RuntimeConfigSet> large = new HashMap<>();
        for (int i = 0; i < 1_200; i++) {
            RuntimeConfigSet set = sampleConfigSet();
            set.any.variants.get(0).heads.get(0).eyes = Collections.nCopies(4, EyeDefinition.DEFAULT);
            large.put(new ResourceLocation("somegoogly", "sync_test_" + i), set);
        }
        Map<ResourceLocation, CompoundTag> largeEncoded = ServerEyeConfigs.encode(large);
        helper.assertTrue(EyeConfigLimits.validateSync(large) == null,
                "4,800 eyes must fit the shared work budget");
        EyeConfigSyncPacket largePacket = new EyeConfigSyncPacket(large, largeEncoded);
        helper.assertTrue(largePacket.refusal() == null,
                "a full modpack's definitions must fit the decoded and compressed size budgets");
        FriendlyByteBuf largeWire = buffer(helper);
        try {
            largePacket.write(largeWire);
            EyeConfigSyncPacket largeDecoded = EyeConfigSyncPacket.read(largeWire);
            // Entity-map iteration order is not part of the protocol; it can change the compressed bytes.
            helper.assertTrue(ServerEyeConfigs.encode(largeDecoded.configs()).equals(largeEncoded),
                    "every entity and its complete eye definition must survive the compressed round-trip");
            helper.assertTrue(largeWire.readableBytes() == 0,
                    "the compressed round-trip must consume the entire packet");
        } finally {
            largeWire.release();
        }
        helper.succeed();
    }

    /**
     * The master switch and behavior settings survive the wire. No in-game form; guards that clients run ongoing
     * behaviors and placed heads' and the held eye's expressions from the server's settings.
     */
    public static void settingsPacketRoundTrips(GameTestHelper helper) {
        BehaviorSettings behaviors = new BehaviorSettings(true, 300, 100,
                List.of(EyeBehavior.BLINK, EyeBehavior.CROSS_EYE), 40, false, 60,
                EnumSet.of(OngoingBehavior.ANGER, OngoingBehavior.SWELL_RATTLE));
        EyeSettingsPacket decoded = roundTrip(helper, EyeSettingsPacket::read,
                new EyeSettingsPacket(false, behaviors), "EyeSettingsPacket");
        helper.assertTrue(!decoded.googlyEyesEnabled(), "the googlyEyesEnabled flag should survive the round-trip");
        helper.assertTrue(decoded.behaviors().equals(behaviors), "the behavior settings should survive the round-trip");
        helper.succeed();
    }

    /**
     * The client refuses an eye-definition sync with too many entries, too many variants, or non-finite
     * geometry. No in-game form with an unmodified server; guards clients against a malicious server.
     */
    public static void configSyncRejectsOversizedAndUnsafePayloads(GameTestHelper helper) {
        FriendlyByteBuf safe = singleConfigPayload(helper, sampleConfigSet());
        try {
            helper.assertTrue(EyeConfigSyncPacket.read(safe).configs().size() == 1
                            && safe.readableBytes() == 0,
                    "the fixture builder must produce a complete decodable payload");
        } finally {
            safe.release();
        }
        FriendlyByteBuf oversized = buffer(helper);
        oversized.writeVarInt(EyeConfigLimits.MAX_CONFIGS_PER_SYNC + 1);
        helper.assertTrue(rejectsConfig(compressedPayload(helper, oversized), "Eye config count exceeds network limit"),
                "config sync must reject an oversized outer count before allocating entries");

        FriendlyByteBuf expanded = buffer(helper);
        expanded.writeBytes(new byte[EyeConfigSyncPacket.MAX_PAYLOAD_BYTES + 1]);
        helper.assertTrue(rejectsConfig(compressedPayload(helper, expanded), "Decoded eye config sync exceeds network limit"),
                "a small compressed payload must not expand beyond the decoded byte cap");
        FriendlyByteBuf tooManyBytes = buffer(helper);
        tooManyBytes.writeBytes(new byte[EyeConfigSyncPacket.MAX_COMPRESSED_BYTES + 1]);
        helper.assertTrue(rejectsConfig(tooManyBytes, "Eye config sync payload exceeds network limit"),
                "an oversized wire payload must be refused before decompression");
        FriendlyByteBuf malformed = buffer(helper);
        malformed.writeByte(0);
        helper.assertTrue(rejectsConfig(malformed, "Malformed compressed eye config sync"),
                "invalid gzip must be refused");

        RuntimeConfigSet tooManyVariants = sampleConfigSet();
        tooManyVariants.any.variants = Collections.nCopies(EyeConfigLimits.MAX_VARIANTS_PER_CONFIG + 1,
                tooManyVariants.any.variants.get(0));
        FriendlyByteBuf counted = singleConfigPayload(helper, tooManyVariants);
        helper.assertTrue(rejectsConfig(counted, "variant count"),
                "config sync must reject a config over the variant limit");

        RuntimeConfigSet unsafe = sampleConfigSet();
        HeadConfig head = unsafe.any.variants.get(0).heads.get(0);
        head.eyes = List.of(new EyeDefinition(
                new EyePlacement(new Vec3(Double.NaN, 0.0, 0.0), 1.0F, 1.0F, 1.0F,
                        0.0F, 0.0F, EyePlacement.NO_CROSS_TARGET), EyeAppearance.DEFAULT));
        FriendlyByteBuf numeric = singleConfigPayload(helper, unsafe);
        helper.assertTrue(rejectsConfig(numeric, "position is not finite"),
                "config sync must reject non-finite placement values");
        helper.succeed();
    }

    /** Require the intended definition rejection, rather than a truncated-buffer or unrelated failure. */
    private static boolean rejectsConfig(FriendlyByteBuf buffer, String diagnostic) {
        try {
            EyeConfigSyncPacket.read(buffer);
            return false;
        } catch (DecoderException e) {
            return e.getMessage().contains(diagnostic);
        } finally {
            buffer.release();
        }
    }

    /** Compress an independently authored wire body, releasing the original buffer. */
    private static FriendlyByteBuf compressedPayload(GameTestHelper helper, FriendlyByteBuf raw) {
        byte[] bytes = new byte[raw.readableBytes()];
        raw.readBytes(bytes);
        raw.release();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(bytes);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        FriendlyByteBuf compressed = buffer(helper);
        compressed.writeBytes(output.toByteArray());
        return compressed;
    }

    /** A compressed config-sync fixture carrying {@code set} for one entity. */
    private static FriendlyByteBuf singleConfigPayload(GameTestHelper helper, RuntimeConfigSet set) {
        FriendlyByteBuf buffer = buffer(helper);
        buffer.writeVarInt(1);
        buffer.writeResourceLocation(new ResourceLocation("minecraft", "cow"));
        buffer.writeNbt((CompoundTag) RuntimeConfigSet.CODEC.encodeStart(NbtOps.INSTANCE, set)
                .result().orElseThrow());
        return compressedPayload(helper, buffer);
    }

    /**
     * A color must have exactly three channels. In game: a datapack eye definition with
     * {@code "irisColors": [0.5, 0.5]} is ignored on {@code /reload}, with an error in the log.
     */
    public static void eyeColorRejectsWrongChannelCount(GameTestHelper helper) {
        JsonArray twoChannels = new JsonArray();
        twoChannels.add(Float.valueOf(0.5F));
        twoChannels.add(Float.valueOf(0.5F));
        boolean parsed = EyeColor.CODEC.parse(JsonOps.INSTANCE, twoChannels).result().isPresent();
        helper.assertTrue(!parsed, "a 2-channel color list must fail to parse");
        helper.succeed();
    }

    /**
     * Both color codecs reject a channel outside {@code 0..1} or non-finite on decode. In-game,
     * {@code /give @s somegoogly:googly_eye[somegoogly:eye_properties={irisColor:[2f,0f,0f]}]} fails to
     * parse instead of producing an eye item.
     */
    public static void eyeColorRejectsOutOfRangeChannels(GameTestHelper helper) {
        for (float bad : new float[]{2.0F, -0.5F, Float.NaN, Float.POSITIVE_INFINITY}) {
            JsonArray channels = new JsonArray();
            channels.add(Float.valueOf(bad));
            channels.add(Float.valueOf(0.5F));
            channels.add(Float.valueOf(0.5F));
            helper.assertTrue(EyeColor.CODEC.parse(JsonOps.INSTANCE, channels).result().isEmpty(),
                    "codec must reject channel " + bad);

            FriendlyByteBuf buffer = buffer(helper);
            buffer.writeFloat(bad);
            buffer.writeFloat(0.5F);
            buffer.writeFloat(0.5F);
            helper.assertTrue(throwsRuntime(() -> EyeColor.read(buffer)),
                    "stream codec must reject channel " + bad);
        }
        helper.succeed();
    }

    /**
     * The canonical form is whatever {@code encode} produces, so this is the guard that stops a field
     * added to the record from being silently dropped on the way to disk: a value-equal round-trip can
     * only pass if every field was written. {@code DEFAULT} would round-trip even with a field elided,
     * so the sample deliberately sets each field away from its default. No in-game form; guards that
     * {@code /sg export} writes every eye property an author set.
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
                .parse(JsonOps.INSTANCE, new JsonObject()).result().isPresent();
        helper.assertTrue(!parsedEmpty, "an eye with no fields must fail to parse");
        helper.succeed();
    }

    /**
     * Float-precision serialization: a value typed as {@code 0.22} must come back out as {@code 0.22},
     * not as the {@code 0.2199999988079071} a double-widened float prints as. The datapack files are
     * hand-edited, so this is what keeps them readable. In game: give an eye in the picker a
     * position component of 0.22 and run {@code /sg exportall}; the exported JSON reads {@code 0.22}.
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

    /**
     * The picker export request survives the wire; a request with no definition or an unsupported age is refused on decode. No
     * in-game form; guards that {@code /sg export} delivers exactly the definition the client authored.
     */
    public static void pickerExportPacketRoundTrips(GameTestHelper helper) {
        CompoundTag config = new CompoundTag();
        config.putBoolean("enabled", true);
        for (String age : List.of(AGE_ADULT, AGE_BABY)) {
            roundTrip(helper, PickerExportPacket::read,
                    new PickerExportPacket(new ResourceLocation("minecraft", "cow"), age, config),
                    "PickerExportPacket with a config");
        }
        for (String age : List.of("", "any", "Adult", "unknown")) {
            FriendlyByteBuf invalidAge = buffer(helper);
            try {
                new PickerExportPacket(new ResourceLocation("minecraft", "cow"), age, config).write(invalidAge);
                helper.assertTrue(throwsRuntime(() -> PickerExportPacket.read(invalidAge)),
                        "picker export must reject unsupported ages on decode");
            } finally {
                invalidAge.release();
            }
        }

        FriendlyByteBuf noConfig = buffer(helper);
        noConfig.writeResourceLocation(new ResourceLocation("minecraft", "cow"));
        noConfig.writeUtf(AGE_ADULT);
        noConfig.writeNbt(null);
        helper.assertTrue(throwsRuntime(() -> PickerExportPacket.read(noConfig)),
                "picker export must reject a packet with no config");
        helper.succeed();
    }

    /**
     * Picker choose and release requests survive the wire. No in-game form; guards that {@code V} in the
     * picker freezes and releases the intended mob.
     */
    public static void pickerFreezePacketRoundTrips(GameTestHelper helper) {
        roundTrip(helper, PickerFreezePacket::read, PickerFreezePacket.freeze(new UUID(0x1234L, 0x5678L)),
                "PickerFreezePacket's freeze form");
        roundTrip(helper, PickerFreezePacket::read, PickerFreezePacket.unfreeze(),
                "PickerFreezePacket's unfreeze form");
        helper.succeed();
    }

    /**
     * Entity eye state survives the wire, with and without appearance overrides. No in-game form; guards that
     * every watching client sees a mob's eyes, placement, and colors as the server has them.
     */
    public static void eyeStatePacketRoundTrips(GameTestHelper helper) {
        AppearanceOverride overrides =
                AppearanceOverride.EMPTY.withIrisColor(new EyeColor(0.2F, 0.4F, 0.6F));
        EyeState.Snapshot snapshot = new EyeState.Snapshot(true, 0.5F, overrides);
        EyeStatePacket withOverrides = new EyeStatePacket(42, snapshot);
        EyeStatePacket d1 = roundTrip(helper, EyeStatePacket::read, withOverrides, "EyeStatePacket with overrides");
        helper.assertTrue(d1.snapshot().equals(snapshot), "EyeStatePacket should preserve its snapshot");

        EyeStatePacket noOverrides = new EyeStatePacket(
                43, new EyeState.Snapshot(false, 0.0F, AppearanceOverride.EMPTY));
        roundTrip(helper, EyeStatePacket::read, noOverrides, "EyeStatePacket without overrides");
        helper.succeed();
    }

    /**
     * The client refuses an eye-state packet with a non-finite placement roll. No in-game form with an
     * unmodified server; guards clients against a malicious server.
     */
    public static void eyeStatePacketRejectsNonFiniteValues(GameTestHelper helper) {
        FriendlyByteBuf invalid = buffer(helper);
        invalid.writeVarInt(42);
        invalid.writeBoolean(true);
        invalid.writeFloat(Float.NaN);
        invalid.writeByte(0);
        helper.assertTrue(throwsRuntime(() -> EyeStatePacket.read(invalid)),
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
