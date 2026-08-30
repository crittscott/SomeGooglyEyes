package com.github.crittscott.somegoogly.item.neoforge;

import com.github.crittscott.somegoogly.client.GooglyEyeItemRenderer;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/** NeoForge construction boundary for the registered Googly Eye item. */
public final class GooglyEyeItemFactoryImpl {

    private GooglyEyeItemFactoryImpl() {
    }

    public static GooglyEyeItem create(Item.Properties properties) {
        return new GooglyEyeItem(properties) {
            @Override
            public void initializeClient(Consumer<IClientItemExtensions> consumer) {
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
