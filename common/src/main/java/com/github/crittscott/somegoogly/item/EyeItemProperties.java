package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The component-backed appearance carried by an eye-bearing item. Shared by {@link GooglyEyeItem}
 * and {@link SlimyEyeItem} so the eye's look survives unchanged as it is crafted from one into the
 * other and finally applied to a mob. Item-to-mob transfer is a straight immutable value copy.
 */
public final class EyeItemProperties {

    private EyeItemProperties() {
    }

    /** The tooltip lines describing {@code stack}'s appearance, appended in place. */
    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        AppearanceOverride props = get(stack);
        props.iris().ifPresent(color ->
                tooltip.add(Component.translatable("somegoogly.tooltip.iris", String.format("%06X", color.toRgb24())).withStyle(ChatFormatting.GRAY)));
        props.cornea().ifPresent(color ->
                tooltip.add(Component.translatable("somegoogly.tooltip.cornea", String.format("%06X", color.toRgb24())).withStyle(ChatFormatting.GRAY)));
        props.glow().ifPresent(glow ->
                tooltip.add(Component.translatable("somegoogly.tooltip.glow",
                        Component.translatable(glow ? "somegoogly.value.on" : "somegoogly.value.off"))
                        .withStyle(ChatFormatting.GRAY)));
    }

    public static AppearanceOverride get(ItemStack stack) {
        AppearanceOverride properties =
                stack.getOrDefault(ModDataComponents.EYE_PROPERTIES.get(), AppearanceOverride.EMPTY);
        return properties.isValid() ? properties : AppearanceOverride.EMPTY;
    }

    public static void set(ItemStack stack, AppearanceOverride properties) {
        if (properties.isEmpty()) {
            stack.remove(ModDataComponents.EYE_PROPERTIES.get());
        } else {
            stack.set(ModDataComponents.EYE_PROPERTIES.get(), properties);
        }
    }
}
