package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.config.EyeConfigModel.HeadConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.item.SlimyEyeItem;
import com.github.crittscott.somegoogly.platform.EntityPersistentData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * {@link RuntimeConfig#isUsable} is the single eligibility predicate behind the slimy eye's targeting,
 * the at-spawn gate, and the client's held-eye inspect indicator, so its endpoints — and
 * {@link ServerEyeConfigs#isEligible} delegating to it — are what keep those three in agreement.
 * The delegation test swaps the config store via {@code replaceAll} (restored afterwards, the same
 * force-and-restore pattern as {@code SpawnGatingGameTests}) rather than depending on which configs
 * happen to ship.
 *
 * <p>{@link #slimyEyeAppliesOnlyToEligibleTargets}, {@link #slimyEyeRefusesAnAlreadyEyedTarget}, and
 * {@link #slimyEyeRerollsThePlacementVariant} then pin the apply verb
 * ({@link SlimyEyeItem#applyToTarget}): an eyeless, configured mob gains eyes carrying the stack's
 * appearance on a freshly drawn variant roll and one eye is consumed; an unconfigured or already-eyed
 * mob is refused with nothing consumed and nothing touched.
 */
public final class EligibilityGameTestsLogic {

    private EligibilityGameTestsLogic() {
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

    public static void slimyEyeAppliesOnlyToEligibleTargets(GameTestHelper helper, Player player) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        EyeColor red = new EyeColor(1F, 0F, 0F);
        // The spawn itself may have naturally rolled eyes under whatever configs were live at the time;
        // force the deterministic eyeless starting point this test assumes.
        EyeState.setHasEyes(cow, false);

        Map<ResourceLocation, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            // Unconfigured: refused outright, and the eye stays in hand.
            ServerEyeConfigs.replaceAll(Map.of());
            ItemStack stack = SlimyEyeItem.create(AppearanceOverride.EMPTY.withIrisColor(red), 2);
            InteractionResult refused = SlimyEyeItem.applyToTarget(stack, player, cow);
            helper.assertTrue(!refused.consumesAction(), "an ineligible target should refuse the apply");
            helper.assertTrue(!EyeState.hasEyes(cow), "an ineligible target should not gain eyes");
            helper.assertTrue(stack.getCount() == 2, "a refused apply should not consume the eye");

            // Configured: eyes on, the stack's appearance carried across, exactly one eye spent.
            RuntimeConfigSet set = new RuntimeConfigSet();
            set.any = usableConfig();
            ServerEyeConfigs.replaceAll(Map.of(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW), set));
            InteractionResult applied = SlimyEyeItem.applyToTarget(stack, player, cow);
            helper.assertTrue(applied.consumesAction(), "an eligible target should accept the apply");
            helper.assertTrue(EyeState.hasEyes(cow), "an eligible target should gain eyes");
            helper.assertTrue(red.equals(EyeState.readProperties(cow).iris().orElse(null)),
                    "the applied eyes should carry the stack's iris color");
            helper.assertTrue(stack.getCount() == 1, "applying should consume exactly one eye");
        } finally {
            ServerEyeConfigs.replaceAll(original);
        }
        helper.succeed();
    }

    public static void slimyEyeRefusesAnAlreadyEyedTarget(GameTestHelper helper, Player player) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        EyeColor red = new EyeColor(1F, 0F, 0F);

        Map<ResourceLocation, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            RuntimeConfigSet set = new RuntimeConfigSet();
            set.any = usableConfig();
            ServerEyeConfigs.replaceAll(Map.of(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW), set));

            EyeState.setIrisTint(cow, red);
            EyeState.setHasEyes(cow, true);
            float roll = EyeState.getVariantRoll(cow);

            ItemStack stack = SlimyEyeItem.create(AppearanceOverride.EMPTY.withIrisColor(new EyeColor(0F, 0F, 1F)), 1);
            InteractionResult refused = SlimyEyeItem.applyToTarget(stack, player, cow);

            helper.assertTrue(!refused.consumesAction(), "an already-eyed target should refuse the apply");
            helper.assertTrue(stack.getCount() == 1, "a refused apply should not consume the eye");
            helper.assertTrue(EyeState.hasEyes(cow), "a refusal should leave the eyes on");
            helper.assertTrue(red.equals(EyeState.readProperties(cow).iris().orElse(null)),
                    "a refusal should not touch the appearance");
            helper.assertTrue(EyeState.getVariantRoll(cow) == roll, "a refusal should not touch the variant roll");
        } finally {
            ServerEyeConfigs.replaceAll(original);
        }
        helper.succeed();
    }

    public static void slimyEyeRerollsThePlacementVariant(GameTestHelper helper, Player player) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));

        Map<ResourceLocation, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            RuntimeConfigSet set = new RuntimeConfigSet();
            set.any = usableConfig();
            ServerEyeConfigs.replaceAll(Map.of(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW), set));

            EyeState.setHasEyes(cow, false);
            // A sentinel outside nextFloat()'s [0, 1) range: any freshly drawn roll must displace it.
            EntityPersistentData.get(cow).putFloat(EyeState.VARIANT_ROLL, 2F);

            ItemStack stack = SlimyEyeItem.create(AppearanceOverride.EMPTY, 1);
            InteractionResult applied = SlimyEyeItem.applyToTarget(stack, player, cow);

            helper.assertTrue(applied.consumesAction(), "an eyeless configured target should accept the apply");
            float roll = EyeState.getVariantRoll(cow);
            helper.assertTrue(roll >= 0F && roll < 1F, "an application should draw a fresh variant roll");
        } finally {
            ServerEyeConfigs.replaceAll(original);
        }
        helper.succeed();
    }
}
