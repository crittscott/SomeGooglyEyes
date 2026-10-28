package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.client.picker.EyeDraft;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.config.EyeConfigLimits;
import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.config.EyeConfigModel.ConfigFile;
import com.github.crittscott.somegoogly.config.EyeConfigModel.HeadConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.config.EyeConfigModel.VersionedEntry;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import com.github.crittscott.somegoogly.eye.state.EyeAppearance;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.UUID;

import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_ADULT;
import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_ANY;
import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_BABY;

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

    private static void assertAge(GameTestHelper helper, ConfigFile file, String age, RuntimeConfig expected) {
        VersionedEntry actual = file.entries.stream().filter(entry -> entry.age.equals(age)).findFirst().orElseThrow();
        helper.assertTrue(VersionedEntry.CODEC.encodeStart(JsonOps.INSTANCE, actual).getOrThrow().equals(
                        VersionedEntry.CODEC.encodeStart(JsonOps.INSTANCE,
                                VersionedEntry.of("1.21.4", age, expected)).getOrThrow()),
                "export must preserve the complete " + age + " arrangement");
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

    private static List<PickerState.DraftVariant> draft(float x) {
        PickerState.DraftVariant variant = new PickerState.DraftVariant();
        EyeDraft eye = new EyeDraft();
        eye.position[0] = x;
        variant.eyes.add(new PickerState.ListedEye("head", eye));
        return new ArrayList<>(List.of(variant));
    }

    /**
     * Drafts retain independent edits for each species and life stage. In game: choose an adult cow,
     * edit and save an eye, then do the same to a baby cow and a sheep. Choose each again; each keeps
     * its own placement, and export-all writes the cow's adult and baby arrangements separately.
     */
    public static void draftsRetainSpeciesAndAge(GameTestHelper helper) {
        PickerState.DraftSession session = new PickerState.DraftSession();
        ResourceLocation cow = ResourceLocation.withDefaultNamespace("cow");
        ResourceLocation sheep = ResourceLocation.withDefaultNamespace("sheep");
        PickerState.Draft adult = session.select(cow, false, () -> draft(1F));
        adult.variants().getFirst().eyes.getFirst().eye.position[0] = 4F;
        PickerState.Draft baby = session.select(cow, true, () -> draft(2F));
        baby.variants().getFirst().eyes.getFirst().eye.position[0] = 5F;
        session.select(sheep, false, () -> draft(3F));
        helper.assertTrue(session.select(cow, false, () -> draft(99F)) == adult,
                "returning to an adult reuses only its adult draft");
        helper.assertTrue(session.current().key().age().equals(AGE_ADULT), "adult draft keeps its age");
        helper.assertTrue(session.select(cow, true, () -> draft(99F)) == baby,
                "returning to a baby reuses only its baby draft");
        helper.assertTrue(session.current().key().age().equals(AGE_BABY), "baby draft keeps its age");
        Map<ResourceLocation, Map<String, RuntimeConfig>> exports = session.exports();
        helper.assertTrue(exports.size() == 2 && exports.get(cow).size() == 2,
                "export retains both cow ages and the other species");
        helper.assertTrue(exports.get(cow).get(AGE_ADULT).variants.getFirst().heads.getFirst()
                        .eyes.getFirst().placement().position().x == 4F
                        && exports.get(cow).get(AGE_BABY).variants.getFirst().heads.getFirst()
                        .eyes.getFirst().placement().position().x == 5F,
                "each exported age keeps its own edits");
        session.clear();
        helper.assertTrue(session.current() == null && session.exports().isEmpty(),
                "clearing a session removes both selection and retained drafts");
        helper.succeed();
    }

    private static RuntimeConfig exportConfig(float x, double weight, boolean enabled) {
        HeadConfig head = new HeadConfig();
        head.attachPoint = "head";
        head.eyes = List.of(new EyeDefinition(new EyePlacement(new Vec3(x, 0, 0), 1F, 1F, 1F,
                        0F, 0F, EyePlacement.NO_CROSS_TARGET),
                new EyeAppearance(EyeColor.WHITE, new EyeColor(0.25F, 0.5F, 0.75F), true)));
        Variant variant = new Variant();
        variant.heads = List.of(head);
        variant.weight = weight;
        RuntimeConfig config = new RuntimeConfig();
        config.enabled = enabled;
        config.variants = List.of(variant);
        return config;
    }

    /**
     * Export-all can replace both ages and preserve an empty disabled bucket; a newly configured
     * species needs no prior definition. In game: author both ages, export-all, and inspect both
     * entries; repeat for a previously unconfigured species. An existing disabled any entry remains.
     */
    public static void exportOverlayCombinesDraftAges(GameTestHelper helper) {
        RuntimeConfigSet base = new RuntimeConfigSet();
        base.adult = exportConfig(1F, 1, true);
        base.any = new RuntimeConfig();
        base.any.enabled = false;
        RuntimeConfig adult = exportConfig(4F, 2, true);
        RuntimeConfig baby = exportConfig(5F, 3, true);
        Map<String, RuntimeConfig> drafts = Map.of(AGE_ADULT, adult, AGE_BABY, baby);
        ConfigFile file = roundTripFile(ConfigFile.overlay(base, drafts, "1.21.4", token -> "root/" + token));
        helper.assertTrue(file.entries.size() == 3, "bulk export retains an empty disabled bucket");
        assertAge(helper, file, AGE_ADULT, adult);
        assertAge(helper, file, AGE_BABY, baby);
        assertAge(helper, file, AGE_ANY, base.any);
        ConfigFile fresh = roundTripFile(ConfigFile.overlay(null, drafts, "1.21.4", UnaryOperator.identity()));
        helper.assertTrue(fresh.entries.size() == 2, "new definitions contain both authored ages");
        assertAge(helper, fresh, AGE_ADULT, adult);
        assertAge(helper, fresh, AGE_BABY, baby);
        ConfigFile canonical = roundTripFile(ConfigFile.overlay(base, Map.of(AGE_BABY, baby),
                "1.21.4", token -> "root/" + token));
        helper.assertTrue(canonical.entries.stream().filter(entry -> entry.age.equals(AGE_ADULT)).findFirst()
                        .orElseThrow().variants.getFirst().heads.getFirst().attachPoint.equals("root/head"),
                "copied base tokens are canonicalized while authored tokens stay unchanged");
        helper.succeed();
    }

    /**
     * Both export paths preserve untouched life stages. In game: use definitions with distinct adult,
     * baby, and age-independent entries, edit only the adult, then use export and export-all. Both
     * written files retain the baby's placement and the fallback, including a disabled fallback.
     */
    public static void exportOverlayPreservesOtherAges(GameTestHelper helper) {
        RuntimeConfigSet base = new RuntimeConfigSet();
        base.adult = exportConfig(1F, 2, true);
        base.baby = exportConfig(2F, 3, true);
        base.any = exportConfig(3F, 4, false);
        RuntimeConfig replacement = exportConfig(4F, 5, true);
        for (String age : List.of(AGE_ADULT, AGE_BABY)) {
            ConfigFile file = roundTripFile(ConfigFile.overlay(base, Map.of(age, replacement),
                    "1.21.4", UnaryOperator.identity()));
            helper.assertTrue(file.entries.size() == 3, "single-age export retains every age bucket");
            assertAge(helper, file, AGE_ADULT, age.equals(AGE_ADULT) ? replacement : base.adult);
            assertAge(helper, file, AGE_BABY, age.equals(AGE_BABY) ? replacement : base.baby);
            assertAge(helper, file, AGE_ANY, base.any);
        }
        helper.assertTrue(base.adult.variants.getFirst().heads.getFirst().eyes.getFirst().placement()
                        .position().x == 1F && base.baby.enabled && !base.any.enabled,
                "overlay must not mutate the installed source set");
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
     * Export refuses the ender dragon. No in-game form with an unmodified client, which cannot choose the
     * dragon in the picker; guards the server against a forged export packet.
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
        EyeConfigLimits.Issue error = EyeConfigLimits.validateRuntimeConfig(config);
        helper.assertTrue(error.message().equals(Component.translatable("somegoogly.validation.eye_context",
                        0, 0, 0, Component.translatable("somegoogly.validation.position"))),
                "the position reason and all placement context must remain translatable");
        helper.assertTrue(error.diagnostic().equals(
                        "variant 0 head 0 eye 0: position is not finite or is out of range"),
                "logs keep a plain diagnostic independent of the client language");
        Tag encoded = RuntimeConfig.CODEC.encodeStart(NbtOps.INSTANCE, config).result().orElseThrow();

        Component result = PickerExportService.export(server(helper), UUID.randomUUID(),
                ResourceLocation.fromNamespaceAndPath("minecraft", "cow"), AGE_ADULT, (CompoundTag) encoded);
        helper.assertTrue(result.equals(Component.translatable(
                        "somegoogly.command.picker.export_rejected_unsafe_payload", error.message())),
                "non-finite exported geometry must be rejected, got: " + result.getString());
        helper.succeed();
    }

    private static ConfigFile roundTripFile(ConfigFile file) {
        return ConfigFile.CODEC.parse(JsonOps.INSTANCE,
                ConfigFile.CODEC.encodeStart(JsonOps.INSTANCE, file).getOrThrow()).getOrThrow();
    }

    private static MinecraftServer server(GameTestHelper helper) {
        return helper.getLevel().getServer();
    }

}
