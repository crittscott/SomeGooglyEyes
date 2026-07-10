package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.config.VersionRangeMatcher;
import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.eye.HeadInfo.ConfigFile;
import com.github.crittscott.somegoogly.eye.HeadInfo.HeadConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.Variant;
import com.github.crittscott.somegoogly.picker.PickerExportService;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/**
 * The server-side export validation ({@link PickerExportService}) and the canonical JSON it emits.
 * Only the <b>rejection</b> paths of the service are exercised — they return before any file is
 * written, so nothing touches the test world's datapacks and no {@code /reload} fires mid-run (the
 * success path, and with it the 10-second cooldown, is covered by manual verification against a real
 * server). Failed requests deliberately don't arm the cooldown, which is also what keeps these tests
 * independent.
 */
@GameTestHolder(SomeGoogly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PickerExportGameTests {

    private static final String TEMPLATE = "empty";

    private PickerExportGameTests() {
    }

    private static MinecraftServer server(GameTestHelper helper) {
        return helper.getLevel().getServer();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void exportRejectsUnknownEntityType(GameTestHelper helper) {
        String result = PickerExportService.export(server(helper), UUID.randomUUID(),
                new ResourceLocation("somegoogly", "not_a_real_mob"), new CompoundTag());
        helper.assertTrue(result.contains("unknown entity type"),
                "an id absent from the entity registry must be rejected, got: " + result);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void exportRejectsEnderDragon(GameTestHelper helper) {
        String result = PickerExportService.export(server(helper), UUID.randomUUID(),
                new ResourceLocation("minecraft", "ender_dragon"), new CompoundTag());
        helper.assertTrue(result.contains("hard-excluded"),
                "the ender dragon must be refused at export, got: " + result);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void exportRejectsMissingPayload(GameTestHelper helper) {
        // A null tag is what an over-quota payload decodes to (PickerExportPacket).
        String result = PickerExportService.export(server(helper), UUID.randomUUID(),
                new ResourceLocation("minecraft", "cow"), null);
        helper.assertTrue(result.contains("missing or oversized"),
                "a null/oversized payload must be rejected, got: " + result);
        helper.succeed();
    }

    /**
     * {@code RuntimeConfig.CODEC}'s fields are required, so a wrong-typed field is a decode failure
     * rather than something DFU swallows into a default. That's what gives the service's
     * malformed-payload rejection something to catch — with optional fields this payload decoded
     * "successfully" as an empty config and only the no-usable-eyes check refused it.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void exportRejectsGarbageTypedField(GameTestHelper helper) {
        CompoundTag garbage = new CompoundTag();
        garbage.putBoolean("enabled", true);
        garbage.putString("variants", "not a list");
        String result = PickerExportService.export(server(helper), UUID.randomUUID(),
                new ResourceLocation("minecraft", "cow"), garbage);
        helper.assertTrue(result.contains("malformed"),
                "a garbage-typed variants field must be refused as malformed, got: " + result);
        helper.succeed();
    }

    /** An empty compound is missing required fields, so it never reaches the usable-eyes check. */
    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void exportRejectsEmptyPayloadAsMalformed(GameTestHelper helper) {
        String result = PickerExportService.export(server(helper), UUID.randomUUID(),
                new ResourceLocation("minecraft", "cow"), new CompoundTag());
        helper.assertTrue(result.contains("malformed"),
                "a payload with no fields must be refused as malformed, got: " + result);
        helper.succeed();
    }

    /** A well-formed config that simply carries no eyes is refused by the usable-eyes check. */
    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void exportRejectsConfigWithNoUsableEyes(GameTestHelper helper) {
        Tag empty = RuntimeConfig.CODEC.encodeStart(NbtOps.INSTANCE, new RuntimeConfig())
                .result().orElseThrow();
        String result = PickerExportService.export(server(helper), UUID.randomUUID(),
                new ResourceLocation("minecraft", "cow"), (CompoundTag) empty);
        helper.assertTrue(result.contains("no usable eyes"),
                "a config with nothing to draw must be rejected, got: " + result);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void versionRangeSynthesis(GameTestHelper helper) {
        helper.assertTrue("[1.20.1,1.21)".equals(VersionRangeMatcher.rangeFor("1.20.1")),
                "a numeric version becomes an inclusive-to-next-minor range");
        helper.assertTrue("banana".equals(VersionRangeMatcher.rangeFor("banana")),
                "an unparseable version falls back to an exact-match entry");
        helper.succeed();
    }

    /**
     * The written form pins values that equal their defaults rather than eliding them, so a file's
     * meaning can't drift with the code. {@code crossTarget} is the one field the old hand-rolled
     * writer suppressed at its default, so it's the one worth naming here; that no field is dropped at
     * all is guarded by the value round-trip in {@code SerializationGameTests}.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void canonicalJsonWritesDefaultValuedFields(GameTestHelper helper) {
        HeadConfig head = new HeadConfig();
        head.attachPoint = "head";
        head.eyes = List.of(EyeDefinition.DEFAULT);
        Variant variant = new Variant();
        variant.heads = List.of(head);
        RuntimeConfig config = new RuntimeConfig();
        config.variants = List.of(variant);

        ConfigFile file = ConfigFile.single("[1.20.1,1.21)", "any", config);
        JsonObject json = ConfigFile.CODEC.encodeStart(JsonOps.INSTANCE, file)
                .result().orElseThrow().getAsJsonObject();
        JsonObject eye = json.getAsJsonArray("entries").get(0).getAsJsonObject()
                .getAsJsonArray("variants").get(0).getAsJsonObject()
                .getAsJsonArray("heads").get(0).getAsJsonObject()
                .getAsJsonArray("eyes").get(0).getAsJsonObject();

        helper.assertTrue(eye.has("crossTarget") && eye.get("crossTarget").getAsInt() == -1,
                "crossTarget must be written even at its no-partner default (-1)");
        helper.succeed();
    }
}
