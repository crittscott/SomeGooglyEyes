package com.github.crittscott.somegoogly.item.fabric;

import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import net.minecraft.world.item.Item;

/**
 * Fabric implementation of {@link com.github.crittscott.somegoogly.item.GooglyEyeItemFactory}. No
 * subclass is needed: Fabric's item-renderer registration happens separately at client init
 * ({@code BuiltinItemRendererRegistry}), not on the item instance itself.
 */
public final class GooglyEyeItemFactoryImpl {

    private GooglyEyeItemFactoryImpl() {
    }

    public static GooglyEyeItem create(Item.Properties properties) {
        return new GooglyEyeItem(properties);
    }
}
