package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.config.EyeConfigJsonWriter;
import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.eye.HeadInfo.HeadConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.Variant;
import com.github.crittscott.somegoogly.picker.PickerExportService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * The server-side export validation ({@link PickerExportService}) and the canonical JSON writer
 * ({@link EyeConfigJsonWriter}) it emits through. Only the <b>rejection</b> paths of the service are
 * exercised — they return before any file is written, so nothing touches the test world's datapacks
 * and no {@code /reload} fires mid-run (the success path, and with it the 10-second cooldown, is
 * covered by manual verification against a real server). Failed requests deliberately don't arm the
 * cooldown, which is also what keeps these tests independent.
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

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void exportRejectsMalformedPayload(GameTestHelper helper) {
        CompoundTag garbage = new CompoundTag();
        garbage.putString("variants", "not a list");
        String result = PickerExportService.export(server(helper), UUID.randomUUID(),
                new ResourceLocation("minecraft", "cow"), garbage);
        helper.assertTrue(result.contains("malformed"),
                "a payload the codec can't parse must be rejected, got: " + result);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void exportRejectsConfigWithNoUsableEyes(GameTestHelper helper) {
        // An empty compound parses (all fields optional) but yields zero usable variants.
        String result = PickerExportService.export(server(helper), UUID.randomUUID(),
                new ResourceLocation("minecraft", "cow"), new CompoundTag());
        helper.assertTrue(result.contains("no usable eyes"),
                "a config with nothing to draw must be rejected, got: " + result);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void versionRangeSynthesis(GameTestHelper helper) {
        helper.assertTrue("[1.20.1,1.21)".equals(EyeConfigJsonWriter.versionRange("1.20.1")),
                "a numeric version becomes an inclusive-to-next-minor range");
        helper.assertTrue("banana".equals(EyeConfigJsonWriter.versionRange("banana")),
                "an unparseable version falls back to an exact-match entry");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void canonicalEyeJsonWritesEveryField(GameTestHelper helper) {
        HeadConfig head = new HeadConfig();
        head.attachPoint = "head";
        head.eyes = List.of(EyeDefinition.DEFAULT);
        Variant variant = new Variant();
        variant.weight = 1.0;
        variant.heads = List.of(head);

        JsonArray variants = EyeConfigJsonWriter.variantsJson(List.of(variant), UnaryOperator.identity());
        helper.assertTrue(variants.size() == 1, "one usable variant should serialize");
        JsonObject eye = variants.get(0).getAsJsonObject()
                .getAsJsonArray("heads").get(0).getAsJsonObject()
                .getAsJsonArray("eyes").get(0).getAsJsonObject();

        // The canonical form pins every value explicitly so the files never rely on code defaults.
        for (String field : List.of("position", "eyeScale", "irisScale", "inclination", "azimuth",
                "corneaColors", "irisColors", "glows", "affectedByInvisibility")) {
            helper.assertTrue(eye.has(field), "canonical eye JSON must write '" + field + "'");
        }
        helper.assertTrue(!eye.has("crossTarget"),
                "crossTarget is a within-head index and is only written when set (default -1)");
        helper.succeed();
    }
}
