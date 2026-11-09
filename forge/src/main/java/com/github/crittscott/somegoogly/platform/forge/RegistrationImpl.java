package com.github.crittscott.somegoogly.platform.forge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

/** Forge implementation of the shared registration boundary, through deferred registers. */
public final class RegistrationImpl {

    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SomeGooglyCommon.MOD_ID);
    private static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(Registries.ENCHANTMENT, SomeGooglyCommon.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, SomeGooglyCommon.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, SomeGooglyCommon.MOD_ID);

    private RegistrationImpl() {
    }

    /** Attach every deferred register to the mod bus; called once from the mod constructor. */
    public static void attach(IEventBus modBus) {
        CREATIVE_TABS.register(modBus);
        ENCHANTMENTS.register(modBus);
        ITEMS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
    }

    public static Supplier<CreativeModeTab> creativeTab(String name, Component title, Supplier<ItemStack> icon,
                                                        CreativeModeTab.DisplayItemsGenerator displayItems) {
        return CREATIVE_TABS.register(name, () -> CreativeModeTab.builder()
                .title(title)
                .icon(icon)
                .displayItems(displayItems)
                .build());
    }

    public static <T extends Enchantment> Supplier<T> enchantment(String name, Supplier<T> factory) {
        return ENCHANTMENTS.register(name, factory);
    }

    public static <T extends Item> Supplier<T> item(String name, Function<Item.Properties, T> factory) {
        return ITEMS.register(name, () -> factory.apply(new Item.Properties()));
    }

    public static <T extends RecipeSerializer<?>> Supplier<T> recipeSerializer(String name, Supplier<T> factory) {
        return RECIPE_SERIALIZERS.register(name, factory);
    }
}
