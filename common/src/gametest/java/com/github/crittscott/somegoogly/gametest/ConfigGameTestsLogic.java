package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.EyeConfigReloadListener;
import com.github.crittscott.somegoogly.config.ModVersionLookup;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

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

    /**
     * An exact {@code entityOverrides} id wins over a wildcard, and an unmatched id uses {@code globalPercent}.
     * In game: set {@code globalPercent = 2} and
     * {@code entityOverrides = ["minecraft:zombie,100", "minecraft:*,50"]} in the server config, then spawn
     * mobs from eggs: every zombie has eyes, about half of the cows do, and only the occasional modded mob.
     */
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
     * opt-in. MineColonies and Create entities require spawn context this command does not provide, so
     * those namespaces must ship excluded. In game: in a new world, the generated
     * {@code serverconfig/somegoogly-server.toml} has {@code allowSpawnAll = false} and
     * {@code spawnExcludedMods = ["minecolonies", "create"]}, and {@code /sg spawnall} refuses to run.
     */
    public static void spawnAllDefaultsOff(GameTestHelper helper) {
        helper.assertTrue(!ServerConfig.ALLOW_SPAWN_ALL.get(),
                "allowSpawnAll must default to false (spawnall is opt-in)");
        helper.assertTrue(ServerConfig.SPAWN_EXCLUDED_MODS.get().equals(List.of("minecolonies", "create")),
                "spawnExcludedMods must default to minecolonies and create");
        helper.succeed();
    }

    /**
     * Every bundled Minecraft eye definition loads and is usable for each age it declares. In game: set
     * {@code globalPercent = 100}, spawn one of each mob that has a file under {@code data/minecraft/eyes},
     * and every one of them has eyes; the server log reports no invalid or unsafe eye config.
     */
    public static void everyShippedMinecraftDefinitionLoads(GameTestHelper helper) {
        Map<ResourceLocation, Resource> files = helper.getLevel().getServer().getResourceManager()
                .listResources("eyes", path -> path.getPath().endsWith(".json"));
        int checked = 0;
        for (ResourceLocation file : files.keySet()) {
            ResourceLocation id = EyeConfigReloadListener.EYE_FILES.fileToId(file);
            if (!ResourceLocation.DEFAULT_NAMESPACE.equals(id.getNamespace())) {
                continue;
            }
            RuntimeConfigSet set = ServerEyeConfigs.all().get(id);
            helper.assertTrue(set != null, "shipped eye definition " + id + " failed to load");
            for (RuntimeConfig config : new RuntimeConfig[] {set.adult, set.baby, set.any}) {
                helper.assertTrue(config == null || usable(config),
                        "shipped eye definition " + id + " has an unusable entry");
            }
            checked++;
        }
        helper.assertTrue(checked > 0, "no shipped Minecraft eye definitions were found");
        helper.succeed();
    }

    /** Exposes the protected datapack {@code apply} so a test can feed synthetic files through real selection. */
    private static final class TestReloadListener extends EyeConfigReloadListener {
        void applyFiles(Map<ResourceLocation, JsonElement> files) {
            apply(files, null, null);
        }
    }

    /**
     * Minecraft-version selection, end to end, including paired adult/baby entries. In game: add a
     * datapack with {@code data/minecraft/eyes/zombie.json} holding an adult entry for
     * {@code "[1.20,1.21)"} with blue irises and adult and baby entries for the running version with red
     * irises, run {@code /reload}, and set {@code entityOverrides = ["minecraft:zombie,100"]}; adult and
     * baby zombies spawn with red irises.
     */
    public static void exactMinecraftGenerationIsSelected(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("minecraft", "zombie");
        JsonElement json = fileJson(
                entryJson("[1.20,1.21)", "adult", 1.0),
                entryJson(running(), "adult", 2.0),
                entryJson(running(), "baby", 2.0),
                entryJson("[99.0,100.0)", "adult", 3.0));
        Map<ResourceLocation, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            new TestReloadListener().applyFiles(Map.of(id, json));
            RuntimeConfig adult = ServerEyeConfigs.get(id, false);
            RuntimeConfig baby = ServerEyeConfigs.get(id, true);
            helper.assertTrue(adult != null, "the exact Minecraft generation must be selected");
            helper.assertTrue(adult.variants.get(0).weight == 2.0,
                    "exact selection must pick the running version's entry (weight 2), got "
                            + adult.variants.get(0).weight);
            helper.assertTrue(baby != null && baby.variants.get(0).weight == 2.0,
                    "the baby entry of the exact generation must be selected with it");
        } finally {
            ServerEyeConfigs.replaceAll(original);
        }
        helper.succeed();
    }

    /**
     * {@code entityOverrides}: an exact id always wins; among wildcards, the first matching list entry
     * wins. In game: set {@code globalPercent = 2} and
     * {@code entityOverrides = ["*:*_horse,50", "minecraft:*,10"]}, then spawn mobs from eggs: about half
     * of the skeleton horses have eyes, about one zombie in ten, and only the occasional modded mob.
     */
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

    /** The running Minecraft version, so synthetic entries select exactly rather than by fallback. */
    private static String running() {
        return ModVersionLookup.versionForNamespace(ResourceLocation.DEFAULT_NAMESPACE).orElseThrow();
    }

    private static String entryJson(String version, String age, double weight) {
        return "{ \"version\": \"" + version + "\", \"age\": \"" + age + "\", \"enabled\": true, \"variants\": [ "
                + "{ \"weight\": " + weight + ", \"heads\": [ { \"attachPoint\": \"head\", \"eyes\": [ "
                + EYE_JSON + " ] } ] } ] }";
    }

    private static JsonElement fileJson(String... entries) {
        return JsonParser.parseString("{ \"entries\": [ " + String.join(", ", entries) + " ] }");
    }

    /**
     * No datapack, from any namespace, may install an eye config for the ender dragon. In game: add a
     * datapack with {@code data/minecraft/eyes/ender_dragon.json} and run {@code /reload}; the server log
     * reports the dragon file refused.
     */
    public static void reloadHardExcludesEnderDragon(GameTestHelper helper) {
        ResourceLocation zombie = ResourceLocation.fromNamespaceAndPath("minecraft", "zombie");
        Map<ResourceLocation, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            new TestReloadListener().applyFiles(Map.of(
                    ServerEyeConfigs.ENDER_DRAGON, fileJson(entryJson(running(), "adult", 1.0)),
                    zombie, fileJson(entryJson(running(), "adult", 1.0))));
            helper.assertTrue(ServerEyeConfigs.get(ServerEyeConfigs.ENDER_DRAGON, false) == null,
                    "the ender dragon config is refused at reload");
            helper.assertTrue(usable(ServerEyeConfigs.get(zombie, false)),
                    "other entities in the same batch still load");
        } finally {
            ServerEyeConfigs.replaceAll(original);
        }
        helper.succeed();
    }

    /**
     * One malformed file, a duplicate age/version entry, and an invalid age string never abort the reload.
     * In game: add a datapack whose {@code zombie.json} has two adult entries for the same version,
     * whose {@code creeper.json} is {@code { "entries": 5 }}, and whose {@code skeleton.json} has an
     * {@code "elder"} entry beside a valid adult one; {@code /reload} logs the problems and completes, and
     * zombies and skeletons still get eyes, the zombies from the first entry.
     */
    public static void reloadToleratesBadFilesAndDuplicates(GameTestHelper helper) {
        ResourceLocation zombie = ResourceLocation.fromNamespaceAndPath("minecraft", "zombie");
        ResourceLocation creeper = ResourceLocation.fromNamespaceAndPath("minecraft", "creeper");
        ResourceLocation skeleton = ResourceLocation.fromNamespaceAndPath("minecraft", "skeleton");
        Map<ResourceLocation, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            new TestReloadListener().applyFiles(Map.of(
                    zombie, fileJson(entryJson(running(), "adult", 1.0), entryJson(running(), "adult", 2.0)),
                    creeper, JsonParser.parseString("{ \"entries\": 5 }"),
                    skeleton, fileJson(entryJson(running(), "elder", 1.0), entryJson(running(), "adult", 3.0))));

            RuntimeConfig z = ServerEyeConfigs.get(zombie, false);
            helper.assertTrue(z != null && z.variants.get(0).weight == 1.0,
                    "a duplicate age/version entry keeps the first");
            helper.assertTrue(ServerEyeConfigs.get(creeper, false) == null,
                    "a malformed file is skipped without aborting the batch");
            RuntimeConfig s = ServerEyeConfigs.get(skeleton, false);
            helper.assertTrue(s != null && s.variants.get(0).weight == 3.0,
                    "an invalid age is ignored while its file's valid entries still load");
        } finally {
            ServerEyeConfigs.replaceAll(original);
        }
        helper.succeed();
    }

    /**
     * An entry with no range covering the loaded version degrades to its nearest generation, not to
     * nothing. In game: add a datapack whose {@code zombie.json} has a single entry for
     * {@code "[1.16,1.17)"}, run {@code /reload}, and zombies still spawn with those eyes.
     */
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

    /**
     * A reload installs a new config set only when the resolved content changes; server stop clears the
     * installed set. In game: near some eyed mobs, run {@code /reload} with no datapack changes; their
     * pupils keep moving without resetting. Change an eye definition and {@code /reload} again, and every
     * eye resets to the new definition.
     */
    public static void reloadReplacesConfigsOnlyOnContentChange(GameTestHelper helper) {
        ResourceLocation zombie = ResourceLocation.fromNamespaceAndPath("minecraft", "zombie");
        Map<ResourceLocation, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            Map<ResourceLocation, JsonElement> files = Map.of(
                    zombie, fileJson(entryJson(running(), "adult", 1.0)));

            new TestReloadListener().applyFiles(files);
            Map<ResourceLocation, RuntimeConfigSet> afterFirst = ServerEyeConfigs.all();

            new TestReloadListener().applyFiles(files);
            helper.assertTrue(ServerEyeConfigs.all() == afterFirst,
                    "an identical reload does not replace the installed configs");

            ServerEyeConfigs.onServerStopping();
            new TestReloadListener().applyFiles(files);
            helper.assertTrue(ServerEyeConfigs.all() != afterFirst,
                    "server stop clears the installed set, so the next reload installs anew");
        } finally {
            ServerEyeConfigs.replaceAll(original);
        }
        helper.succeed();
    }
}
