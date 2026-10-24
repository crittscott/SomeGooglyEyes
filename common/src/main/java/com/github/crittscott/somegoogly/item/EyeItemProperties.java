package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The appearance carried by an eye-bearing item, stored as the {@value #TAG_EYE_PROPERTIES} stack NBT
 * compound. Shared by {@link GooglyEyeItem}
 * and {@link SlimyEyeItem} so the eye's look survives unchanged as it is crafted from one into the
 * other and finally applied to a mob. Item-to-mob transfer is a straight immutable value copy.
 */
public final class EyeItemProperties {

    /**
     * Tint index of the iris layer in {@code models/item/slimy_eye.json} ({@code layer2}); every
     * loader's Slimy Eye color handler keys the iris tint on it. Must match that model file.
     */
    public static final int SLIMY_EYE_IRIS_TINT_INDEX = 2;

    public static final String TAG_EYE_PROPERTIES = "EyeProperties";

    private EyeItemProperties() {
    }

    /**
     * The Slimy Eye {@code ItemColor} body shared by all three loaders: the stored iris color for the
     * iris layer as opaque ARGB, {@code -1} (no tint) for every other layer.
     */
    public static int slimyEyeTint(ItemStack stack, int tintIndex) {
        return tintIndex == SLIMY_EYE_IRIS_TINT_INDEX
                ? get(stack).iris().orElse(EyeColor.BLACK).toOpaqueArgb32()
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

    /** The stack's appearance override, or {@link AppearanceOverride#EMPTY} when it has none. */
    public static AppearanceOverride get(ItemStack stack) {
        return AppearanceOverride.fromNbt(stack.getTagElement(TAG_EYE_PROPERTIES));
    }

    /** Store {@code properties} on the stack; an empty override removes the compound. */
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
