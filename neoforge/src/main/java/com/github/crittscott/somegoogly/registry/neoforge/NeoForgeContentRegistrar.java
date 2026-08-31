package com.github.crittscott.somegoogly.registry.neoforge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.registry.ContentRegistrar;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** NeoForge deferred registration for common content definitions. */
public final class NeoForgeContentRegistrar implements ContentRegistrar {

    private final DeferredRegister<Item> items =
            DeferredRegister.create(Registries.ITEM, SomeGooglyCommon.MOD_ID);
    private final DeferredRegister<DataComponentType<?>> dataComponents =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, SomeGooglyCommon.MOD_ID);
    private final DeferredRegister<RecipeSerializer<?>> recipeSerializers =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, SomeGooglyCommon.MOD_ID);
    private final DeferredRegister<CreativeModeTab> creativeTabs =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SomeGooglyCommon.MOD_ID);

    @Override
    public <T extends Item> Supplier<T> registerItem(String name, Supplier<T> factory) {
        return items.register(name, factory);
    }

    @Override
    public <T> Supplier<DataComponentType<T>> registerDataComponent(
            String name, Supplier<DataComponentType<T>> factory) {
        return dataComponents.register(name, factory);
    }

    @Override
    public <T extends RecipeSerializer<?>> Supplier<T> registerRecipeSerializer(
            String name, Supplier<T> factory) {
        return recipeSerializers.register(name, factory);
    }

    @Override
    public Supplier<CreativeModeTab> registerCreativeTab(
            String name,
            Component title,
            Supplier<ItemStack> icon,
            CreativeModeTab.DisplayItemsGenerator displayItems) {
        return creativeTabs.register(name, () -> CreativeModeTab.builder()
                .title(title)
                .icon(icon)
                .displayItems(displayItems)
                .build());
    }

    public void register(IEventBus modBus) {
        dataComponents.register(modBus);
        items.register(modBus);
        creativeTabs.register(modBus);
        recipeSerializers.register(modBus);
    }
}
