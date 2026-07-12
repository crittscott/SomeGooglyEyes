package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.config.EyeConfigReloadListener;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo.HeadConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfigSet;
import com.github.crittscott.somegoogly.eye.HeadInfo.Variant;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Checks on the datapack-loaded {@link ServerEyeConfigs} (the shipped configs are loaded by the
 * gametest server — the manual {@code somegoogly-test-datapack} is not mounted here), the reload
 * listener's version-fallback selection, and the pure {@link ServerConfig#percentFor} resolution.
 * Tests that mutate shared state (config values via their public {@code set}, the loaded eye configs
 * via {@code replaceAll}) restore the originals in a {@code finally} so later tests aren't affected.
 */
@GameTestHolder(SomeGoogly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ConfigGameTests {

    private static final String TEMPLATE = "empty";

    private ConfigGameTests() {
    }

    private static boolean usable(RuntimeConfig config) {
        return RuntimeConfig.isUsable(config);
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

        // Players have a definition (player.json) and so are configured — the basis for being an application target.
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

    /**
     * Version fallback, end to end: a file whose entries are all older than the installed version must
     * not be dropped — the newest generation is selected instead, adult and baby together (they share a
     * declared version). The installed version for the {@code minecraft} namespace is the game version
     * (1.20.x), so every range below 1.20 is stale here. The generations are told apart by weight.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void staleConfigFallsBackToNewestGeneration(GameTestHelper helper) {
        ResourceLocation id = new ResourceLocation("minecraft", "zombie");
        String json = """
                { "entries": [
                    { "version": "[1.18,1.19)", "age": "adult", "enabled": true, "variants": [
                        { "weight": 1.0, "heads": [ { "attachPoint": "head", "eyes": [] } ] } ] },
                    { "version": "[1.19,1.20)", "age": "adult", "enabled": true, "variants": [
                        { "weight": 2.0, "heads": [ { "attachPoint": "head", "eyes": [] } ] } ] },
                    { "version": "[1.19,1.20)", "age": "baby", "enabled": true, "variants": [
                        { "weight": 2.0, "heads": [ { "attachPoint": "head", "eyes": [] } ] } ] }
                ] }
                """;
        Map<ResourceLocation, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            new TestReloadListener().applyFiles(Map.of(id, JsonParser.parseString(json)));
            RuntimeConfig adult = ServerEyeConfigs.get(id, false);
            RuntimeConfig baby = ServerEyeConfigs.get(id, true);
            helper.assertTrue(adult != null, "a stale config must fall back, not vanish");
            helper.assertTrue(adult.variants.get(0).weight() == 2.0,
                    "fallback must pick the newest older generation (weight 2), got "
                            + adult.variants.get(0).weight());
            helper.assertTrue(baby != null && baby.variants.get(0).weight() == 2.0,
                    "the baby entry of the same generation must fall back with it");
        } finally {
            ServerEyeConfigs.replaceAll(original);
        }
        helper.succeed();
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
