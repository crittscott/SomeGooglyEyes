package com.github.crittscott.somegoogly.platform.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import java.util.function.Function;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Fabric implementation of the shared registration boundary, directly into the vanilla registries. */
public final class RegistrationImpl {

    private RegistrationImpl() {
    }

    public static Supplier<CreativeModeTab> creativeTab(String name, Component title, Supplier<ItemStack> icon,
                                                        CreativeModeTab.DisplayItemsGenerator displayItems) {
        CreativeModeTab value = FabricCreativeModeTab.builder()
                .title(title)
                .icon(icon)
                .displayItems(displayItems)
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SomeGooglyCommon.id(name), value);
        return () -> value;
    }

    public static <T> Supplier<DataComponentType<T>> dataComponent(String name,
                                                                  Supplier<DataComponentType<T>> factory) {
        DataComponentType<T> value = factory.get();
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, SomeGooglyCommon.id(name), value);
        return () -> value;
    }

    public static <T extends Item> Supplier<T> item(String name, Function<Item.Properties, T> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SomeGooglyCommon.id(name));
        T value = factory.apply(new Item.Properties().setId(key));
        Registry.register(BuiltInRegistries.ITEM, key, value);
        return () -> value;
    }

    public static <T extends RecipeSerializer<?>> Supplier<T> recipeSerializer(String name, Supplier<T> factory) {
        T value = factory.get();
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, SomeGooglyCommon.id(name), value);
        return () -> value;
    }
}
