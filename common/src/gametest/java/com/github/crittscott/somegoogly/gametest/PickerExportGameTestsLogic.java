package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.EyeConfigLimits;
import com.github.crittscott.somegoogly.config.VersionRangeMatcher;
import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.config.EyeConfigModel.ConfigFile;
import com.github.crittscott.somegoogly.config.EyeConfigModel.HeadConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import com.github.crittscott.somegoogly.eye.state.EyeAppearance;
import com.github.crittscott.somegoogly.network.PickerExportPacket;
import com.github.crittscott.somegoogly.picker.PickerExportService;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_ADULT;

/**
 * The server-side export validation ({@link PickerExportService}) and the canonical JSON it emits.
 * Only the <b>rejection</b> paths of the service are exercised — they return before any file is
 * written, so nothing touches the test world's datapacks and no {@code /reload} fires mid-run (the
 * success path, and with it the 10-second cooldown, is covered by manual verification against a real
 * server). Each test uses a fresh player id so the all-attempt throttle remains independent.
 */
public final class PickerExportGameTestsLogic {

    private PickerExportGameTestsLogic() {
    }

    private static MinecraftServer server(GameTestHelper helper) {
        return helper.getLevel().getServer();
    }

    /**
     * Export refuses an entity type the server does not know. No in-game form with an unmodified client, which
     * exports only mobs that exist; guards the server against a forged export packet.
     */
    public static void exportRejectsUnknownEntityType(GameTestHelper helper) {
        ResourceLocation type = ResourceLocation.fromNamespaceAndPath("somegoogly", "not_a_real_mob");
        Component result = PickerExportService.export(
                server(helper), UUID.randomUUID(), type, AGE_ADULT, new CompoundTag());
        helper.assertTrue(result.equals(Component.translatable(
                        "somegoogly.command.picker.export_rejected_unknown_type", type.toString())),
                "an id absent from the entity registry must be rejected, got: " + result.getString());
        helper.succeed();
    }

    /**
     * Export refuses the ender dragon. In game: as a creative operator, summon an ender dragon, choose it in the
     * picker with {@code V}, create an eye with {@code /sg create 0 0 0}, and run {@code /sg export}; the export
     * is refused with a message and nothing is written.
     */
    public static void exportRejectsEnderDragon(GameTestHelper helper) {
        Component result = PickerExportService.export(server(helper), UUID.randomUUID(),
                ResourceLocation.fromNamespaceAndPath("minecraft", "ender_dragon"), AGE_ADULT, new CompoundTag());
        helper.assertTrue(result.equals(Component.translatable(
                        "somegoogly.command.picker.export_rejected_ender_dragon")),
                "the ender dragon must be refused at export, got: " + result.getString());
        helper.succeed();
    }

    /**
     * The client's pre-send size check. In game: author a mob with a very large number of eyes in the
     * picker and run {@code /sg export}; the client answers "too large to send" instead of the server
     * disconnecting you, while an ordinary config exports normally.
     */
    public static void exportPacketRefusesOversizedConfig(GameTestHelper helper) {
        ResourceLocation cow = ResourceLocation.fromNamespaceAndPath("minecraft", "cow");
        CompoundTag small = new CompoundTag();
        small.putBoolean("enabled", true);
        helper.assertTrue(new PickerExportPacket(cow, AGE_ADULT, small).fitsServerbound(),
                "an ordinary config must fit in a serverbound payload");

        CompoundTag large = new CompoundTag();
        large.putString("padding", "x".repeat(PickerExportPacket.MAX_SERVERBOUND_BYTES));
        helper.assertTrue(!new PickerExportPacket(cow, AGE_ADULT, large).fitsServerbound(),
                "a config larger than vanilla's serverbound payload cap must be refused before sending");
        helper.succeed();
    }

    /**
     * {@code RuntimeConfig.CODEC}'s fields are required, so a wrong-typed field is a decode failure
     * rather than something DFU swallows into a default, and the service refuses the payload as
     * malformed. No in-game form with an unmodified client; guards the server against a forged export
     * packet.
     */
    public static void exportRejectsGarbageTypedField(GameTestHelper helper) {
        CompoundTag garbage = new CompoundTag();
        garbage.putBoolean("enabled", true);
        garbage.putString("variants", "not a list");
        Component result = PickerExportService.export(server(helper), UUID.randomUUID(),
                ResourceLocation.fromNamespaceAndPath("minecraft", "cow"), AGE_ADULT, garbage);
        helper.assertTrue(result.equals(Component.translatable(
                        "somegoogly.command.picker.export_rejected_malformed_payload")),
                "a garbage-typed variants field must be refused as malformed, got: " + result.getString());
        helper.succeed();
    }

    /**
     * An empty compound is missing required fields, so it never reaches the usable-eyes check. No in-game
     * form with an unmodified client; guards the server against a forged export packet.
     */
    public static void exportRejectsEmptyPayloadAsMalformed(GameTestHelper helper) {
        Component result = PickerExportService.export(server(helper), UUID.randomUUID(),
                ResourceLocation.fromNamespaceAndPath("minecraft", "cow"), AGE_ADULT, new CompoundTag());
        helper.assertTrue(result.equals(Component.translatable(
                        "somegoogly.command.picker.export_rejected_malformed_payload")),
                "a payload with no fields must be refused as malformed, got: " + result.getString());
        helper.succeed();
    }

    /**
     * A well-formed config that simply carries no eyes is refused by the usable-eyes check. In game:
     * choose a mob in the picker, delete every eye with {@code /sg delete}, and run {@code /sg export}; the
     * export is refused because there are no eyes to draw.
     */
    public static void exportRejectsConfigWithNoUsableEyes(GameTestHelper helper) {
        Tag empty = RuntimeConfig.CODEC.encodeStart(NbtOps.INSTANCE, new RuntimeConfig())
                .result().orElseThrow();
        Component result = PickerExportService.export(server(helper), UUID.randomUUID(),
                ResourceLocation.fromNamespaceAndPath("minecraft", "cow"), AGE_ADULT, (CompoundTag) empty);
        helper.assertTrue(result.equals(Component.translatable(
                        "somegoogly.command.picker.export_rejected_no_usable_eyes")),
                "a config with nothing to draw must be rejected, got: " + result.getString());
        helper.succeed();
    }

    /**
     * Export refuses geometry outside the validator's limits, such as a non-finite position. No in-game form,
     * since picker commands accept only finite numbers; guards the server against a forged export packet.
     */
    public static void exportRejectsUnsafeNumericConfig(GameTestHelper helper) {
        HeadConfig head = new HeadConfig();
        head.attachPoint = "head";
        head.eyes = List.of(new EyeDefinition(
                new EyePlacement(new Vec3(Double.NaN, 0.0, 0.0), 1.0F, 1.0F, 1.0F,
                        0.0F, 0.0F, EyePlacement.NO_CROSS_TARGET), EyeAppearance.DEFAULT));
        Variant variant = new Variant();
        variant.heads = List.of(head);
        RuntimeConfig config = new RuntimeConfig();
        config.variants = List.of(variant);
        String error = EyeConfigLimits.validateRuntimeConfig(config);
        Tag encoded = RuntimeConfig.CODEC.encodeStart(NbtOps.INSTANCE, config).result().orElseThrow();

        Component result = PickerExportService.export(server(helper), UUID.randomUUID(),
                ResourceLocation.fromNamespaceAndPath("minecraft", "cow"), AGE_ADULT, (CompoundTag) encoded);
        helper.assertTrue(result.equals(Component.translatable(
                        "somegoogly.command.picker.export_rejected_unsafe_payload", error)),
                "non-finite exported geometry must be rejected, got: " + result.getString());
        helper.succeed();
    }

    /**
     * Exports declare another mod's definitions for that mod's current minor release. In game: with an
     * optional mod installed (say version 4.12.4), run {@code /sg exportall}; that mod's files under
     * {@code somegoogly-export} declare {@code "version": "[4.12.4,4.13)"}.
     */
    public static void optionalModVersionRangeSynthesis(GameTestHelper helper) {
        helper.assertTrue("[4.12.4,4.13)".equals(VersionRangeMatcher.rangeFor("4.12.4")),
                "an optional-mod version becomes an inclusive-to-next-minor range");
        helper.assertTrue("banana".equals(VersionRangeMatcher.rangeFor("banana")),
                "an unparseable version falls back to an exact-match entry");
        helper.succeed();
    }

    /**
     * The written form pins values that equal their defaults rather than eliding them, so a file's
     * meaning can't drift with the code. {@code crossTarget} sits at its default in most real configs,
     * so it's the field checked by name here; that no field is dropped at all is guarded by the value
     * round-trip in {@code SerializationGameTests}. In game: run {@code /sg exportall} and open any
     * exported JSON; every eye lists {@code "crossTarget"}, including {@code -1}.
     */
    public static void canonicalJsonWritesDefaultValuedFields(GameTestHelper helper) {
        HeadConfig head = new HeadConfig();
        head.attachPoint = "head";
        head.eyes = List.of(EyeDefinition.DEFAULT);
        Variant variant = new Variant();
        variant.heads = List.of(head);
        RuntimeConfig config = new RuntimeConfig();
        config.variants = List.of(variant);

        ConfigFile file = ConfigFile.single("1.21.1", "any", config);
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
