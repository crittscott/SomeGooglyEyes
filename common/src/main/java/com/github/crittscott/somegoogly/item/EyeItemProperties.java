package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.registry.ModContent;
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

    /** The Slimy Eye's iris-layer tint: the stored iris color (black when unset) as opaque ARGB. */
    public static int slimyEyeIrisColor(ItemStack stack) {
        return get(stack).iris().orElse(EyeColor.BLACK).toOpaqueArgb32();
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

    /** The stack's appearance override, or {@link AppearanceOverride#EMPTY} when it has none. */
    public static AppearanceOverride get(ItemStack stack) {
        return stack.getOrDefault(ModContent.EYE_PROPERTIES.get(), AppearanceOverride.EMPTY);
    }

    /** Store {@code properties} on the stack; an empty override removes the component. */
    public static void set(ItemStack stack, AppearanceOverride properties) {
        if (properties.isEmpty()) {
            stack.remove(ModContent.EYE_PROPERTIES.get());
        } else {
            stack.set(ModContent.EYE_PROPERTIES.get(), properties);
        }
    }
}
