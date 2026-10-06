package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.fabric.TomlConfig;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public static void spawnAllDefaultsOff(GameTestHelper helper) {
        ConfigGameTestsLogic.spawnAllDefaultsOff(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void shippedConfigsLoadForKnownEntities(GameTestHelper helper) {
        ConfigGameTestsLogic.shippedConfigsLoadForKnownEntities(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void everyShippedConfigHasNonBlankAttachTokens(GameTestHelper helper) {
        ConfigGameTestsLogic.everyShippedConfigHasNonBlankAttachTokens(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void exactMinecraftGenerationIsSelected(GameTestHelper helper) {
        ConfigGameTestsLogic.exactMinecraftGenerationIsSelected(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void shippedPigHasTwoVariants(GameTestHelper helper) {
        ConfigGameTestsLogic.shippedPigHasTwoVariants(helper);
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
    public static void reloadBumpsGenerationOnlyOnContentChange(GameTestHelper helper) {
        ConfigGameTestsLogic.reloadBumpsGenerationOnlyOnContentChange(helper);
    }

    /** {@link TomlConfig} writes defaults for an absent file, then re-reads an existing file without overwriting it. */
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
                Map<String, Object> written = TomlConfig.readOrCreate(file, defaults);
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
