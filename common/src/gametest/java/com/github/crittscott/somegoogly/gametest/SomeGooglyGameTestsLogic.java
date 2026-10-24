package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.config.EyeConfigModel;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.registry.ModContent;
import com.github.crittscott.somegoogly.server.EyeItemService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Core integration checks for entity eye-state persistence and conversion, the Googly Eye item's
 * portable appearance payload, and death harvesting.
 */
public final class SomeGooglyGameTestsLogic {

    private SomeGooglyGameTestsLogic() {
    }

    /**
     * Eye state survives an entity save and load through the loader's own persistent-data store. In game:
     * give a mob eyes and an iris tint with {@code /sg admin}, save and quit, reopen the world, and the mob
     * still shows the same eyes in the same color.
     */
    public static void entityPersistentDataSurvivesSaveLoad(GameTestHelper helper, String loader) {
        Cow original = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        EyeColor iris = new EyeColor(0.2F, 0.4F, 0.6F);
        EyeState.initialize(original, true, 0.375F);
        EyeState.setIrisTint(original, iris);

        CompoundTag saved = original.saveWithoutId(new CompoundTag());
        Cow restored = Objects.requireNonNull(EntityType.COW.create(helper.getLevel(), EntitySpawnReason.LOAD));
        restored.load(saved);

        helper.assertTrue(EyeState.hasEyes(restored), loader + " should restore the has-eyes flag");
        helper.assertTrue(EyeState.getVariantRoll(restored) == 0.375F,
                loader + " should restore the placement-variant roll");
        helper.assertTrue(iris.equals(EyeState.readProperties(restored).iris().orElse(null)),
                loader + " should restore appearance overrides");
        helper.succeed();
    }

    /**
     * A mob Minecraft converts into another keeps its eye state. In game: give a mooshroom eyes and an iris
     * tint with {@code /sg admin}, then shear it; the cow it becomes shows the same eyes in the same color.
     */
    public static void conversionKeepsEyeState(GameTestHelper helper) {
        MushroomCow mooshroom = helper.spawnWithNoFreeWill(EntityType.MOOSHROOM, new BlockPos(2, 2, 2));
        EyeColor iris = new EyeColor(0.2F, 0.4F, 0.6F);
        EyeState.initialize(mooshroom, true, 0.375F);
        EyeState.setIrisTint(mooshroom, iris);

        mooshroom.shear(helper.getLevel(), SoundSource.PLAYERS, new ItemStack(Items.SHEARS));

        helper.succeedWhen(() -> {
            AABB area = new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(8);
            List<Cow> cows = helper.getLevel().getEntitiesOfClass(Cow.class, area,
                    cow -> cow.getType() == EntityType.COW);
            helper.assertTrue(cows.size() == 1, "Expected the sheared mooshroom to become one cow");
            Cow cow = cows.getFirst();
            helper.assertTrue(EyeState.hasEyes(cow), "Converted mob should keep its eyes");
            helper.assertTrue(EyeState.getVariantRoll(cow) == 0.375F, "Converted mob should keep its variant roll");
            helper.assertTrue(iris.equals(EyeState.readProperties(cow).iris().orElse(null)),
                    "Converted mob should keep its appearance overrides");
        });
    }

    /**
     * A Googly Eye stack carries its appearance. In game: harvest an eye from a cow given an iris, cornea, and
     * glow with {@code /sg admin}; the dropped eye's tooltip lists all three.
     */
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

    /**
     * Optometrist goes only on shears and is treasure-only. In game: in an anvil, an Optometrist book combines
     * with shears but not with a pickaxe, and the enchantment never appears in an enchanting table.
     */
    public static void optometristAcceptsOnlyShears(GameTestHelper helper) {
        Holder.Reference<Enchantment> optometrist = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ModContent.OPTOMETRIST);
        helper.assertTrue(optometrist.value().isSupportedItem(new ItemStack(Items.SHEARS)),
                "Optometrist should accept shears");
        helper.assertTrue(!optometrist.value().isSupportedItem(new ItemStack(Items.IRON_PICKAXE)),
                "Optometrist should reject another durable item");
        helper.assertTrue(optometrist.is(EnchantmentTags.TREASURE),
                "Optometrist should remain treasure-only");
        helper.succeed();
    }

    /**
     * A shears kill can drop one Googly Eye carrying the mob's look. In game: set
     * {@code harvestOnKillPercent = 100}, tint an eyed cow's iris with {@code /sg admin tint iris 0.2 0.4 0.6},
     * and kill it in survival with melee blows from shears; one Googly Eye with that iris color drops with the
     * cow's loot, and the shears lose one durability for the harvest.
     */
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
        helper.assertTrue(drops.get(0).is(ModContent.GOOGLY_EYE.get()), "The emitted stack should be a Googly Eye");
        helper.assertTrue(iris.equals(EyeItemProperties.get(drops.get(0)).iris().orElse(null)),
                "The harvested eye should capture the entity's effective iris color");
        helper.assertTrue(shears.getDamageValue() == 1, "A successful death harvest should damage the shears once");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                        ItemEntity.class, cow.getBoundingBox().inflate(4.0)).isEmpty(),
                "The shared harvest service should not spawn the returned eye directly");
        helper.succeed();
    }

    /**
     * Only a qualifying kill harvests. In game, with {@code harvestOnKillPercent = 100}: no Googly Eye drops when
     * an eyed cow is killed with a sword, an eyeless cow with shears, an eyed cow by something other than a
     * player, an eyed cow shot by a player holding shears, or an eyed cow with shears while {@code harvestOnKillPercent = 0} or
     * {@code googlyEyesEnabled = false}.
     */
    public static void deathHarvestRejectsNonqualifyingKills(GameTestHelper helper, Player player) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        ItemStack shears = new ItemStack(Items.SHEARS);
        List<ItemStack> drops = new ArrayList<>();
        int originalPercent = ServerConfig.HARVEST_ON_KILL_PERCENT.get();
        Map<ResourceLocation, EyeConfigModel.RuntimeConfigSet> originalConfigs = ServerEyeConfigs.all();
        boolean originalEnabled = ServerConfig.GOOGLY_EYES_ENABLED.get();

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

            Arrow arrow = Objects.requireNonNull(
                    EntityType.ARROW.create(helper.getLevel(), EntitySpawnReason.TRIGGERED));
            EyeItemService.onDeath(cow, helper.getLevel().damageSources().arrow(arrow, player), drops::add);

            ServerConfig.HARVEST_ON_KILL_PERCENT.set(0);
            EyeItemService.onDeath(cow, helper.getLevel().damageSources().playerAttack(player), drops::add);

            ServerConfig.HARVEST_ON_KILL_PERCENT.set(100);
            ServerEyeConfigs.replaceAll(Map.of());
            EyeItemService.onDeath(cow, helper.getLevel().damageSources().playerAttack(player), drops::add);

            ServerEyeConfigs.replaceAll(originalConfigs);
            EyeState.setHasEyes(cow, true);
            ServerConfig.GOOGLY_EYES_ENABLED.set(false);
            EyeItemService.onDeath(cow, helper.getLevel().damageSources().playerAttack(player), drops::add);
        } finally {
            ServerEyeConfigs.replaceAll(originalConfigs);
            ServerConfig.HARVEST_ON_KILL_PERCENT.set(originalPercent);
            ServerConfig.GOOGLY_EYES_ENABLED.set(originalEnabled);
        }

        helper.assertTrue(drops.isEmpty(), "Nonqualifying death harvests should emit no eye stacks");
        helper.assertTrue(shears.getDamageValue() == 0, "Nonqualifying death harvests should not damage shears");
        helper.succeed();
    }
}
