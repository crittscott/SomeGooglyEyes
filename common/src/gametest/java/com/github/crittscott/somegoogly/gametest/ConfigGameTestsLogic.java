package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.EyeConfigReloadListener;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.config.TomlConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.HeadConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Checks on the datapack-loaded {@link ServerEyeConfigs} (the shipped configs are loaded by the
 * gametest server — the manual {@code somegoogly-test-datapack} is not mounted here), the reload
 * listener's exact Minecraft-version selection, and the pure {@link ServerConfig#percentFor} resolution.
 * Tests that mutate shared state (config values via their public {@code set}, the loaded eye configs
 * via {@code replaceAll}) restore the originals in a {@code finally} so later tests aren't affected.
 */
public final class ConfigGameTestsLogic {

    private ConfigGameTestsLogic() {
    }

    private static boolean usable(RuntimeConfig config) {
        return RuntimeConfig.isUsable(config);
    }

    public static void percentForResolvesExactBeforeWildcard(GameTestHelper helper) {
        int originalGlobal = ServerConfig.GLOBAL_PERCENT.get();
        List<String> originalOverrides = ServerConfig.ENTITY_OVERRIDES.get();
        try {
            ServerConfig.GLOBAL_PERCENT.set(2);
            // List an exact id and a broad wildcard; the exact id must win regardless of list order, and
            // an unmatched id must fall through to globalPercent.
            ServerConfig.ENTITY_OVERRIDES.set(List.of("minecraft:zombie,100", "minecraft:*,50"));

            int zombie = ServerConfig.percentFor(ResourceLocation.fromNamespaceAndPath("minecraft", "zombie"));
            int cow = ServerConfig.percentFor(ResourceLocation.fromNamespaceAndPath("minecraft", "cow"));
            int other = ServerConfig.percentFor(ResourceLocation.fromNamespaceAndPath("examplemod", "thing"));

            helper.assertTrue(zombie == 100, "exact override should win (expected 100, got " + zombie + ")");
            helper.assertTrue(cow == 50, "wildcard override should apply to cow (expected 50, got " + cow + ")");
            helper.assertTrue(other == 2, "unmatched id should fall back to globalPercent (expected 2, got " + other + ")");
        } finally {
            ServerConfig.GLOBAL_PERCENT.set(originalGlobal);
            ServerConfig.ENTITY_OVERRIDES.set(originalOverrides);
        }
        helper.succeed();
    }

    /**
     * {@code /sg spawnall} terraforms and mass-spawns with no undo, so its server-config gate must ship
     * opt-in. Guards the default; the packet-handler refusal itself needs a live player and stays
     * source-verified like the rest of picker behavior.
     */
    public static void spawnAllDefaultsOff(GameTestHelper helper) {
        helper.assertTrue(!ServerConfig.ALLOW_SPAWN_ALL.get(),
                "allowSpawnAll must default to false (spawnall is opt-in)");
        helper.succeed();
    }

    public static void shippedConfigsLoadForKnownEntities(GameTestHelper helper) {
        RuntimeConfig cow = ServerEyeConfigs.get(ResourceLocation.fromNamespaceAndPath("minecraft", "cow"), false);
        helper.assertTrue(usable(cow), "cow should have a usable shipped eye config");

        // Players have a definition (player.json) and so are configured — the basis for being an application target.
        RuntimeConfig player = ServerEyeConfigs.get(
                ResourceLocation.fromNamespaceAndPath("minecraft", "player"), false);
        helper.assertTrue(usable(player), "player should have a usable shipped eye config");
        helper.succeed();
    }

    /**
     * Every head in every shipped config must carry a non-blank attach token. The attachPoint is required
     * (no default), so a token that loads as {@code null}/empty means a corrupt data edit — this catches
     * that across all 80+ files in one cheap headless check. It does not (and can't, on the dedicated
     * server, where the client-only resolvers don't load) verify the token resolves to a real model part;
     * that needs an in-client pass.
     */
    public static void everyShippedConfigHasNonBlankAttachTokens(GameTestHelper helper) {
        for (Map.Entry<ResourceLocation, RuntimeConfigSet> entry : ServerEyeConfigs.all().entrySet()) {
            RuntimeConfigSet set = entry.getValue();
            assertTokens(helper, entry.getKey(), set.adult);
            assertTokens(helper, entry.getKey(), set.baby);
            assertTokens(helper, entry.getKey(), set.any);
        }
        helper.succeed();
    }

    private static void assertTokens(GameTestHelper helper, ResourceLocation id, RuntimeConfig config) {
        if (config == null) {
            return;
        }
        for (Variant variant : config.variants) {
            for (HeadConfig head : variant.heads) {
                helper.assertTrue(head.attachPoint != null && !head.attachPoint.isBlank(),
                        "config " + id + " has a head with a blank attach token");
            }
        }
    }

    /** Exposes the protected datapack {@code apply} so a test can feed synthetic files through real selection. */
    private static final class TestReloadListener extends EyeConfigReloadListener {
        void applyFiles(Map<ResourceLocation, JsonElement> files) {
            apply(files, null, null);
        }
    }

    /** Exact Minecraft-version selection, end to end, including paired adult/baby entries. */
    public static void exactMinecraftGenerationIsSelected(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("minecraft", "zombie");
        String json = """
                { "entries": [
                    { "version": "[1.20,1.21)", "age": "adult", "enabled": true, "variants": [
                        { "weight": 1.0, "heads": [ { "attachPoint": "head", "eyes": [ {
                            "position": [0.0, 0.0, 0.0], "eyeScale": 1.0, "irisScale": 1.0, "depth": 1.0,
                            "inclination": 90.0, "azimuth": 270.0, "crossTarget": -1,
                            "corneaColors": [1.0, 1.0, 1.0], "irisColors": [0.0, 0.0, 0.0], "glows": false
                        } ] } ] } ] },
                    { "version": "1.21.1", "age": "adult", "enabled": true, "variants": [
                        { "weight": 2.0, "heads": [ { "attachPoint": "head", "eyes": [ {
                            "position": [0.0, 0.0, 0.0], "eyeScale": 1.0, "irisScale": 1.0, "depth": 1.0,
                            "inclination": 90.0, "azimuth": 270.0, "crossTarget": -1,
                            "corneaColors": [1.0, 1.0, 1.0], "irisColors": [0.0, 0.0, 0.0], "glows": false
                        } ] } ] } ] },
                    { "version": "1.21.1", "age": "baby", "enabled": true, "variants": [
                        { "weight": 2.0, "heads": [ { "attachPoint": "head", "eyes": [ {
                            "position": [0.0, 0.0, 0.0], "eyeScale": 1.0, "irisScale": 1.0, "depth": 1.0,
                            "inclination": 90.0, "azimuth": 270.0, "crossTarget": -1,
                            "corneaColors": [1.0, 1.0, 1.0], "irisColors": [0.0, 0.0, 0.0], "glows": false
                        } ] } ] } ] }
                ] }
                """;
        Map<ResourceLocation, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            new TestReloadListener().applyFiles(Map.of(id, JsonParser.parseString(json)));
            RuntimeConfig adult = ServerEyeConfigs.get(id, false);
            RuntimeConfig baby = ServerEyeConfigs.get(id, true);
            helper.assertTrue(adult != null, "the exact Minecraft generation must be selected");
            helper.assertTrue(adult.variants.get(0).weight() == 2.0,
                    "exact selection must pick the 1.21.1 generation (weight 2), got "
                            + adult.variants.get(0).weight());
            helper.assertTrue(baby != null && baby.variants.get(0).weight() == 2.0,
                    "the baby entry of the exact generation must be selected with it");
        } finally {
            ServerEyeConfigs.replaceAll(original);
        }
        helper.succeed();
    }

    public static void shippedPigHasTwoVariants(GameTestHelper helper) {
        RuntimeConfig pig = ServerEyeConfigs.get(ResourceLocation.fromNamespaceAndPath("minecraft", "pig"), false);
        helper.assertTrue(usable(pig), "pig should have a usable shipped eye config");
        helper.assertTrue(pig.variants.size() == 2,
                "pig ships two placement variants (got " + pig.variants.size() + ")");
        Variant butt = pig.variants.get(1);
        helper.assertTrue(butt.heads.get(0).attachPoint.equals("body"),
                "the second variant is the low-weight butt-eyes placement");
        helper.assertTrue(butt.weight() < pig.variants.get(0).weight(),
                "the butt-eyes variant should be rarer than the head variant");
        helper.succeed();
    }

    /** {@code entityOverrides}: an exact id always wins; among wildcards, the first matching list entry wins. */
    public static void percentForResolvesWildcardsInListOrder(GameTestHelper helper) {
        int originalGlobal = ServerConfig.GLOBAL_PERCENT.get();
        List<String> originalOverrides = ServerConfig.ENTITY_OVERRIDES.get();
        try {
            ServerConfig.GLOBAL_PERCENT.set(2);
            ServerConfig.ENTITY_OVERRIDES.set(List.of("*:*_horse,50", "minecraft:*,10"));

            int skeletonHorse = ServerConfig.percentFor(
                    ResourceLocation.fromNamespaceAndPath("minecraft", "skeleton_horse"));
            int zombie = ServerConfig.percentFor(ResourceLocation.fromNamespaceAndPath("minecraft", "zombie"));
            int moddedHorse = ServerConfig.percentFor(
                    ResourceLocation.fromNamespaceAndPath("examplemod", "fancy_horse"));
            int other = ServerConfig.percentFor(ResourceLocation.fromNamespaceAndPath("examplemod", "thing"));

            helper.assertTrue(skeletonHorse == 50,
                    "the first matching wildcard wins even when a later one also matches (expected 50, got "
                            + skeletonHorse + ")");
            helper.assertTrue(zombie == 10,
                    "minecraft:* applies where the horse pattern does not (expected 10, got " + zombie + ")");
            helper.assertTrue(moddedHorse == 50,
                    "*:*_horse spans namespaces (expected 50, got " + moddedHorse + ")");
            helper.assertTrue(other == 2,
                    "an unmatched id falls back to globalPercent (expected 2, got " + other + ")");
        } finally {
            ServerConfig.GLOBAL_PERCENT.set(originalGlobal);
            ServerConfig.ENTITY_OVERRIDES.set(originalOverrides);
        }
        helper.succeed();
    }

    private static final String EYE_JSON = """
            { "position": [0.0, 0.0, 0.0], "eyeScale": 1.0, "irisScale": 1.0, "depth": 1.0,
              "inclination": 90.0, "azimuth": 270.0, "crossTarget": -1,
              "corneaColors": [1.0, 1.0, 1.0], "irisColors": [0.0, 0.0, 0.0], "glows": false }""";

    private static String entryJson(String version, String age, double weight) {
        return "{ \"version\": \"" + version + "\", \"age\": \"" + age + "\", \"enabled\": true, \"variants\": [ "
                + "{ \"weight\": " + weight + ", \"heads\": [ { \"attachPoint\": \"head\", \"eyes\": [ "
                + EYE_JSON + " ] } ] } ] }";
    }

    private static JsonElement fileJson(String... entries) {
        return JsonParser.parseString("{ \"entries\": [ " + String.join(", ", entries) + " ] }");
    }

    /** No datapack, from any namespace, may install an eye config for the ender dragon. */
    public static void reloadHardExcludesEnderDragon(GameTestHelper helper) {
        ResourceLocation zombie = ResourceLocation.fromNamespaceAndPath("minecraft", "zombie");
        Map<ResourceLocation, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            new TestReloadListener().applyFiles(Map.of(
                    ServerEyeConfigs.ENDER_DRAGON, fileJson(entryJson("1.21.1", "adult", 1.0)),
                    zombie, fileJson(entryJson("1.21.1", "adult", 1.0))));
            helper.assertTrue(ServerEyeConfigs.get(ServerEyeConfigs.ENDER_DRAGON, false) == null,
                    "the ender dragon config is refused at reload");
            helper.assertTrue(usable(ServerEyeConfigs.get(zombie, false)),
                    "other entities in the same batch still load");
        } finally {
            ServerEyeConfigs.replaceAll(original);
        }
        helper.succeed();
    }

    /** One malformed file, a duplicate age/version entry, and an invalid age string never abort the reload. */
    public static void reloadToleratesBadFilesAndDuplicates(GameTestHelper helper) {
        ResourceLocation zombie = ResourceLocation.fromNamespaceAndPath("minecraft", "zombie");
        ResourceLocation creeper = ResourceLocation.fromNamespaceAndPath("minecraft", "creeper");
        ResourceLocation skeleton = ResourceLocation.fromNamespaceAndPath("minecraft", "skeleton");
        Map<ResourceLocation, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            new TestReloadListener().applyFiles(Map.of(
                    zombie, fileJson(entryJson("1.21.1", "adult", 1.0), entryJson("1.21.1", "adult", 2.0)),
                    creeper, JsonParser.parseString("{ \"entries\": 5 }"),
                    skeleton, fileJson(entryJson("1.21.1", "elder", 1.0), entryJson("1.21.1", "adult", 3.0))));

            RuntimeConfig z = ServerEyeConfigs.get(zombie, false);
            helper.assertTrue(z != null && z.variants.get(0).weight() == 1.0,
                    "a duplicate age/version entry keeps the first");
            helper.assertTrue(ServerEyeConfigs.get(creeper, false) == null,
                    "a malformed file is skipped without aborting the batch");
            RuntimeConfig s = ServerEyeConfigs.get(skeleton, false);
            helper.assertTrue(s != null && s.variants.get(0).weight() == 3.0,
                    "an invalid age is ignored while its file's valid entries still load");
        } finally {
            ServerEyeConfigs.replaceAll(original);
        }
        helper.succeed();
    }

    /** An entry with no range covering the loaded version degrades to its nearest generation, not to nothing. */
    public static void reloadFallsBackToNearestGeneration(GameTestHelper helper) {
        ResourceLocation zombie = ResourceLocation.fromNamespaceAndPath("minecraft", "zombie");
        Map<ResourceLocation, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            new TestReloadListener().applyFiles(Map.of(
                    zombie, fileJson(entryJson("[1.16,1.17)", "adult", 1.0))));
            helper.assertTrue(usable(ServerEyeConfigs.get(zombie, false)),
                    "an entry that matches no loaded version falls back to its nearest generation");
        } finally {
            ServerEyeConfigs.replaceAll(original);
        }
        helper.succeed();
    }

    /** The eye-config generation bumps only when the resolved content changes; server stop forces the next resync. */
    public static void reloadBumpsGenerationOnlyOnContentChange(GameTestHelper helper) {
        ResourceLocation zombie = ResourceLocation.fromNamespaceAndPath("minecraft", "zombie");
        Map<ResourceLocation, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            Map<ResourceLocation, JsonElement> files = Map.of(
                    zombie, fileJson(entryJson("1.21.1", "adult", 1.0)));

            new TestReloadListener().applyFiles(files);
            long afterFirst = ServerEyeConfigs.generation();

            new TestReloadListener().applyFiles(files);
            helper.assertTrue(ServerEyeConfigs.generation() == afterFirst,
                    "an identical reload does not bump the generation");

            ServerEyeConfigs.onServerStopping();
            new TestReloadListener().applyFiles(files);
            helper.assertTrue(ServerEyeConfigs.generation() == afterFirst + 1,
                    "clearing the content signature on server stop forces the next reload to resync");
        } finally {
            ServerEyeConfigs.replaceAll(original);
        }
        helper.succeed();
    }

    /** {@link TomlConfig} writes defaults for an absent file, then re-reads an existing file without overwriting it. */
    public static void serverTomlRoundTrips(GameTestHelper helper) {
        String defaults = """
                [Server Settings]
                googlyEyesEnabled = false
                globalPercent = 17
                entityOverrides = ["minecraft:zombie,100", "*:*_horse,50"]
                """;
        try {
            Path dir = Files.createTempDirectory("somegoogly-toml-test");
            Path file = dir.resolve("server.toml");
            try {
                Map<String, Object> written = TomlConfig.readOrCreate(file, defaults);
                helper.assertTrue(Files.exists(file), "readOrCreate writes the defaults when the file is absent");
                helper.assertTrue(!TomlConfig.bool(written, "googlyEyesEnabled", true), "a boolean round-trips");
                helper.assertTrue(TomlConfig.integer(written, "globalPercent", 5) == 17, "an integer round-trips");
                helper.assertTrue(
                        TomlConfig.strings(written, "entityOverrides", List.of())
                                .equals(List.of("minecraft:zombie,100", "*:*_horse,50")),
                        "a quoted string list round-trips with colons and wildcards intact");

                Files.writeString(file, """
                        [Server Settings]
                        googlyEyesEnabled = true
                        globalPercent = 3
                        entityOverrides = []
                        """);
                Map<String, Object> reread = TomlConfig.readOrCreate(file, defaults);
                helper.assertTrue(TomlConfig.bool(reread, "googlyEyesEnabled", false),
                        "an existing file is re-read, not overwritten by the defaults");
                helper.assertTrue(TomlConfig.integer(reread, "globalPercent", 5) == 3,
                        "the re-read picks up the edited value");
                helper.assertTrue(TomlConfig.strings(reread, "entityOverrides", List.of("x")).isEmpty(),
                        "an empty list parses as empty");
            } finally {
                Files.deleteIfExists(file);
                Files.deleteIfExists(dir);
            }
        } catch (IOException e) {
            throw new RuntimeException("TOML round-trip raised an IOException", e);
        }
        helper.succeed();
    }
}
