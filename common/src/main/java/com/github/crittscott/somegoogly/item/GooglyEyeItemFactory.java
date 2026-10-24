package com.github.crittscott.somegoogly.item;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.item.Item;

/**
 * Constructs the registered {@link GooglyEyeItem} instance. Its 3D held-item renderer is attached here
 * rather than on {@code GooglyEyeItem} itself, since each loader's renderer-attachment hook differs:
 * Forge overrides a method on the item instance ({@code Item#initializeClient}, added by its patched
 * {@code Item}), while NeoForge ({@code RegisterClientExtensionsEvent}) and Fabric
 * ({@code BuiltinItemRendererRegistry}) register the renderer separately at client init.
 */
public final class GooglyEyeItemFactory {

    private GooglyEyeItemFactory() {
    }

    @ExpectPlatform
    public static GooglyEyeItem create(Item.Properties properties) {
        throw new AssertionError();
    }
}
