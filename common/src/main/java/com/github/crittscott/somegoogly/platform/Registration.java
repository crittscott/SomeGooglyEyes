package com.github.crittscott.somegoogly.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Registers the mod's content under its namespace the loader's way: Forge and NeoForge through deferred
 * registers, Fabric directly into the vanilla registries. Each method returns the registered value's supplier,
 * which is valid once the loader has run registration.
 */
public final class Registration {

    private Registration() {
    }

    /** Register a creative tab built from {@code title}, {@code icon}, and {@code displayItems}. */
    @ExpectPlatform
    public static Supplier<CreativeModeTab> creativeTab(String name, Component title, Supplier<ItemStack> icon,
                                                        CreativeModeTab.DisplayItemsGenerator displayItems) {
        throw new AssertionError();
    }

    /** Register a data component type. */
    @ExpectPlatform
    public static <T> Supplier<DataComponentType<T>> dataComponent(String name,
                                                                  Supplier<DataComponentType<T>> factory) {
        throw new AssertionError();
    }

    /**
     * Register an item. The factory receives properties already carrying the item's registry id, which
     * {@link Item} requires at construction.
     */
    @ExpectPlatform
    public static <T extends Item> Supplier<T> item(String name, Function<Item.Properties, T> factory) {
        throw new AssertionError();
    }

    /** Register a recipe serializer. */
    @ExpectPlatform
    public static <T extends RecipeSerializer<?>> Supplier<T> recipeSerializer(String name, Supplier<T> factory) {
        throw new AssertionError();
    }
}
