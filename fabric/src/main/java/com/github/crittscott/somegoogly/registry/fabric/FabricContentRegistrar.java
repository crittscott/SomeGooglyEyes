package com.github.crittscott.somegoogly.registry.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.registry.ContentRegistrar;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.function.Function;
import java.util.function.Supplier;

/** Fabric registration through vanilla registries during the mod initializer. */
public final class FabricContentRegistrar implements ContentRegistrar {

    @Override
    public <T extends Item> Supplier<T> registerItem(String name, Function<Item.Properties, T> factory) {
        T value = factory.apply(new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM, id(name), value);
        return () -> value;
    }

    @Override
    public <T extends Enchantment> Supplier<T> registerEnchantment(String name, Supplier<T> factory) {
        T value = factory.get();
        Registry.register(BuiltInRegistries.ENCHANTMENT, id(name), value);
        return () -> value;
    }

    @Override
    public <T extends RecipeSerializer<?>> Supplier<T> registerRecipeSerializer(
            String name, Supplier<T> factory) {
        T value = factory.get();
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id(name), value);
        return () -> value;
    }

    @Override
    public Supplier<CreativeModeTab> registerCreativeTab(
            String name,
            Component title,
            Supplier<ItemStack> icon,
            CreativeModeTab.DisplayItemsGenerator displayItems) {
        CreativeModeTab value = FabricItemGroup.builder()
                .title(title)
                .icon(icon)
                .displayItems(displayItems)
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id(name), value);
        return () -> value;
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(SomeGooglyCommon.MOD_ID, name);
    }
}
