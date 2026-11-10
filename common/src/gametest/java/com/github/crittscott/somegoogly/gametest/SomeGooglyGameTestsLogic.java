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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Core integration checks for entity eye-state persistence and conversion, the Googly Eye item's
 * portable appearance payload, and death harvesting.
 */
public final class SomeGooglyGameTestsLogic {

    /** Blocks around the sheared mooshroom's spawn point searched for the cow it becomes. */
    private static final double CONVERSION_SEARCH_RADIUS = 2.0;

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
        Cow restored = Objects.requireNonNull(EntityType.COW.create(helper.getLevel()));
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
        BlockPos spawnPos = new BlockPos(2, 2, 2);
        MushroomCow mooshroom = helper.spawnWithNoFreeWill(EntityType.MOOSHROOM, spawnPos);
        EyeColor iris = new EyeColor(0.2F, 0.4F, 0.6F);
        EyeState.initialize(mooshroom, true, 0.375F);
        EyeState.setIrisTint(mooshroom, iris);

        mooshroom.shear(SoundSource.PLAYERS);

        helper.succeedWhen(() -> {
            // The cow replaces the mooshroom in place; a tight box keeps neighboring tests' cows out of the count.
            AABB area = new AABB(helper.absolutePos(spawnPos)).inflate(CONVERSION_SEARCH_RADIUS);
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
     * Only a qualifying kill harvests. In game, with {@code harvestOnKillPercent = 100}: no Googly Eye drops and the
     * shears keep their durability when an eyed cow is killed with a sword, an eyeless cow with shears, an eyed cow
     * by something other than a player, an eyed cow shot by a player holding shears, an eyed cow killed by the Thorns
     * armor of a player holding shears, an eyed cow with no eye definition, or an eyed cow with shears while
     * {@code harvestOnKillPercent = 0} or {@code googlyEyesEnabled = false} or the {@code doMobLoot} game rule off.
     */
    public static void deathHarvestRejectsNonqualifyingKills(GameTestHelper helper, Player player) {
        Map<ResourceLocation, EyeConfigModel.RuntimeConfigSet> definitions = ServerEyeConfigs.all();
        int percent = ServerConfig.HARVEST_ON_KILL_PERCENT.get();
        boolean enabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
        ItemStack held = player.getMainHandItem().copy();
        boolean creative = player.getAbilities().instabuild;
        GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT);
        boolean mobLoot = rule.get();
        try {
            player.getAbilities().instabuild = false;
            for (String refusal : List.of("sword", "eyeless", "mob", "arrow", "thorns", "chance", "definition",
                    "disabled", "loot")) {
                TestEyeConfigs.install(definitions);
                ServerConfig.GOOGLY_EYES_ENABLED.set(true);
                ServerConfig.HARVEST_ON_KILL_PERCENT.set(ServerConfig.PERCENT_MAX);
                rule.set(true, helper.getLevel().getServer());
                Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
                EyeState.setHasEyes(cow, !refusal.equals("eyeless"));
                ItemStack weapon = new ItemStack(refusal.equals("sword") ? Items.IRON_SWORD : Items.SHEARS);
                player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
                switch (refusal) {
                    case "chance" -> ServerConfig.HARVEST_ON_KILL_PERCENT.set(0);
                    case "definition" -> TestEyeConfigs.install(Map.of());
                    case "disabled" -> ServerConfig.GOOGLY_EYES_ENABLED.set(false);
                    case "loot" -> rule.set(false, helper.getLevel().getServer());
                    default -> {
                    }
                }
                var sources = helper.getLevel().damageSources();
                var source = switch (refusal) {
                    case "mob" -> sources.mobAttack(cow);
                    case "arrow" -> sources.arrow(Objects.requireNonNull(EntityType.ARROW.create(helper.getLevel())), player);
                    case "thorns" -> sources.thorns(player);
                    default -> sources.playerAttack(player);
                };
                List<ItemEntity> before = eyeDrops(helper, cow);
                cow.hurt(source, Float.MAX_VALUE);
                helper.assertTrue(cow.isDeadOrDying(), "the refusal case still kills its victim: " + refusal);
                helper.assertTrue(eyeDrops(helper, cow).equals(before), "no eye drops for " + refusal);
                helper.assertTrue(weapon.getDamageValue() == 0, "no harvest durability is spent for " + refusal);
            }
        } finally {
            TestEyeConfigs.install(definitions);
            ServerConfig.HARVEST_ON_KILL_PERCENT.set(percent);
            ServerConfig.GOOGLY_EYES_ENABLED.set(enabled);
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            player.getAbilities().instabuild = creative;
            rule.set(mobLoot, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    /**
     * A mob lightning turns into another keeps its eye state. In game, on any difficulty but Peaceful: give a
     * pig and a villager eyes and an iris tint with {@code /sg admin}, then strike each with
     * {@code /summon lightning_bolt}; the zombified piglin and the witch show the same eyes in the same color.
     */
    public static void lightningConversionKeepsEyeState(GameTestHelper helper) {
        BlockPos pigPos = new BlockPos(1, 2, 1);
        BlockPos villagerPos = new BlockPos(4, 2, 4);
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, pigPos);
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, villagerPos);
        EyeColor iris = new EyeColor(0.2F, 0.4F, 0.6F);
        for (LivingEntity mob : List.of(pig, villager)) {
            EyeState.initialize(mob, true, 0.375F);
            EyeState.setIrisTint(mob, iris);
        }

        LightningBolt bolt = Objects.requireNonNull(EntityType.LIGHTNING_BOLT.create(helper.getLevel()));
        pig.thunderHit(helper.getLevel(), bolt);
        villager.thunderHit(helper.getLevel(), bolt);

        helper.succeedWhen(() -> {
            AABB pigArea = new AABB(helper.absolutePos(pigPos)).inflate(CONVERSION_SEARCH_RADIUS);
            AABB villagerArea = new AABB(helper.absolutePos(villagerPos)).inflate(CONVERSION_SEARCH_RADIUS);
            List<ZombifiedPiglin> piglins = helper.getLevel().getEntitiesOfClass(ZombifiedPiglin.class, pigArea);
            List<Witch> witches = helper.getLevel().getEntitiesOfClass(Witch.class, villagerArea);
            helper.assertTrue(piglins.size() == 1, "Expected the struck pig to become one zombified piglin");
            helper.assertTrue(witches.size() == 1, "Expected the struck villager to become one witch");
            for (LivingEntity converted : List.of(piglins.get(0), witches.get(0))) {
                helper.assertTrue(EyeState.hasEyes(converted), "A struck mob should keep its eyes");
                helper.assertTrue(EyeState.getVariantRoll(converted) == 0.375F, "A struck mob should keep its variant roll");
                helper.assertTrue(iris.equals(EyeState.readProperties(converted).iris().orElse(null)),
                        "A struck mob should keep its appearance overrides");
            }
        });
    }

    /**
     * A real shears kill harvests through each loader's own death path, and a baby harvests like an adult though it
     * drops no loot of its own; the eye carries the mob's look and the harvest wears the shears once. In game: set
     * {@code harvestOnKillPercent = 100}, tint an eyed cow's iris with {@code /sg admin tint iris 0.2 0.4 0.6}, and
     * kill it and an eyed calf with shears in survival; each drops one Googly Eye in its own iris color, and each
     * harvest costs the shears one durability.
     */
    public static void realShearsKillsHarvestAnEye(GameTestHelper helper, Player player) {
        ItemStack held = player.getMainHandItem().copy();
        boolean creative = player.getAbilities().instabuild;
        boolean enabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
        int percent = ServerConfig.HARVEST_ON_KILL_PERCENT.get();
        GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT);
        boolean mobLoot = rule.get();
        try {
            player.getAbilities().instabuild = false;
            ServerConfig.GOOGLY_EYES_ENABLED.set(true);
            ServerConfig.HARVEST_ON_KILL_PERCENT.set(ServerConfig.PERCENT_MAX);
            rule.set(true, helper.getLevel().getServer());
            for (boolean baby : List.of(false, true)) {
                Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
                cow.setBaby(baby);
                EyeState.setHasEyes(cow, true);
                EyeColor iris = baby ? new EyeColor(0.7F, 0.2F, 0.1F) : new EyeColor(0.2F, 0.4F, 0.6F);
                EyeState.setIrisTint(cow, iris);
                ItemStack shears = new ItemStack(Items.SHEARS);
                player.setItemInHand(InteractionHand.MAIN_HAND, shears);
                List<ItemEntity> before = eyeDrops(helper, cow);
                cow.hurt(helper.getLevel().damageSources().playerAttack(player), Float.MAX_VALUE);
                List<ItemEntity> after = eyeDrops(helper, cow).stream().filter(drop -> !before.contains(drop)).toList();
                helper.assertTrue(cow.isDeadOrDying(), "the real blow kills the cow");
                helper.assertTrue(after.size() == 1, "an adult or baby shears kill drops exactly one eye");
                helper.assertTrue(iris.equals(EyeItemProperties.get(after.get(0).getItem()).iris().orElse(null)),
                        "the real death path carries the chosen iris color");
                helper.assertTrue(shears.getDamageValue() == 1, "one harvest wears the shears exactly once");
            }
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
            player.getAbilities().instabuild = creative;
            ServerConfig.GOOGLY_EYES_ENABLED.set(enabled);
            ServerConfig.HARVEST_ON_KILL_PERCENT.set(percent);
            rule.set(mobLoot, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    /** The Googly Eyes lying around {@code victim}. */
    private static List<ItemEntity> eyeDrops(GameTestHelper helper, LivingEntity victim) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, victim.getBoundingBox().inflate(4.0),
                drop -> drop.getItem().is(ModContent.GOOGLY_EYE.get()));
    }
}
