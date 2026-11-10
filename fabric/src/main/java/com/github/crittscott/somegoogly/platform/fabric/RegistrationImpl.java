package com.github.crittscott.somegoogly.platform.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
        CreativeModeTab value = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SomeGooglyCommon.id(name),
                FabricItemGroup.builder()
                        .title(title)
                        .icon(icon)
                        .displayItems(displayItems)
                        .build());
        return () -> value;
    }

    public static <T> Supplier<DataComponentType<T>> dataComponent(String name,
                                                                  Supplier<DataComponentType<T>> factory) {
        DataComponentType<T> value = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                SomeGooglyCommon.id(name), factory.get());
        return () -> value;
    }

    public static <T extends Item> Supplier<T> item(String name, Supplier<T> factory) {
        T value = Registry.register(BuiltInRegistries.ITEM, SomeGooglyCommon.id(name), factory.get());
        return () -> value;
    }

    public static <T extends RecipeSerializer<?>> Supplier<T> recipeSerializer(String name, Supplier<T> factory) {
        T value = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, SomeGooglyCommon.id(name), factory.get());
        return () -> value;
    }
}
