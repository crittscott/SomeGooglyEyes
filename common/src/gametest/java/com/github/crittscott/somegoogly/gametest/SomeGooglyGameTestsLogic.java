package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.config.EyeConfigModel;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.enchant.ModEnchantments;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.item.ModItems;
import com.github.crittscott.somegoogly.platform.EntityPersistentData;
import com.github.crittscott.somegoogly.server.EyeItemService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Core integration checks for loaded cow geometry, spawn-time eye-state initialization, entity
 * appearance overrides, the Googly Eye item's portable appearance payload, and death harvesting.
 */
public final class SomeGooglyGameTestsLogic {

    private SomeGooglyGameTestsLogic() {
    }

    public static void configuredCowHasServerGeometry(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(cow.getType());
        HeadInfo headInfo = ServerEyeConfigs.resolve(type, cow, EyeState.getVariantRoll(cow));

        helper.assertTrue(ServerEyeConfigs.isEligible(cow), "Expected cow to be eligible for eyes");
        helper.assertTrue(headInfo.hasConfig(), "Expected cow to have selected server eye config");
        helper.assertTrue(headInfo.getHeadCount() > 0, "Expected cow config to have at least one head");
        helper.assertTrue(headInfo.getEyeCount(0) > 0, "Expected cow config to have at least one eye");
        helper.succeed();
    }

    public static void spawnInitializesEyePersistentData(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));

        helper.succeedWhen(() -> {
            CompoundTag data = EntityPersistentData.get(cow);
            helper.assertTrue(data.contains(EyeState.HAS_EYES), "Expected spawned mob to have has-eyes flag");
            helper.assertTrue(data.contains(EyeState.VARIANT_ROLL), "Expected spawned mob to have variant roll");
            float roll = data.getFloat(EyeState.VARIANT_ROLL);
            helper.assertTrue(roll >= 0.0F && roll < 1.0F, "Expected variant roll in [0, 1)");
        });
    }

    public static void eyeStateAppearanceOverridesRoundTrip(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        EyeColor iris = new EyeColor(0.25F, 0.5F, 0.75F);
        EyeColor cornea = new EyeColor(0.9F, 0.8F, 0.7F);

        EyeState.setHasEyes(cow, true);
        EyeState.setIrisTint(cow, iris);
        EyeState.setCorneaTint(cow, cornea);
        EyeState.setGlow(cow, true);

        AppearanceOverride overrides = EyeState.readProperties(cow);
        helper.assertTrue(EyeState.hasEyes(cow), "Expected EyeState has-eyes flag to be true");
        helper.assertTrue(overrides.iris().isPresent() && overrides.iris().get().equals(iris),
                "Expected iris override to round-trip");
        helper.assertTrue(overrides.cornea().isPresent() && overrides.cornea().get().equals(cornea),
                "Expected cornea override to round-trip");
        helper.assertTrue(overrides.glow().isPresent() && overrides.glow().get(),
                "Expected glow override to round-trip");
        helper.succeed();
    }

    public static void googlyEyeItemStoresAppearanceOverride(GameTestHelper helper) {
        AppearanceOverride appearance = AppearanceOverride.EMPTY
                .withIrisColor(new EyeColor(0.1F, 0.2F, 0.3F))
                .withCorneaColor(new EyeColor(0.4F, 0.5F, 0.6F))
                .withGlow(true);

        ItemStack stack = GooglyEyeItem.create(appearance, 3);
        AppearanceOverride roundTrip = EyeItemProperties.get(stack);

        helper.assertTrue(stack.getCount() == 3, "Expected created eye stack count to be preserved");
        helper.assertTrue(roundTrip.equals(appearance), "Expected eye item appearance component to round-trip");
        helper.succeed();
    }

    public static void optometristAcceptsOnlyShears(GameTestHelper helper) {
        Holder.Reference<Enchantment> optometrist = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ModEnchantments.OPTOMETRIST);
        helper.assertTrue(optometrist.value().isSupportedItem(new ItemStack(Items.SHEARS)),
                "Optometrist should accept shears");
        helper.assertTrue(!optometrist.value().isSupportedItem(new ItemStack(Items.IRON_PICKAXE)),
                "Optometrist should reject another durable item");
        helper.assertTrue(optometrist.is(EnchantmentTags.TREASURE),
                "Optometrist should remain treasure-only");
        helper.succeed();
    }

    public static void deathHarvestUsesTheSuppliedDropSink(GameTestHelper helper, Player player) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        ItemStack shears = new ItemStack(Items.SHEARS);
        EyeColor iris = new EyeColor(0.2F, 0.4F, 0.6F);
        List<ItemStack> drops = new ArrayList<>();
        int originalPercent = ServerConfig.HARVEST_ON_KILL_PERCENT.get();

        try {
            ServerConfig.HARVEST_ON_KILL_PERCENT.set(100);
            player.setItemInHand(InteractionHand.MAIN_HAND, shears);
            EyeState.setHasEyes(cow, true);
            EyeState.setIrisTint(cow, iris);

            EyeItemService.onDeath(cow, helper.getLevel().damageSources().playerAttack(player), drops::add);
        } finally {
            ServerConfig.HARVEST_ON_KILL_PERCENT.set(originalPercent);
        }

        helper.assertTrue(drops.size() == 1, "A qualifying death harvest should emit exactly one stack");
        helper.assertTrue(drops.get(0).is(ModItems.GOOGLY_EYE.get()), "The emitted stack should be a Googly Eye");
        helper.assertTrue(iris.equals(EyeItemProperties.get(drops.get(0)).iris().orElse(null)),
                "The harvested eye should capture the entity's effective iris color");
        helper.assertTrue(shears.getDamageValue() == 1, "A successful death harvest should damage the shears once");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                        ItemEntity.class, cow.getBoundingBox().inflate(4.0)).isEmpty(),
                "The shared harvest service should not spawn the returned eye directly");
        helper.succeed();
    }

    public static void deathHarvestRejectsNonqualifyingKills(GameTestHelper helper, Player player) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        ItemStack shears = new ItemStack(Items.SHEARS);
        List<ItemStack> drops = new ArrayList<>();
        int originalPercent = ServerConfig.HARVEST_ON_KILL_PERCENT.get();
        Map<ResourceLocation, EyeConfigModel.RuntimeConfigSet> originalConfigs = ServerEyeConfigs.all();

        try {
            ServerConfig.HARVEST_ON_KILL_PERCENT.set(100);
            EyeState.setHasEyes(cow, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
            EyeItemService.onDeath(cow, helper.getLevel().damageSources().playerAttack(player), drops::add);

            player.setItemInHand(InteractionHand.MAIN_HAND, shears);
            EyeState.setHasEyes(cow, false);
            EyeItemService.onDeath(cow, helper.getLevel().damageSources().playerAttack(player), drops::add);

            EyeState.setHasEyes(cow, true);
            EyeItemService.onDeath(cow, helper.getLevel().damageSources().mobAttack(cow), drops::add);

            ServerConfig.HARVEST_ON_KILL_PERCENT.set(0);
            EyeItemService.onDeath(cow, helper.getLevel().damageSources().playerAttack(player), drops::add);

            ServerConfig.HARVEST_ON_KILL_PERCENT.set(100);
            ServerEyeConfigs.replaceAll(Map.of());
            EyeItemService.onDeath(cow, helper.getLevel().damageSources().playerAttack(player), drops::add);
        } finally {
            ServerEyeConfigs.replaceAll(originalConfigs);
            ServerConfig.HARVEST_ON_KILL_PERCENT.set(originalPercent);
        }

        helper.assertTrue(drops.isEmpty(), "Nonqualifying death harvests should emit no eye stacks");
        helper.assertTrue(shears.getDamageValue() == 0, "Nonqualifying death harvests should not damage shears");
        helper.succeed();
    }
}
