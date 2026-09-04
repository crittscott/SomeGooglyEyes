package com.github.crittscott.somegoogly.item;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.item.Item;

/**
 * Constructs the registered {@link GooglyEyeItem} instance. Its 3D held-item renderer is attached here
 * rather than on {@code GooglyEyeItem} itself, since each loader's renderer-attachment hook differs:
 * NeoForge and Forge override a method on the item instance ({@code Item#initializeClient}, added by
 * their patched {@code Item}), while Fabric registers the renderer separately at client init
 * ({@code BuiltinItemRendererRegistry}) with no change to the item class needed at all.
 */
public final class GooglyEyeItemFactory {

    private GooglyEyeItemFactory() {
    }

    @ExpectPlatform
    public static GooglyEyeItem create(Item.Properties properties) {
        throw new AssertionError();
    }
}
