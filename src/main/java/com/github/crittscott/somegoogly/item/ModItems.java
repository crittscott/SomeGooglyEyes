package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.SomeGoogly;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Item registry for the mod. Currently just the single NBT-carrying {@link GooglyEyeItem}. */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SomeGoogly.MOD_ID);

    public static final RegistryObject<GooglyEyeItem> GOOGLY_EYE =
            ITEMS.register("googly_eye", () -> new GooglyEyeItem(new Item.Properties()));

    private ModItems() {
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(GOOGLY_EYE);
        }
    }

    /** Wire on the mod event bus (from {@code SomeGoogly}). */
    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(ModItems::addToCreativeTabs);
    }
}
