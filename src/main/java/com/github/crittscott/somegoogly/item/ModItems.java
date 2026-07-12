package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.SomeGoogly;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Item registry for the mod: the {@link GooglyEyeItem} (the eye itself, an ingredient) and the
 * {@link SlimeyEyeItem} it crafts into (the applicator). Both carry the same appearance NBT.
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SomeGoogly.MOD_ID);

    public static final RegistryObject<GooglyEyeItem> GOOGLY_EYE =
            ITEMS.register("googly_eye", () -> new GooglyEyeItem(new Item.Properties()));

    public static final RegistryObject<SlimeyEyeItem> SLIMEY_EYE =
            ITEMS.register("slimey_eye", () -> new SlimeyEyeItem(new Item.Properties()));

    private ModItems() {
    }

    /** Wire on the mod event bus (from {@code SomeGoogly}). Both items are shown in the mod's own
     * creative tab ({@code ModCreativeTabs}), not a vanilla one. */
    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
