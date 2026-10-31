package com.github.crittscott.somegoogly.gametest;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.neoforge.NeoForgeServerConfig;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * NeoForge GameTest entry points for {@link ConfigGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConfigGameTests {

    private static final String TEMPLATE = "empty";

    private ConfigGameTests() {
    }

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
     * Native file correction retains released values and adds absent keys. In game: open a world saved
     * by the 1.21.1 release with globalPercent set to 17; correction keeps 17 and adds squintOnSleep.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void releasedConfigSurvivesNativeCorrection(GameTestHelper helper) {
        CommentedConfig config = new TomlParser().parse("[\"Server Settings\"]\nglobalPercent = 17\n");
        NeoForgeServerConfig.SPEC.correct(config);
        helper.assertTrue(config.<Number>get(List.of(ServerConfig.SECTION_SERVER, ServerConfig.GLOBAL_PERCENT_KEY)).intValue() == 17,
                "native correction retains a released non-default value");
        helper.assertTrue(config.<Boolean>get(List.of(ServerConfig.SECTION_BEHAVIORS, ServerConfig.SQUINT_ON_SLEEP_KEY)),
                "native correction supplies an absent behavior key");
        helper.succeed();
    }
}

