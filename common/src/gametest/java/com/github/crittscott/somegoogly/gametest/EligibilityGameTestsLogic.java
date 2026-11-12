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
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.item.EyeItemService;
import com.github.crittscott.somegoogly.registry.ModContent;
import com.github.crittscott.somegoogly.platform.EntityPersistentData;
import com.github.crittscott.somegoogly.server.ServerServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Which mobs can get eyes. The natural eye decision is tested through a real spawn, and the Slimy Eye's apply verb
 * ({@link EyeItemService#applySlimyEye}) through its refusals and acceptance: an eyeless, configured mob gains eyes
 * carrying the stack's appearance on a freshly drawn variant roll and one eye is consumed; an unconfigured, disabled,
 * or already-eyed mob is refused with nothing consumed and nothing touched. Tests swap the config store via
 * {@link TestEyeConfigs} and restore it afterwards rather than depending on which configs happen to ship.
 */
public final class EligibilityGameTestsLogic {

    private EligibilityGameTestsLogic() {
    }

    private static ItemStack slimyEye(AppearanceOverride properties, int count) {
        ItemStack stack = new ItemStack(ModContent.SLIMY_EYE.get(), count);
        EyeItemProperties.set(stack, properties);
        return stack;
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


    /**
     * A Slimy Eye applies only to an eligible, eyeless mob, and only while the mod is enabled. In game, in
     * survival, craft a red-iris Slimy Eye (Googly Eye plus red dye, then plus a slimeball) and right-click:
     * a cow disabled by a datapack whose {@code data/minecraft/eyes/cow.json} entry has {@code "enabled": false} (refused,
     * item kept); an eyeless cow with {@code googlyEyesEnabled = false} (refused, item kept); and an eyeless
     * cow with it {@code true} (the cow gains red-iris eyes and one Slimy Eye is used).
     */
    public static void slimyEyeAppliesOnlyToEligibleTargets(GameTestHelper helper, Player player) {
        // The loader's shared fake player takes the server's default game mode, and creative skips both the
        // consumption and the durability cost this test checks.
        ((ServerPlayer) player).setGameMode(GameType.SURVIVAL);
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        EyeColor red = new EyeColor(1F, 0F, 0F);
        // The spawn itself may have naturally rolled eyes under whatever configs were live at the time;
        // force the deterministic eyeless starting point this test assumes.
        EyeState.setHasEyes(cow, false);

        Map<Identifier, RuntimeConfigSet> original = ServerEyeConfigs.all();
        boolean originalEnabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
        try {
            // Unconfigured: refused outright, and the eye stays in hand.
            TestEyeConfigs.install(Map.of());
            ItemStack stack = slimyEye(AppearanceOverride.EMPTY.withIrisColor(red), 2);
            InteractionResult refused = EyeItemService.applySlimyEye(stack, (ServerPlayer) player, cow);
            helper.assertTrue(!refused.consumesAction(), Component.literal("an ineligible target should refuse the apply"));
            helper.assertTrue(!EyeState.hasEyes(cow), Component.literal("an ineligible target should not gain eyes"));
            helper.assertTrue(stack.getCount() == 2, Component.literal("a refused apply should not consume the eye"));

            RuntimeConfigSet disabledSet = new RuntimeConfigSet();
            disabledSet.any = usableConfig();
            disabledSet.any.enabled = false;
            TestEyeConfigs.install(Map.of(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW), disabledSet));
            helper.assertTrue(EyeItemService.applySlimyEye(stack, player, cow) == InteractionResult.PASS
                            && stack.getCount() == 2 && !EyeState.hasEyes(cow),
                    Component.literal("an explicitly disabled definition refuses use without consuming the eye"));

            // Configured but the master switch is off: still refused, nothing consumed.
            RuntimeConfigSet set = new RuntimeConfigSet();
            set.any = usableConfig();
            TestEyeConfigs.install(Map.of(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW), set));
            ServerConfig.GOOGLY_EYES_ENABLED.set(false);
            helper.assertTrue(EyeItemService.verdictFor(cow) == EyeItemService.Verdict.CANNOT,
                    Component.literal("the master switch decides the verdict, so the inspector hint agrees with the refusal"));
            InteractionResult disabled = EyeItemService.applySlimyEye(stack, (ServerPlayer) player, cow);
            helper.assertTrue(!disabled.consumesAction(), Component.literal("googlyEyesEnabled=false should refuse the apply"));
            helper.assertTrue(!EyeState.hasEyes(cow), Component.literal("a disabled apply should not gain eyes"));
            helper.assertTrue(stack.getCount() == 2, Component.literal("a disabled apply should not consume the eye"));

            // Configured and enabled: eyes on, the stack's appearance carried across, exactly one eye spent.
            ServerConfig.GOOGLY_EYES_ENABLED.set(true);
            InteractionResult applied = EyeItemService.applySlimyEye(stack, (ServerPlayer) player, cow);
            helper.assertTrue(applied.consumesAction(), Component.literal("an eligible target should accept the apply"));
            helper.assertTrue(EyeState.hasEyes(cow), Component.literal("an eligible target should gain eyes"));
            helper.assertTrue(red.equals(EyeState.readProperties(cow).iris().orElse(null)),
                    Component.literal("the applied eyes should carry the stack's iris color"));
            helper.assertTrue(stack.getCount() == 1, Component.literal("applying should consume exactly one eye"));
        } finally {
            TestEyeConfigs.install(original);
            ServerConfig.GOOGLY_EYES_ENABLED.set(originalEnabled);
        }
        helper.succeed();
    }

    /**
     * An already-eyed mob refuses a Slimy Eye, keeps its look, and gets the click for its own use. In game:
     * right-click a cow with red-iris eyes using a blue-iris Slimy Eye; the item is kept and the cow's eyes stay
     * red and in place. Right-click an eyed horse with a Slimy Eye in hand; you mount it.
     */
    public static void slimyEyeRefusesAnAlreadyEyedTarget(GameTestHelper helper, Player player) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        EyeColor red = new EyeColor(1F, 0F, 0F);

        Map<Identifier, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            RuntimeConfigSet set = new RuntimeConfigSet();
            set.any = usableConfig();
            TestEyeConfigs.install(Map.of(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW), set));

            EyeState.setIrisTint(cow, red);
            EyeState.setHasEyes(cow, true);
            float roll = EyeState.getVariantRoll(cow);

            ItemStack stack = slimyEye(AppearanceOverride.EMPTY.withIrisColor(new EyeColor(0F, 0F, 1F)), 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            InteractionResult refused = EyeItemService.interact(player, helper.getLevel(), InteractionHand.MAIN_HAND,
                    cow);

            helper.assertTrue(refused == InteractionResult.PASS,
                    Component.literal("an already-eyed target should pass the click on to its own use"));
            helper.assertTrue(stack.getCount() == 1, Component.literal("a refused apply should not consume the eye"));
            helper.assertTrue(EyeState.hasEyes(cow), Component.literal("a refusal should leave the eyes on"));
            helper.assertTrue(red.equals(EyeState.readProperties(cow).iris().orElse(null)),
                    Component.literal("a refusal should not touch the appearance"));
            helper.assertTrue(EyeState.getVariantRoll(cow) == roll, Component.literal("a refusal should not touch the variant roll"));
        } finally {
            TestEyeConfigs.install(original);
        }
        helper.succeed();
    }

    /**
     * Each application draws a fresh placement. In game: take a pig's eyes off with Optometrist shears and put
     * them back with a Slimy Eye, over and over; usually the eyes land on its head, but now and then on its
     * rear.
     */
    public static void slimyEyeRerollsThePlacementVariant(GameTestHelper helper, Player player) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));

        Map<Identifier, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            RuntimeConfigSet set = new RuntimeConfigSet();
            set.any = usableConfig();
            TestEyeConfigs.install(Map.of(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW), set));

            EyeState.setHasEyes(cow, false);
            // A sentinel outside nextFloat()'s [0, 1) range: any freshly drawn roll must displace it.
            EntityPersistentData.get(cow).putFloat(EyeState.VARIANT_ROLL, 2F);

            ItemStack stack = slimyEye(AppearanceOverride.EMPTY, 1);
            InteractionResult applied = EyeItemService.applySlimyEye(stack, (ServerPlayer) player, cow);

            helper.assertTrue(applied.consumesAction(), Component.literal("an eyeless configured target should accept the apply"));
            float roll = EyeState.getVariantRoll(cow);
            helper.assertTrue(roll >= 0F && roll < 1F, Component.literal("an application should draw a fresh variant roll"));
        } finally {
            TestEyeConfigs.install(original);
        }
        helper.succeed();
    }

    /**
     * A cow with a definition only for the life stage it is not in still takes the natural eye roll when it joins
     * the level, and keeps those eyes as it ages; a missing or disabled definition never wins the roll. The at-spawn
     * roll is offered to a mob that only has a config for the life stage it is not currently in — otherwise it
     * would store {@code hasGooglyEyes=false} for life and never gain eyes on aging. In game: add a
     * datapack whose {@code data/minecraft/eyes/cow.json} has only an {@code "adult"} entry, run
     * {@code /reload}, set {@code entityOverrides = ["minecraft:cow,100"]}, and use a cow spawn egg on an
     * adult cow; the calf has no eyes, and once it grows up it does.
     */
    public static void canEverWearEyesSpansBothAgeBuckets(GameTestHelper helper) {
        Map<Identifier, RuntimeConfigSet> original = ServerEyeConfigs.all();
        List<String> overrides = ServerConfig.ENTITY_OVERRIDES.get();
        boolean enabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
        Identifier cowId = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW);
        try {
            ServerConfig.GOOGLY_EYES_ENABLED.set(true);
            ServerConfig.ENTITY_OVERRIDES.set(List.of(cowId + "," + ServerConfig.PERCENT_MAX));
            for (boolean baby : List.of(true, false)) {
                RuntimeConfigSet set = new RuntimeConfigSet();
                if (baby) {
                    set.adult = usableConfig();
                } else {
                    set.baby = usableConfig();
                }
                TestEyeConfigs.install(Map.of(cowId, set));
                // Age is set before the cow joins the level, where the natural eye decision is made.
                Cow cow = Objects.requireNonNull(EntityType.COW.create(helper.getLevel(), EntitySpawnReason.NATURAL));
                cow.setBaby(baby);
                cow.snapTo(helper.absolutePos(new BlockPos(2, 2, 2)), 0F, 0F);
                helper.assertTrue(helper.getLevel().addFreshEntity(cow), Component.literal("the configured cow joins the level"));
                helper.assertTrue(EyeState.hasEyes(cow), Component.literal("the other age's definition permits the natural eye roll"));
                float roll = EyeState.getVariantRoll(cow);
                cow.setBaby(!baby);
                helper.assertTrue(EyeState.hasEyes(cow) && EyeState.getVariantRoll(cow) == roll,
                        Component.literal("aging reveals the layout without rerolling the stored eyes"));
                cow.discard();
            }
            for (boolean missing : List.of(false, true)) {
                RuntimeConfigSet disabled = new RuntimeConfigSet();
                disabled.any = usableConfig();
                disabled.any.enabled = false;
                TestEyeConfigs.install(missing ? Map.of() : Map.of(cowId, disabled));
                Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
                helper.assertTrue(!EyeState.hasEyes(cow), Component.literal("a missing or disabled definition never wins a natural roll"));
            }
        } finally {
            TestEyeConfigs.install(original);
            ServerConfig.ENTITY_OVERRIDES.set(overrides);
            ServerConfig.GOOGLY_EYES_ENABLED.set(enabled);
        }
        helper.succeed();
    }

    /**
     * The natural-eye decision in {@link ServerServices#onLivingEntityLoaded}: {@code googlyEyesEnabled}
     * suppresses the roll but the entity is still marked initialized, and a later load never revisits an
     * entity that already has a decision and a variant roll. In game: set {@code googlyEyesEnabled = false}
     * and {@code entityOverrides = ["minecraft:cow,100"]} and spawn cows; none has eyes, and they still have
     * none after {@code googlyEyesEnabled} is turned back on, while newly spawned cows do. Then set
     * {@code entityOverrides = ["minecraft:cow,0"]} and rejoin the world; the eyed cows keep their eyes in
     * the same places.
     */
    public static void naturalDecisionRespectsMasterToggleAndIsOneShot(GameTestHelper helper) {
        Identifier cowId = BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW);
        Map<Identifier, RuntimeConfigSet> originalConfigs = ServerEyeConfigs.all();
        boolean originalEnabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
        int originalPercent = ServerConfig.GLOBAL_PERCENT.get();
        try {
            RuntimeConfigSet set = new RuntimeConfigSet();
            set.any = usableConfig();
            TestEyeConfigs.install(Map.of(cowId, set));
            ServerConfig.GLOBAL_PERCENT.set(100);

            ServerConfig.GOOGLY_EYES_ENABLED.set(false);
            Cow suppressed = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
            helper.assertTrue(EyeState.isInitialized(suppressed),
                    Component.literal("the decision is still recorded so it is never re-rolled"));
            helper.assertTrue(!EyeState.hasEyes(suppressed), Component.literal("googlyEyesEnabled=false suppresses the spawn roll"));

            ServerConfig.GOOGLY_EYES_ENABLED.set(true);
            Cow eyed = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(4, 2, 2));
            helper.assertTrue(EyeState.hasEyes(eyed), Component.literal("an eligible entity at globalPercent=100 rolls eyes"));

            float roll = EyeState.getVariantRoll(eyed);
            ServerConfig.GLOBAL_PERCENT.set(0);
            ServerServices.onLivingEntityLoaded(eyed);
            helper.assertTrue(EyeState.hasEyes(eyed) && EyeState.getVariantRoll(eyed) == roll,
                    Component.literal("a later load never revisits an already-initialized entity"));
        } finally {
            TestEyeConfigs.install(originalConfigs);
            ServerConfig.GOOGLY_EYES_ENABLED.set(originalEnabled);
            ServerConfig.GLOBAL_PERCENT.set(originalPercent);
        }
        helper.succeed();
    }

    /**
     * A creative slimy-eye application succeeds and gains the target eyes, but spends no eye from the
     * stack. In game: in creative, right-click an eyeless cow with a Slimy Eye; the cow gains eyes and the
     * stack count does not change.
     */
    public static void slimyEyeCreativeApplicationDoesNotConsume(GameTestHelper helper, Player player) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        EyeState.setHasEyes(cow, false);
        Map<Identifier, RuntimeConfigSet> original = ServerEyeConfigs.all();
        boolean instabuild = player.getAbilities().instabuild;
        try {
            RuntimeConfigSet set = new RuntimeConfigSet();
            set.any = usableConfig();
            TestEyeConfigs.install(Map.of(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW), set));

            player.getAbilities().instabuild = true;
            ItemStack stack = slimyEye(AppearanceOverride.EMPTY, 3);
            InteractionResult applied = EyeItemService.applySlimyEye(stack, (ServerPlayer) player, cow);

            helper.assertTrue(applied.consumesAction(), Component.literal("a creative application still succeeds"));
            helper.assertTrue(EyeState.hasEyes(cow), Component.literal("the target gains eyes"));
            helper.assertTrue(stack.getCount() == 3, Component.literal("a creative application consumes no eye"));
        } finally {
            player.getAbilities().instabuild = instabuild;
            TestEyeConfigs.install(original);
        }
        helper.succeed();
    }

    /**
     * {@link SlimyEyeItem#use}: only a sneaking use applies the eye to the player; a plain right-click
     * passes. In game: holding a Slimy Eye, right-click the air and nothing happens; sneak and right-click
     * the air, and you have eyes (visible in third person) and the Slimy Eye is used.
     */
    public static void slimyEyeSelfApplyRequiresSneak(GameTestHelper helper, Player player) {
        Map<Identifier, RuntimeConfigSet> original = ServerEyeConfigs.all();
        try {
            RuntimeConfigSet set = new RuntimeConfigSet();
            set.any = usableConfig();
            TestEyeConfigs.install(Map.of(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER), set));
            EyeState.setHasEyes(player, false);

            ItemStack stack = slimyEye(AppearanceOverride.EMPTY, 1);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            SlimyEyeItem item = (SlimyEyeItem) stack.getItem();

            player.setShiftKeyDown(false);
            InteractionResult passed = item.use(player.level(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(passed == InteractionResult.PASS,
                    Component.literal("a non-sneaking use passes so a stray right-click does not eye the player"));
            helper.assertTrue(!EyeState.hasEyes(player), Component.literal("nothing was applied"));

            player.setShiftKeyDown(true);
            InteractionResult consumed = item.use(player.level(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(consumed.consumesAction(), Component.literal("sneak + use applies the eye to the player"));
            helper.assertTrue(EyeState.hasEyes(player), Component.literal("the player now has eyes"));
        } finally {
            player.setShiftKeyDown(false);
            EyeState.disableAndClearProperties(player);
            TestEyeConfigs.install(original);
        }
        helper.succeed();
    }
}
