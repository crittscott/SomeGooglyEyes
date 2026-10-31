package com.github.crittscott.somegoogly.gametest;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.neoforge.NeoForgeServerConfig;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * NeoForge GameTest entry points for {@link ConfigGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class ConfigGameTests {

    private ConfigGameTests() {
    }

    public static void percentForResolvesExactBeforeWildcard(GameTestHelper helper) {
        ConfigGameTestsLogic.percentForResolvesExactBeforeWildcard(helper);
    }

    public static void configSectionsMatchReleasedFiles(GameTestHelper helper) {
        ConfigGameTestsLogic.configSectionsMatchReleasedFiles(helper);
    }

    public static void spawnAllDefaultsOff(GameTestHelper helper) {
        ConfigGameTestsLogic.spawnAllDefaultsOff(helper);
    }

    public static void everyShippedMinecraftDefinitionLoads(GameTestHelper helper) {
        ConfigGameTestsLogic.everyShippedMinecraftDefinitionLoads(helper);
    }

    public static void exactMinecraftGenerationIsSelected(GameTestHelper helper) {
        ConfigGameTestsLogic.exactMinecraftGenerationIsSelected(helper);
    }

    public static void percentForResolvesWildcardsInListOrder(GameTestHelper helper) {
        ConfigGameTestsLogic.percentForResolvesWildcardsInListOrder(helper);
    }

    public static void reloadHardExcludesEnderDragon(GameTestHelper helper) {
        ConfigGameTestsLogic.reloadHardExcludesEnderDragon(helper);
    }

    public static void reloadToleratesBadFilesAndDuplicates(GameTestHelper helper) {
        ConfigGameTestsLogic.reloadToleratesBadFilesAndDuplicates(helper);
    }

    public static void reloadFallsBackToNearestGeneration(GameTestHelper helper) {
        ConfigGameTestsLogic.reloadFallsBackToNearestGeneration(helper);
    }

    public static void reloadReplacesConfigsOnlyOnContentChange(GameTestHelper helper) {
        ConfigGameTestsLogic.reloadReplacesConfigsOnlyOnContentChange(helper);
    }
    /**
     * Native file correction retains released values and adds absent keys. In game: open a world saved
     * by the 1.21.4 release with globalPercent set to 17 and the squintOnSleep line deleted; correction
     * keeps 17 and restores squintOnSleep.
     */
    public static void releasedConfigSurvivesNativeCorrection(GameTestHelper helper) {
        CommentedConfig config = new TomlParser().parse("[\"Server Settings\"]\nglobalPercent = 17\n");
        NeoForgeServerConfig.SPEC.correct(config);
        helper.assertTrue(config.<Number>get(List.of(ServerConfig.SECTION_SERVER, ServerConfig.GLOBAL_PERCENT_KEY)).intValue() == 17,
                Component.literal("native correction retains a released non-default value"));
        helper.assertTrue(config.<Boolean>get(List.of(ServerConfig.SECTION_BEHAVIORS, ServerConfig.SQUINT_ON_SLEEP_KEY)),
                Component.literal("native correction supplies an absent behavior key"));
        helper.succeed();
    }
}

