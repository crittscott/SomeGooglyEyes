package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.item.ModItems;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;

/**
 * Tints the slimey eye's iris layer from the appearance its stack carries, so the item in hand shows
 * the color the mob will end up with. The model stacks vanilla's slime ball (layer 0) and the white of
 * the eye (layer 1), both untinted; the iris disc is layer 2, drawn white so the tint reproduces the
 * color exactly.
 *
 * <p>The default matches {@code GooglyEyeItemRenderer}: an eye with no iris override is black.
 */
public final class SlimeyEyeColors {

    private SlimeyEyeColors() {
    }

    /** Wire on the mod event bus (from {@code SomeGoogly}, client side only). */
    public static void register(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 2
                        ? EyeItemProperties.get(stack).iris().orElse(EyeColor.BLACK).toRgb24()
                        : -1,
                ModItems.SLIMEY_EYE.get());
    }
}
