package com.github.crittscott.somegoogly.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.Objects;
import java.util.function.Supplier;

/** Loader-neutral registration boundary for the mod's four content registries. */
public interface ContentRegistrar {

    <T extends Item> Supplier<T> registerItem(String name, Supplier<T> factory);

    <T> Supplier<DataComponentType<T>> registerDataComponent(
            String name, Supplier<DataComponentType<T>> factory);

    <T extends RecipeSerializer<?>> Supplier<T> registerRecipeSerializer(
            String name, Supplier<T> factory);

    Supplier<CreativeModeTab> registerCreativeTab(
            String name,
            Component title,
            Supplier<ItemStack> icon,
            CreativeModeTab.DisplayItemsGenerator displayItems);

    /** Stable common handle bound once to the supplier owned by the active loader. */
    final class Handle<T> implements Supplier<T> {
        private Supplier<? extends T> delegate;

        public void bind(Supplier<? extends T> supplier) {
            if (delegate != null) {
                throw new IllegalStateException("Registry handle is already bound");
            }
            delegate = Objects.requireNonNull(supplier, "supplier");
        }

        @Override
        public T get() {
            if (delegate == null) {
                throw new IllegalStateException("Registry handle has not been bound");
            }
            return delegate.get();
        }
    }
}
