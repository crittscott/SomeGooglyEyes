package com.github.crittscott.somegoogly.item.forge;

import com.github.crittscott.somegoogly.client.GooglyEyeItemRenderer;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** Forge implementation of {@link com.github.crittscott.somegoogly.item.GooglyEyeItemFactory}. */
public final class GooglyEyeItemFactoryImpl {

    private GooglyEyeItemFactoryImpl() {
    }

    public static GooglyEyeItem create(Item.Properties properties) {
        return new GooglyEyeItem(properties) {
            @Override
            public void initializeClient(Consumer<IClientItemExtensions> consumer) {
                // Render the item as the real 3D eye model (tinted by its AppearanceOverride, googly when held).
                consumer.accept(new IClientItemExtensions() {
                    private GooglyEyeItemRenderer renderer;

                    @Override
                    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                        if (renderer == null) {
                            renderer = new GooglyEyeItemRenderer();
                        }
                        return renderer;
                    }
                });
            }
        };
    }
}
