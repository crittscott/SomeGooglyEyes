package com.github.crittscott.somegoogly.gametest;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.github.crittscott.somegoogly.config.ConfigValue;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.fabric.FabricServerConfig;
import com.github.crittscott.somegoogly.config.fabric.TomlConfig;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fabric GameTest entry points for {@link ConfigGameTestsLogic}, plus the Fabric-only TOML reader behind
 * Fabric's config files.
 */
public final class ConfigGameTests implements FabricGameTest {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void percentForResolvesExactBeforeWildcard(GameTestHelper helper) {
        ConfigGameTestsLogic.percentForResolvesExactBeforeWildcard(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void configSectionsMatchReleasedFiles(GameTestHelper helper) {
        ConfigGameTestsLogic.configSectionsMatchReleasedFiles(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void spawnAllDefaultsOff(GameTestHelper helper) {
        ConfigGameTestsLogic.spawnAllDefaultsOff(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void everyShippedMinecraftDefinitionLoads(GameTestHelper helper) {
        ConfigGameTestsLogic.everyShippedMinecraftDefinitionLoads(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void exactMinecraftGenerationIsSelected(GameTestHelper helper) {
        ConfigGameTestsLogic.exactMinecraftGenerationIsSelected(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void percentForResolvesWildcardsInListOrder(GameTestHelper helper) {
        ConfigGameTestsLogic.percentForResolvesWildcardsInListOrder(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void reloadHardExcludesEnderDragon(GameTestHelper helper) {
        ConfigGameTestsLogic.reloadHardExcludesEnderDragon(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void reloadToleratesBadFilesAndDuplicates(GameTestHelper helper) {
        ConfigGameTestsLogic.reloadToleratesBadFilesAndDuplicates(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void reloadFallsBackToNearestGeneration(GameTestHelper helper) {
        ConfigGameTestsLogic.reloadFallsBackToNearestGeneration(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void reloadReplacesConfigsOnlyOnContentChange(GameTestHelper helper) {
        ConfigGameTestsLogic.reloadReplacesConfigsOnlyOnContentChange(helper);
    }

    /**
     * {@link TomlConfig} writes defaults for an absent file, then re-reads an existing file without overwriting it.
     * In game: open a new world and {@code serverconfig/somegoogly-server.toml} appears with every key at its
     * default; set {@code globalPercent = 17}, run {@code /reload}, and the file still says 17.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void serverTomlRoundTrips(GameTestHelper helper) {
        String defaults = """
                [server]
                googlyEyesEnabled = false
                globalPercent = 17
                entityOverrides = [
                    "minecraft:zombie,100",
                    "*:*_horse,50"
                ]
                """;
        try {
            Path dir = Files.createTempDirectory("somegoogly-toml-test");
            Path file = dir.resolve("server.toml");
            try {
                Map<String, Object> written = TomlConfig.section(TomlConfig.readOrCreate(file, defaults), "server");
                helper.assertTrue(Files.exists(file), "readOrCreate writes the defaults when the file is absent");
                helper.assertTrue(!TomlConfig.bool(written, "googlyEyesEnabled", true), "a boolean round-trips");
                helper.assertTrue(TomlConfig.integer(written, "globalPercent", 5) == 17, "an integer round-trips");
                helper.assertTrue(
                        TomlConfig.strings(written, "entityOverrides", List.of())
                                .equals(List.of("minecraft:zombie,100", "*:*_horse,50")),
                        "a quoted string list round-trips with colons and wildcards intact");

                Files.writeString(file, """
                        [server]
                        googlyEyesEnabled = true
                        globalPercent = 3
                        entityOverrides = []
                        """);
                Map<String, Object> reread = TomlConfig.section(TomlConfig.readOrCreate(file, defaults), "server");
                helper.assertTrue(TomlConfig.bool(reread, "googlyEyesEnabled", false),
                        "an existing file is re-read, not overwritten by the defaults");
                helper.assertTrue(TomlConfig.integer(reread, "globalPercent", 5) == 3,
                        "the re-read picks up the edited value");
                helper.assertTrue(TomlConfig.strings(reread, "entityOverrides", List.of("x")).isEmpty(),
                        "an empty list parses as empty");
            } finally {
                Files.deleteIfExists(file.resolveSibling(file.getFileName() + ".bak"));
                Files.deleteIfExists(file);
                Files.deleteIfExists(dir);
            }
        } catch (IOException e) {
            throw new RuntimeException("TOML round-trip raised an IOException", e);
        }
        helper.succeed();
    }

    /**
     * A server file written by the released 0.8.2 Fabric build loads with every value it holds, and is rewritten
     * once with the keys it lacks at their defaults, keeping those values; a complete file is left alone. In
     * game: on Fabric, open a world made with 0.8.2 whose {@code somegoogly-server.toml} sets
     * {@code globalPercent = 17}; after loading it with this version the file still says 17 and has gained the
     * new keys, such as {@code squintOnSleep = true}.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void releasedServerTomlKeepsItsValues(GameTestHelper helper) {
        List<Runnable> restore = snapshotServerConfig();
        String released = """
                [Server Settings]
                googlyEyesEnabled = true
                globalPercent = 17
                harvestOnKillPercent = 25
                # Per-entity eye chances, one entry per line as "entity-pattern,percent" (percent 0-100).
                entityOverrides = ["minecraft:zombie,100"]

                [Behaviors]
                ambientBehaviors = true
                ambientMinTicks = 150
                ambientMaxTicks = 400
                ambientBehaviorPool = ["somegoogly:blink", "somegoogly:stare"]
                growOnHitPercent = 20
                swirlOnTrade = false
                swirlOnHeal = true
                swirlHealCooldownTicks = 200

                [Picker]
                allowSpawnAll = false
                """;
        try {
            Path dir = Files.createTempDirectory("somegoogly-toml-test");
            Path file = dir.resolve("server.toml");
            try {
                Files.writeString(file, released);
                FabricServerConfig.load(file);
                helper.assertTrue(ServerConfig.GLOBAL_PERCENT.get() == 17
                                && ServerConfig.ENTITY_OVERRIDES.get().equals(List.of("minecraft:zombie,100"))
                                && ServerConfig.AMBIENT_MIN_TICKS.get() == 150
                                && ServerConfig.AMBIENT_BEHAVIOR_POOL.get()
                                        .equals(List.of("somegoogly:blink", "somegoogly:stare"))
                                && !ServerConfig.SWIRL_ON_TRADE.get(),
                        "every released value loads");
                helper.assertTrue(ServerConfig.SQUINT_ON_SLEEP.get() && ServerConfig.GROW_ON_MILK.get(),
                        "keys the released file lacks take their defaults");

                String completed = Files.readString(file);
                helper.assertTrue(completed.contains("globalPercent = 17")
                                && completed.contains("ambientMinTicks = 150")
                                && completed.contains("swirlOnTrade = false")
                                && completed.contains("\"minecraft:zombie,100\""),
                        "the rewritten file keeps the released values");
                helper.assertTrue(completed.contains("squintOnSleep = true")
                                && completed.contains("[\"Server Settings\"]"),
                        "the rewritten file gains the missing keys under the released sections");

                FabricServerConfig.load(file);
                helper.assertTrue(Files.readString(file).equals(completed), "a complete file is left alone");
                helper.assertTrue(ServerConfig.GLOBAL_PERCENT.get() == 17, "the completed file loads the same values");
            } finally {
                restore.forEach(Runnable::run);
                Files.deleteIfExists(file.resolveSibling(file.getFileName() + ".bak"));
                Files.deleteIfExists(file);
                Files.deleteIfExists(dir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Released TOML load raised an IOException", e);
        }
        helper.succeed();
    }

    /**
     * {@link TomlConfig} reads single-quoted literal strings, keeps quoted {@code #} and {@code ]}, and
     * uses the integer default for a value of the wrong type. In game: on a Fabric server, write
     * {@code spawnExcludedMods = ['minecolonies', 'create']} and {@code globalPercent = 5.0} into
     * {@code serverconfig/somegoogly-server.toml}, run {@code /reload}, and check the log warns about
     * {@code globalPercent} while both mods stay excluded.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void serverTomlReadsLiteralStringsAndRejectsWrongTypes(GameTestHelper helper) {
        String source = """
                [picker]
                spawnExcludedMods = ['minecolonies', "create"]
                spawnExcludedEntities = ['mod:a#b', 'mod:c]d'] # trailing comment
                [server]
                globalPercent = 5.0
                """;
        try {
            Path dir = Files.createTempDirectory("somegoogly-toml-test");
            Path file = dir.resolve("server.toml");
            try {
                Files.writeString(file, source);
                CommentedConfig document = TomlConfig.readOrCreate(file, "");
                Map<String, Object> values = TomlConfig.section(document, "picker");
                helper.assertTrue(
                        TomlConfig.strings(values, "spawnExcludedMods", List.of())
                                .equals(List.of("minecolonies", "create")),
                        "literal and basic strings mix in one array");
                helper.assertTrue(
                        TomlConfig.strings(values, "spawnExcludedEntities", List.of())
                                .equals(List.of("mod:a#b", "mod:c]d")),
                        "a literal string keeps # and ] and a trailing comment is stripped");
                helper.assertTrue(TomlConfig.integer(TomlConfig.section(document, "server"), "globalPercent", 7) == 7,
                        "a floating-point value uses the integer default");
            } finally {
                Files.deleteIfExists(file.resolveSibling(file.getFileName() + ".bak"));
                Files.deleteIfExists(file);
                Files.deleteIfExists(dir);
            }
        } catch (IOException e) {
            throw new RuntimeException("TOML literal-string read raised an IOException", e);
        }
        helper.succeed();
    }
    /**
     * Valid TOML syntax and table identity survive completion; malformed input is preserved. In game:
     * quote globalPercent's key and set it to 1_0, add a comment and an unknown key, then reload. The value
     * becomes 10 and the authored content remains. A syntax error is logged without rewriting the file.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void tomlCompletionPreservesAuthoredContent(GameTestHelper helper) {
        Path dir = null;
        try {
            dir = Files.createTempDirectory("somegoogly-toml-completion");
            Path file = dir.resolve("server.toml");
            String source = """
                    ["Server Settings"]
                    # An administrator's spawn policy.
                    "globalPercent" = 1_0
                    unknown = ["keep", "all"]
                    [Behaviors]
                    ambientBehaviorPool = ["somegoogly:unknown", "somegoogly:blink"]
                    [other]
                    globalPercent = 91
                    """;
            Files.writeString(file, source);
            CommentedConfig document = TomlConfig.readOrCreate(file, "");
            helper.assertTrue(TomlConfig.integer(TomlConfig.section(document, ServerConfig.SECTION_SERVER),
                            ServerConfig.GLOBAL_PERCENT_KEY, 5) == 10,
                    "quoted keys and digit separators are parsed in the declared table");
            helper.assertTrue(TomlConfig.completeIfMissing(file, document,
                            "[\"Server Settings\"]\nglobalPercent=5\ngooglyEyesEnabled=true\n"),
                    "an absent schema key is added");
            String completed = Files.readString(file);
            CommentedConfig reread = TomlConfig.readOrCreate(file, "");
            helper.assertTrue(completed.contains("An administrator's spawn policy.")
                            && TomlConfig.section(reread, ServerConfig.SECTION_SERVER).containsKey("unknown")
                            && TomlConfig.strings(TomlConfig.section(reread, ServerConfig.SECTION_BEHAVIORS),
                                    ServerConfig.AMBIENT_BEHAVIOR_POOL_KEY, List.of())
                                    .contains("somegoogly:unknown"),
                    "completion preserves comments, unknown keys, and unrecognized list entries");
            helper.assertTrue(Files.readString(file.resolveSibling("server.toml.bak")).equals(source),
                    "the original text is retained before reformatting");
            String invalid = "[\"Server Settings\"]\nglobalPercent = 17%\n";
            Files.writeString(file, invalid);
            boolean rejected = false;
            try {
                TomlConfig.readOrCreate(file, "");
            } catch (IOException expected) {
                rejected = true;
            }
            helper.assertTrue(rejected && Files.readString(file).equals(invalid),
                    "syntax failure preserves the administrator's file");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (dir != null) {
                try {
                    Files.deleteIfExists(dir.resolve("server.toml.bak"));
                    Files.deleteIfExists(dir.resolve("server.toml"));
                    Files.deleteIfExists(dir);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        helper.succeed();
    }

    /** Capture each shared runtime value without changing the native server configuration. */
    private static List<Runnable> snapshotServerConfig() {
        List<Runnable> restore = new ArrayList<>();
        try {
            for (var field : ServerConfig.class.getFields()) {
                if (field.get(null) instanceof ConfigValue<?> value) {
                    restore.add(restoreValue(value));
                }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
        return restore;
    }

    private static <T> Runnable restoreValue(ConfigValue<T> value) {
        T original = value.get();
        return () -> value.set(original);
    }
}
