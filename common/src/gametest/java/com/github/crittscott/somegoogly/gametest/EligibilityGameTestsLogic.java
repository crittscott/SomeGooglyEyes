package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.ServerConfig;
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
import com.github.crittscott.somegoogly.server.ServerServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
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

    /**
     * {@link ServerEyeConfigs#canEverWearEyes} consults <b>both</b> age buckets, so the at-spawn roll is
     * offered to a mob that only has a config for the life stage it is not currently in — otherwise it
     * would store {@code hasGooglyEyes=false} for life and never gain eyes on aging.
     */
    public static void canEverWearEyesSpansBothAgeBuckets(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        ResourceLocation cowId = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW);
        Map<ResourceLocation, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            RuntimeConfigSet adultOnly = new RuntimeConfigSet();
            adultOnly.adult = usableConfig();
            ServerEyeConfigs.replaceAll(Map.of(cowId, adultOnly));
            helper.assertTrue(ServerEyeConfigs.canEverWearEyes(cow),
                    "an adult-only config still lets the entity roll at spawn");

            RuntimeConfigSet babyOnly = new RuntimeConfigSet();
            babyOnly.baby = usableConfig();
            ServerEyeConfigs.replaceAll(Map.of(cowId, babyOnly));
            helper.assertTrue(ServerEyeConfigs.canEverWearEyes(cow),
                    "a baby-only config also lets an adult roll, so both buckets are consulted");

            ServerEyeConfigs.replaceAll(Map.of());
            helper.assertTrue(!ServerEyeConfigs.canEverWearEyes(cow),
                    "an unconfigured entity can never wear eyes");
        } finally {
            ServerEyeConfigs.replaceAll(original);
        }
        helper.succeed();
    }

    /**
     * The natural-eye decision in {@link ServerServices#onLivingEntityLoaded}: {@code googlyEyesEnabled}
     * suppresses the roll but the entity is still marked initialized, and a later load never revisits an
     * entity that already has a decision and a variant roll.
     */
    public static void naturalDecisionRespectsMasterToggleAndIsOneShot(GameTestHelper helper) {
        ResourceLocation cowId = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW);
        Map<ResourceLocation, RuntimeConfigSet> originalConfigs = ServerEyeConfigs.all();
        boolean originalEnabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
        int originalPercent = ServerConfig.GLOBAL_PERCENT.get();
        try {
            RuntimeConfigSet set = new RuntimeConfigSet();
            set.any = usableConfig();
            ServerEyeConfigs.replaceAll(Map.of(cowId, set));
            ServerConfig.GLOBAL_PERCENT.set(100);

            ServerConfig.GOOGLY_EYES_ENABLED.set(false);
            Cow suppressed = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
            helper.assertTrue(EyeState.isInitialized(suppressed),
                    "the decision is still recorded so it is never re-rolled");
            helper.assertTrue(!EyeState.hasEyes(suppressed), "googlyEyesEnabled=false suppresses the spawn roll");

            ServerConfig.GOOGLY_EYES_ENABLED.set(true);
            Cow eyed = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(4, 2, 2));
            helper.assertTrue(EyeState.hasEyes(eyed), "an eligible entity at globalPercent=100 rolls eyes");

            float roll = EyeState.getVariantRoll(eyed);
            ServerConfig.GLOBAL_PERCENT.set(0);
            ServerServices.onLivingEntityLoaded(eyed);
            helper.assertTrue(EyeState.hasEyes(eyed) && EyeState.getVariantRoll(eyed) == roll,
                    "a later load never revisits an already-initialized entity");
        } finally {
            ServerEyeConfigs.replaceAll(originalConfigs);
            ServerConfig.GOOGLY_EYES_ENABLED.set(originalEnabled);
            ServerConfig.GLOBAL_PERCENT.set(originalPercent);
        }
        helper.succeed();
    }

    /** A creative slimy-eye application succeeds and gains the target eyes, but spends no eye from the stack. */
    public static void slimyEyeCreativeApplicationDoesNotConsume(GameTestHelper helper, Player player) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        EyeState.setHasEyes(cow, false);
        Map<ResourceLocation, RuntimeConfigSet> original = ServerEyeConfigs.all();
        boolean instabuild = player.getAbilities().instabuild;
        try {
            RuntimeConfigSet set = new RuntimeConfigSet();
            set.any = usableConfig();
            ServerEyeConfigs.replaceAll(Map.of(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW), set));

            player.getAbilities().instabuild = true;
            ItemStack stack = SlimyEyeItem.create(AppearanceOverride.EMPTY, 3);
            InteractionResult applied = SlimyEyeItem.applyToTarget(stack, player, cow);

            helper.assertTrue(applied.consumesAction(), "a creative application still succeeds");
            helper.assertTrue(EyeState.hasEyes(cow), "the target gains eyes");
            helper.assertTrue(stack.getCount() == 3, "a creative application consumes no eye");
        } finally {
            player.getAbilities().instabuild = instabuild;
            ServerEyeConfigs.replaceAll(original);
        }
        helper.succeed();
    }

    /** {@link SlimyEyeItem#use}: only a sneaking use applies the eye to the player; a plain right-click passes. */
    public static void slimyEyeSelfApplyRequiresSneak(GameTestHelper helper, Player player) {
        Map<ResourceLocation, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            RuntimeConfigSet set = new RuntimeConfigSet();
            set.any = usableConfig();
            ServerEyeConfigs.replaceAll(Map.of(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER), set));
            EyeState.setHasEyes(player, false);

            ItemStack stack = SlimyEyeItem.create(AppearanceOverride.EMPTY, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            SlimyEyeItem item = (SlimyEyeItem) stack.getItem();

            player.setShiftKeyDown(false);
            InteractionResultHolder<ItemStack> passed = item.use(player.level(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(passed.getResult() == InteractionResult.PASS,
                    "a non-sneaking use passes so a stray right-click does not eye the player");
            helper.assertTrue(!EyeState.hasEyes(player), "nothing was applied");

            player.setShiftKeyDown(true);
            InteractionResultHolder<ItemStack> consumed = item.use(player.level(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(consumed.getResult().consumesAction(), "sneak + use applies the eye to the player");
            helper.assertTrue(EyeState.hasEyes(player), "the player now has eyes");
        } finally {
            player.setShiftKeyDown(false);
            EyeState.disableAndClearProperties(player);
            ServerEyeConfigs.replaceAll(original);
        }
        helper.succeed();
    }
}
