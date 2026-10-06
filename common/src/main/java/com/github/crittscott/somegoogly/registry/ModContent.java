package com.github.crittscott.somegoogly.registry;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.item.SlimyEyeItem;
import com.github.crittscott.somegoogly.recipe.EyeModifierRecipe;
import com.github.crittscott.somegoogly.recipe.SlimyEyeRecipe;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/**
 * Everything the mod registers, declared once and bound through each loader's {@link ContentRegistrar}:
 * the Googly Eye (an ingredient) and the Slimy Eye it crafts into (the applicator), the appearance
 * component both carry, the mod's creative tab, and the two recipe serializers. The Optometrist
 * enchantment is data-driven, so only its resource key lives here.
 */
public final class ModContent {

    public static final ContentRegistrar.Handle<GooglyEyeItem> GOOGLY_EYE = new ContentRegistrar.Handle<>();
    public static final ContentRegistrar.Handle<SlimyEyeItem> SLIMY_EYE = new ContentRegistrar.Handle<>();

    public static final ContentRegistrar.Handle<DataComponentType<AppearanceOverride>> EYE_PROPERTIES =
            new ContentRegistrar.Handle<>();

    /**
     * The mod's own creative tab, icon'd with the googly eye. It gathers everything the mod adds — the
     * eye, the slimy eye that applies it, and an Optometrist book — in one place rather than scattered
     * across vanilla tabs.
     */
    public static final ContentRegistrar.Handle<CreativeModeTab> CREATIVE_TAB = new ContentRegistrar.Handle<>();

    public static final ContentRegistrar.Handle<CustomRecipe.Serializer<EyeModifierRecipe>> EYE_MODIFIER_RECIPE =
            new ContentRegistrar.Handle<>();
    public static final ContentRegistrar.Handle<SlimyEyeRecipe.Serializer> SLIMY_EYE_RECIPE =
            new ContentRegistrar.Handle<>();

    public static final ResourceKey<Enchantment> OPTOMETRIST = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "optometrist"));

    private ModContent() {
    }

    /** Bind every handle; the data component first, since the items' stacks carry it. */
    public static void register(ContentRegistrar registrar) {
        EYE_PROPERTIES.bind(registrar.registerDataComponent(
                "eye_properties", () -> DataComponentType.<AppearanceOverride>builder()
                        .persistent(AppearanceOverride.CODEC)
                        .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(AppearanceOverride.CODEC))
                        .build()));

        GOOGLY_EYE.bind(registrar.registerItem("googly_eye", GooglyEyeItem::new));
        SLIMY_EYE.bind(registrar.registerItem("slimy_eye", SlimyEyeItem::new));

        CREATIVE_TAB.bind(registrar.registerCreativeTab(
                "googly",
                Component.translatable("itemGroup." + SomeGooglyCommon.MOD_ID),
                () -> new ItemStack(GOOGLY_EYE.get()),
                (params, output) -> {
                    output.accept(GOOGLY_EYE.get());
                    output.accept(SLIMY_EYE.get());
                    output.accept(optometristBook(params.holders()));
                }));

        EYE_MODIFIER_RECIPE.bind(registrar.registerRecipeSerializer(
                "eye_modifier", () -> new CustomRecipe.Serializer<>(EyeModifierRecipe::new)));
        SLIMY_EYE_RECIPE.bind(registrar.registerRecipeSerializer(
                "slimy_eye", SlimyEyeRecipe.Serializer::new));
    }

    /** An enchanted book holding {@link #OPTOMETRIST} at its max level. */
    private static ItemStack optometristBook(HolderLookup.Provider registries) {
        Holder<Enchantment> optometrist = registries.lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(OPTOMETRIST);
        return EnchantmentHelper.createBook(new EnchantmentInstance(optometrist, 1));
    }
}
