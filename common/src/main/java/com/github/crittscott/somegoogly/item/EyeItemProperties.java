package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
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

    /**
     * Tint index of the iris layer in {@code models/item/slimy_eye.json} ({@code layer2}); every
     * loader's Slimy Eye color handler keys the iris tint on it. Must match that model file.
     */
    public static final int SLIMY_EYE_IRIS_TINT_INDEX = 2;

    private EyeItemProperties() {
    }

    /**
     * The Slimy Eye {@code ItemColor} body shared by all three loaders: the stored iris color for the
     * iris layer, {@code -1} (no tint) for every other layer.
     */
    public static int slimyEyeTint(ItemStack stack, int tintIndex) {
        return tintIndex == SLIMY_EYE_IRIS_TINT_INDEX
                ? get(stack).iris().orElse(EyeColor.BLACK).toRgb24()
                : -1;
    }

    /** The tooltip lines describing {@code stack}'s appearance, appended in place. */
    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        AppearanceOverride props = get(stack);
        props.iris().ifPresent(color ->
                tooltip.add(Component.translatable("somegoogly.tooltip.iris", color.toHex()).withStyle(ChatFormatting.GRAY)));
        props.cornea().ifPresent(color ->
                tooltip.add(Component.translatable("somegoogly.tooltip.cornea", color.toHex()).withStyle(ChatFormatting.GRAY)));
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
