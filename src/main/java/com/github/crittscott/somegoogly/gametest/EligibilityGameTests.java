package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo.HeadConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfigSet;
import com.github.crittscott.somegoogly.eye.HeadInfo.Variant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

/**
 * {@link RuntimeConfig#isUsable} is the single eligibility predicate behind the splash potion's
 * targeting, the at-spawn gate, and the client's held-eye inspect indicator, so its endpoints — and
 * {@link ServerEyeConfigs#isEligible} delegating to it — are what keep those three in agreement.
 * The delegation test swaps the config store via {@code replaceAll} (restored afterwards, the same
 * force-and-restore pattern as {@link SpawnGatingGameTests}) rather than depending on which configs
 * happen to ship.
 */
@GameTestHolder(SomeGoogly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EligibilityGameTests {

    private static final String TEMPLATE = "empty";

    private EligibilityGameTests() {
    }

    private static RuntimeConfig usableConfig() {
        HeadConfig head = new HeadConfig();
        head.attachPoint = "head";
        head.eyes = List.of();
        Variant variant = new Variant();
        variant.heads = List.of(head);
        RuntimeConfig config = new RuntimeConfig();
        config.variants = List.of(variant);
        return config;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void usablePredicateEndpoints(GameTestHelper helper) {
        helper.assertTrue(!RuntimeConfig.isUsable(null), "a missing config should not be usable");

        RuntimeConfig noVariants = new RuntimeConfig();
        noVariants.variants = List.of();
        helper.assertTrue(!RuntimeConfig.isUsable(noVariants), "a config with no variants should not be usable");

        RuntimeConfig usable = usableConfig();
        helper.assertTrue(RuntimeConfig.isUsable(usable), "an enabled config with a variant should be usable");

        usable.enabled = false;
        helper.assertTrue(!RuntimeConfig.isUsable(usable), "enabled:false should be a hard disable");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void eligibilityFollowsTheSharedPredicate(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        ResourceLocation cowId = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW);

        Map<ResourceLocation, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            RuntimeConfigSet set = new RuntimeConfigSet();
            set.any = usableConfig();
            ServerEyeConfigs.replaceAll(Map.of(cowId, set));
            helper.assertTrue(ServerEyeConfigs.isEligible(cow), "a cow with a usable config should be eligible");

            ServerEyeConfigs.replaceAll(Map.of());
            helper.assertTrue(!ServerEyeConfigs.isEligible(cow), "a cow with no config should not be eligible");
        } finally {
            ServerEyeConfigs.replaceAll(original);
        }
        helper.succeed();
    }
}
