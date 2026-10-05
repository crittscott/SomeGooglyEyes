package com.github.crittscott.somegoogly.item;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.item.Item;

/**
 * Constructs the registered {@link GooglyEyeItem} instance. Its 3D held-item renderer is attached per
 * loader: Forge overrides {@code Item#initializeClient} (added by its patched {@code Item}) on the item
 * instance, while NeoForge ({@code RegisterClientExtensionsEvent}) and Fabric
 * ({@code BuiltinItemRendererRegistry}) register the renderer separately at client init with no change
 * to the item class.
 */
public final class GooglyEyeItemFactory {

    private GooglyEyeItemFactory() {
    }

    @ExpectPlatform
    public static GooglyEyeItem create(Item.Properties properties) {
        throw new AssertionError();
    }
}
