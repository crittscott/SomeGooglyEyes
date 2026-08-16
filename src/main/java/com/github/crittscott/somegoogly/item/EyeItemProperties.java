package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The stack-NBT appearance carried by an eye-bearing item, as an {@link AppearanceOverride} under a
 * single tag key. Shared by {@link GooglyEyeItem} and {@link SlimyEyeItem} so the eye's look survives
 * unchanged as it is crafted from one into the other and finally applied to a mob — it is the same
 * codec-serialized schema a mob carries, so item↔mob transfer is a straight property copy.
 */
public final class EyeItemProperties {

    public static final String TAG_EYE_PROPERTIES = "EyeProperties";

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
                tooltip.add(Component.translatable("somegoogly.tooltip.glow", glow).withStyle(ChatFormatting.GRAY)));
    }

    public static AppearanceOverride get(ItemStack stack) {
        return AppearanceOverride.fromNbt(stack.getTagElement(TAG_EYE_PROPERTIES));
    }

    public static void set(ItemStack stack, AppearanceOverride properties) {
        if (properties.isEmpty()) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                tag.remove(TAG_EYE_PROPERTIES);
            }
        } else {
            stack.getOrCreateTag().put(TAG_EYE_PROPERTIES, properties.toNbt());
        }
    }
}
