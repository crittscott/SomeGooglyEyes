package com.github.crittscott.somegoogly.item.neoforge;

import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import net.minecraft.world.item.Item;

/** NeoForge construction boundary for the registered Googly Eye item. */
public final class GooglyEyeItemFactoryImpl {

    private GooglyEyeItemFactoryImpl() {
    }

    public static GooglyEyeItem create(Item.Properties properties) {
        return new GooglyEyeItem(properties);
    }
}
