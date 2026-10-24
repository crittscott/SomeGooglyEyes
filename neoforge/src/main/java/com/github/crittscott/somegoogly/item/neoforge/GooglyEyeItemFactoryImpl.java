package com.github.crittscott.somegoogly.item.neoforge;

import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import net.minecraft.world.item.Item;

/**
 * NeoForge implementation of {@link com.github.crittscott.somegoogly.item.GooglyEyeItemFactory}. No
 * subclass is needed: NeoForge attaches the 3D held-item renderer through
 * {@code RegisterClientExtensionsEvent} (see {@code NeoForgeClient}), not on the item instance itself.
 */
public final class GooglyEyeItemFactoryImpl {

    private GooglyEyeItemFactoryImpl() {
    }

    public static GooglyEyeItem create(Item.Properties properties) {
        return new GooglyEyeItem(properties);
    }
}
