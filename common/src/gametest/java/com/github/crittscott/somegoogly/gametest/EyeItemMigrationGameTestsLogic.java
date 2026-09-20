package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.EyeConfigModel.HeadConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.item.ModDataComponents;
import com.github.crittscott.somegoogly.item.ModItems;
import com.github.crittscott.somegoogly.item.SlimyEyeItem;
import com.github.crittscott.somegoogly.recipe.EyeModifierRecipe;
import com.github.crittscott.somegoogly.recipe.SlimyEyeRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.List;
import java.util.Map;

/** Loading-path coverage for converting released 1.20.1 eye-item NBT into the current component. */
public final class EyeItemMigrationGameTestsLogic {

    private static final String LEGACY_PROPERTIES_KEY = "EyeProperties";

    private EyeItemMigrationGameTestsLogic() {
    }

    private static List<Item> eyeItems() {
        return List.of(ModItems.GOOGLY_EYE.get(), ModItems.SLIMY_EYE.get());
    }

    private static ItemStack legacyStack(Item item, AppearanceOverride appearance, int count) {
        return legacyStack(item, appearance.toNbt(), count);
    }

    private static ItemStack legacyStack(Item item, CompoundTag payload, int count) {
        ItemStack stack = new ItemStack(item, count);
        CompoundTag customTag = new CompoundTag();
        customTag.put(LEGACY_PROPERTIES_KEY, payload.copy());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customTag));
        return stack;
    }

    private static ItemStack decode(ItemStack stack, RegistryAccess registries) {
        return ItemStack.parse(registries, stack.save(registries)).orElseThrow();
    }

    private static CompoundTag customTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? new CompoundTag() : customData.copyTag();
    }

    private static void assertLegacyAbsent(GameTestHelper helper, ItemStack stack) {
        helper.assertTrue(!customTag(stack).contains(LEGACY_PROPERTIES_KEY),
                "the converted stack must not retain EyeProperties in custom data");
    }

    private static CraftingInput grid(ItemStack eye, ItemStack modifier) {
        return CraftingInput.of(2, 1, List.of(eye, modifier));
    }

    private static SlimyEyeRecipe slimyEyeRecipe() {
        NonNullList<Ingredient> ingredients = NonNullList.of(Ingredient.EMPTY,
                Ingredient.of(ModItems.GOOGLY_EYE.get()), Ingredient.of(Items.SLIME_BALL));
        return new SlimyEyeRecipe("", CraftingBookCategory.MISC,
                new ItemStack(ModItems.SLIMY_EYE.get()), ingredients);
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

    public static void fullAppearanceMigratesForBothItems(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        AppearanceOverride expected = AppearanceOverride.EMPTY
                .withIrisColor(new EyeColor(0.1F, 0.2F, 0.3F))
                .withCorneaColor(new EyeColor(0.4F, 0.5F, 0.6F))
                .withGlow(false);

        for (Item item : eyeItems()) {
            ItemStack migrated = decode(legacyStack(item, expected, 3), registries);
            helper.assertTrue(migrated.is(item), "migration must preserve item identity");
            helper.assertTrue(migrated.getCount() == 3, "migration must preserve stack count");
            helper.assertTrue(EyeItemProperties.get(migrated).equals(expected),
                    "all released appearance fields must migrate unchanged");
            assertLegacyAbsent(helper, migrated);
            helper.assertTrue(migrated.get(DataComponents.CUSTOM_DATA) == null,
                    "custom data must be removed when EyeProperties was its only entry");
        }
        helper.succeed();
    }

    public static void sparseAndGlowStatesMigrate(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        List<AppearanceOverride> appearances = List.of(
                AppearanceOverride.EMPTY.withIrisColor(new EyeColor(0.2F, 0.4F, 0.6F)),
                AppearanceOverride.EMPTY.withGlow(true),
                AppearanceOverride.EMPTY.withGlow(false),
                AppearanceOverride.EMPTY);

        for (Item item : eyeItems()) {
            for (AppearanceOverride expected : appearances) {
                ItemStack migrated = decode(legacyStack(item, expected, 1), registries);
                helper.assertTrue(EyeItemProperties.get(migrated).equals(expected),
                        "sparse fields and all three glow states must remain distinct");
                assertLegacyAbsent(helper, migrated);
            }
        }
        helper.succeed();
    }

    public static void cleanupPreservesUnrelatedStackData(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        AppearanceOverride expected = AppearanceOverride.EMPTY.withGlow(true);
        Component name = Component.literal("keep");

        for (Item item : eyeItems()) {
            ItemStack source = legacyStack(item, expected, 5);
            CompoundTag customTag = source.get(DataComponents.CUSTOM_DATA).copyTag();
            customTag.putBoolean("keep", true);
            source.set(DataComponents.CUSTOM_DATA, CustomData.of(customTag));
            source.set(DataComponents.CUSTOM_NAME, name);

            ItemStack migrated = decode(source, registries);
            helper.assertTrue(migrated.getCount() == 5, "migration must preserve count");
            helper.assertTrue(name.equals(migrated.get(DataComponents.CUSTOM_NAME)),
                    "migration must preserve unrelated components");
            helper.assertTrue(customTag(migrated).getBoolean("keep"),
                    "migration must preserve unrelated custom data");
            assertLegacyAbsent(helper, migrated);

            ItemStack noLegacy = new ItemStack(item);
            CompoundTag unrelated = new CompoundTag();
            unrelated.putInt("keep", 7);
            noLegacy.set(DataComponents.CUSTOM_DATA, CustomData.of(unrelated));
            ItemStack unchanged = decode(noLegacy, registries);
            helper.assertTrue(EyeItemProperties.get(unchanged).isEmpty(),
                    "a stack without EyeProperties must not gain an appearance component");
            helper.assertTrue(customTag(unchanged).getInt("keep") == 7,
                    "a stack without EyeProperties must remain unchanged");
        }
        helper.succeed();
    }

    public static void currentDataWinsAndMalformedLegacyDataSurvives(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        AppearanceOverride current = AppearanceOverride.EMPTY
                .withIrisColor(new EyeColor(0.8F, 0.7F, 0.6F));
        AppearanceOverride old = AppearanceOverride.EMPTY
                .withIrisColor(new EyeColor(0.1F, 0.2F, 0.3F)).withGlow(true);

        for (Item item : eyeItems()) {
            ItemStack conflict = legacyStack(item, old, 1);
            conflict.set(ModDataComponents.EYE_PROPERTIES.get(), current);
            ItemStack resolved = decode(conflict, registries);
            helper.assertTrue(EyeItemProperties.get(resolved).equals(current),
                    "an existing current component must win without field merging");
            assertLegacyAbsent(helper, resolved);

            ItemStack emptyConflict = legacyStack(item, old, 1);
            emptyConflict.set(ModDataComponents.EYE_PROPERTIES.get(), AppearanceOverride.EMPTY);
            ItemStack emptyResolved = decode(emptyConflict, registries);
            helper.assertTrue(emptyResolved.has(ModDataComponents.EYE_PROPERTIES.get())
                            && EyeItemProperties.get(emptyResolved).isEmpty(),
                    "component presence must win even when the current component is empty");
            assertLegacyAbsent(helper, emptyResolved);

            CompoundTag malformed = new CompoundTag();
            malformed.putString("irisColor", "not-a-color");
            ItemStack retained = decode(legacyStack(item, malformed, 1), registries);
            helper.assertTrue(!retained.has(ModDataComponents.EYE_PROPERTIES.get()),
                    "malformed old data must not invent a current component");
            helper.assertTrue(customTag(retained).contains(LEGACY_PROPERTIES_KEY),
                    "malformed old data must remain available for recovery");
        }
        helper.succeed();
    }

    public static void conversionIsIdempotentAndPersists(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        AppearanceOverride expected = AppearanceOverride.EMPTY
                .withCorneaColor(new EyeColor(0.3F, 0.5F, 0.7F)).withGlow(false);

        for (Item item : eyeItems()) {
            ItemStack migrated = decode(legacyStack(item, expected, 2), registries);
            ItemStack beforeRepeat = migrated.copy();
            EyeItemProperties.migrateStoredAppearance(migrated);
            helper.assertTrue(ItemStack.matches(beforeRepeat, migrated),
                    "running migration again must not change the stack");

            ItemStack reloaded = decode(migrated, registries);
            helper.assertTrue(EyeItemProperties.get(reloaded).equals(expected),
                    "the current component must persist through save and reload");
            assertLegacyAbsent(helper, reloaded);

            ItemStack newlyCreated = new ItemStack(item, 2);
            EyeItemProperties.set(newlyCreated, expected);
            helper.assertTrue(ItemStack.isSameItemSameComponents(reloaded, newlyCreated),
                    "a converted eye must stack with an equivalent newly created eye");
        }
        helper.succeed();
    }

    public static void migratedEyesSurviveCrafting(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        EyeColor cornea = new EyeColor(0.2F, 0.3F, 0.4F);
        AppearanceOverride expected = AppearanceOverride.EMPTY
                .withIrisColor(new EyeColor(0.5F, 0.6F, 0.7F))
                .withCorneaColor(cornea)
                .withGlow(false);
        ItemStack migrated = decode(legacyStack(ModItems.GOOGLY_EYE.get(), expected, 1), registries);

        EyeModifierRecipe modifierRecipe = new EyeModifierRecipe(CraftingBookCategory.MISC);
        ItemStack dyed = modifierRecipe.assemble(grid(migrated, new ItemStack(Items.RED_DYE)), registries);
        AppearanceOverride dyedAppearance = EyeItemProperties.get(dyed);
        helper.assertTrue(dyedAppearance.iris().isPresent(), "dyeing a converted eye must set its iris");
        helper.assertTrue(cornea.equals(dyedAppearance.cornea().orElse(null)),
                "dyeing must retain the converted cornea");
        helper.assertTrue(dyedAppearance.glow().isPresent() && !dyedAppearance.glow().get(),
                "dyeing must retain an explicit converted glow-off override");

        ItemStack slimy = slimyEyeRecipe().assemble(
                grid(migrated, new ItemStack(Items.SLIME_BALL)), registries);
        helper.assertTrue(EyeItemProperties.get(slimy).equals(expected),
                "Slimy Eye crafting must transfer the converted appearance");

        ItemStack cleared = modifierRecipe.assemble(grid(migrated, new ItemStack(Items.COBWEB)), registries);
        ItemStack clearedReloaded = decode(cleared, registries);
        helper.assertTrue(EyeItemProperties.get(clearedReloaded).isEmpty(),
                "cobweb-cleared converted properties must stay cleared after reload");
        assertLegacyAbsent(helper, clearedReloaded);
        helper.succeed();
    }

    public static void migratedSlimyEyeAppliesToMob(GameTestHelper helper, Player player) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        AppearanceOverride expected = AppearanceOverride.EMPTY
                .withIrisColor(new EyeColor(0.2F, 0.5F, 0.8F)).withGlow(false);
        ItemStack migrated = decode(legacyStack(ModItems.SLIMY_EYE.get(), expected, 2), registries);
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        EyeState.setHasEyes(cow, false);

        Map<ResourceLocation, RuntimeConfigSet> originalConfigs = ServerEyeConfigs.all();
        boolean originalEnabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
        boolean originalInstabuild = player.getAbilities().instabuild;
        try {
            RuntimeConfigSet set = new RuntimeConfigSet();
            set.any = usableConfig();
            ServerEyeConfigs.replaceAll(Map.of(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.COW), set));
            ServerConfig.GOOGLY_EYES_ENABLED.set(true);
            player.getAbilities().instabuild = false;

            InteractionResult result = SlimyEyeItem.applyToTarget(migrated, (ServerPlayer) player, cow);
            helper.assertTrue(result.consumesAction(), "a migrated Slimy Eye must apply successfully");
            helper.assertTrue(EyeState.readProperties(cow).equals(expected),
                    "the mob must receive the converted appearance");
            helper.assertTrue(migrated.getCount() == 1, "a successful application must consume one item");
        } finally {
            ServerEyeConfigs.replaceAll(originalConfigs);
            ServerConfig.GOOGLY_EYES_ENABLED.set(originalEnabled);
            player.getAbilities().instabuild = originalInstabuild;
            EyeState.disableAndClearProperties(cow);
        }
        helper.succeed();
    }

    public static void chestContentsMigrateOnLoad(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        AppearanceOverride expected = AppearanceOverride.EMPTY.withGlow(true);
        BlockPos pos = new BlockPos(1, 1, 1);
        ChestBlockEntity source = new ChestBlockEntity(pos, Blocks.CHEST.defaultBlockState());
        source.setItem(0, legacyStack(ModItems.GOOGLY_EYE.get(), expected, 4));

        CompoundTag saved = source.saveWithFullMetadata(registries);
        BlockEntity loaded = BlockEntity.loadStatic(pos, Blocks.CHEST.defaultBlockState(), saved, registries);
        helper.assertTrue(loaded instanceof ChestBlockEntity, "the saved chest must load as a chest");
        ItemStack migrated = ((ChestBlockEntity) loaded).getItem(0);
        helper.assertTrue(migrated.getCount() == 4 && EyeItemProperties.get(migrated).equals(expected),
                "an eye in a chest must migrate through the chest loading path");
        assertLegacyAbsent(helper, migrated);
        helper.succeed();
    }

    public static void nestedContainerContentsMigrateOnLoad(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        AppearanceOverride expected = AppearanceOverride.EMPTY
                .withIrisColor(new EyeColor(0.6F, 0.4F, 0.2F));
        ItemStack inner = legacyStack(ModItems.SLIMY_EYE.get(), expected, 2);
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(inner)));

        ItemStack loadedShulker = decode(shulker, registries);
        ItemContainerContents contents = loadedShulker.getOrDefault(
                DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        ItemStack migrated = contents.nonEmptyItems().iterator().next();
        helper.assertTrue(migrated.getCount() == 2 && EyeItemProperties.get(migrated).equals(expected),
                "an eye nested in an item container must migrate when the outer stack decodes");
        assertLegacyAbsent(helper, migrated);
        helper.succeed();
    }
}
