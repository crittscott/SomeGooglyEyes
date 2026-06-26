package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfig;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Read-only checks on the datapack-loaded {@link ServerEyeConfigs} (the shipped configs are loaded by the
 * gametest server — the manual {@code somegoogly-test-datapack} is not mounted here), plus the pure
 * {@link ServerConfig#percentFor} resolution. {@code percentFor} mutates server config values via their
 * public {@code set}; every test restores the originals in a {@code finally} so later tests aren't affected.
 */
@GameTestHolder(SomeGoogly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConfigGameTests {

    private static final String TEMPLATE = "empty";

    private ConfigGameTests() {
    }

    private static boolean usable(RuntimeConfig config) {
        return config != null && config.isEnabled() && config.variants != null && !config.variants.isEmpty();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void percentForResolvesExactBeforeWildcard(GameTestHelper helper) {
        int originalGlobal = ServerConfig.GLOBAL_PERCENT.get();
        List<? extends String> originalOverrides = ServerConfig.ENTITY_OVERRIDES.get();
        try {
            ServerConfig.GLOBAL_PERCENT.set(2);
            // List an exact id and a broad wildcard; the exact id must win regardless of list order, and
            // an unmatched id must fall through to globalPercent.
            ServerConfig.ENTITY_OVERRIDES.set(List.of("minecraft:zombie,100", "minecraft:*,50"));

            int zombie = ServerConfig.percentFor(new ResourceLocation("minecraft", "zombie"));
            int cow = ServerConfig.percentFor(new ResourceLocation("minecraft", "cow"));
            int other = ServerConfig.percentFor(new ResourceLocation("examplemod", "thing"));

            helper.assertTrue(zombie == 100, "exact override should win (expected 100, got " + zombie + ")");
            helper.assertTrue(cow == 50, "wildcard override should apply to cow (expected 50, got " + cow + ")");
            helper.assertTrue(other == 2, "unmatched id should fall back to globalPercent (expected 2, got " + other + ")");
        } finally {
            ServerConfig.GLOBAL_PERCENT.set(originalGlobal);
            ServerConfig.ENTITY_OVERRIDES.set(originalOverrides);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void shippedConfigsLoadForKnownEntities(GameTestHelper helper) {
        RuntimeConfig cow = ServerEyeConfigs.get(new ResourceLocation("minecraft", "cow"), false);
        helper.assertTrue(usable(cow), "cow should have a usable shipped eye config");

        // Players have a definition (player.json) and so are configured — the basis for being a potion target.
        RuntimeConfig player = ServerEyeConfigs.get(new ResourceLocation("minecraft", "player"), false);
        helper.assertTrue(usable(player), "player should have a usable shipped eye config");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void shippedPigHasTwoVariants(GameTestHelper helper) {
        RuntimeConfig pig = ServerEyeConfigs.get(new ResourceLocation("minecraft", "pig"), false);
        helper.assertTrue(usable(pig), "pig should have a usable shipped eye config");
        helper.assertTrue(pig.variants.size() == 2,
                "pig ships two placement variants (got " + pig.variants.size() + ")");
        helper.succeed();
    }
}
