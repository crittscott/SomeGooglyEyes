package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo.HeadConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfigSet;
import com.github.crittscott.somegoogly.eye.HeadInfo.Variant;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

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

    /**
     * {@code /sg spawnall} terraforms and mass-spawns with no undo, so its server-config gate must ship
     * opt-in. Guards the default; the packet-handler refusal itself needs a live player and stays
     * source-verified like the rest of picker behavior.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void spawnAllDefaultsOff(GameTestHelper helper) {
        helper.assertTrue(!ServerConfig.ALLOW_SPAWN_ALL.get(),
                "allowSpawnAll must default to false (spawnall is opt-in)");
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

    /**
     * Every head in every shipped config must carry a non-blank attach token. The attachPoint is required
     * (no default), so a token that loads as {@code null}/empty means a corrupt data edit — this catches
     * that across all 80+ files in one cheap headless check. It does not (and can't, on the dedicated
     * server, where the client-only resolvers don't load) verify the token resolves to a real model part;
     * that needs an in-client pass.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
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
        if (config == null || config.variants == null) {
            return;
        }
        for (Variant variant : config.variants) {
            if (variant == null || variant.heads == null) {
                continue;
            }
            for (HeadConfig head : variant.heads) {
                helper.assertTrue(head != null && head.attachPoint != null && !head.attachPoint.isBlank(),
                        "config " + id + " has a head with a blank attach token");
            }
        }
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void shippedPigHasOneVariant(GameTestHelper helper) {
        RuntimeConfig pig = ServerEyeConfigs.get(new ResourceLocation("minecraft", "pig"), false);
        helper.assertTrue(usable(pig), "pig should have a usable shipped eye config");
        helper.assertTrue(pig.variants.size() == 1,
                "pig ships one placement variant (got " + pig.variants.size() + ")");
        helper.succeed();
    }
}
