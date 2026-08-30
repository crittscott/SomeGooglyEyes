package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/**
 * Item registry for the mod: the {@link GooglyEyeItem} (the eye itself, an ingredient) and the
 * {@link SlimyEyeItem} it crafts into (the applicator). Both carry the same appearance component.
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(SomeGooglyCommon.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<GooglyEyeItem> GOOGLY_EYE =
            ITEMS.register("googly_eye", () -> GooglyEyeItemFactory.create(new Item.Properties()));

    public static final RegistrySupplier<SlimyEyeItem> SLIMY_EYE =
            ITEMS.register("slimy_eye", () -> new SlimyEyeItem(new Item.Properties()));

    private ModItems() {
    }

    /** Both items are shown in the mod's own creative tab ({@code ModCreativeTabs}), not a vanilla one. */
    public static void register() {
        ITEMS.register();
    }
}
