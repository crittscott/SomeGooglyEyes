package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * The component-backed appearance carried by an eye-bearing item. Shared by {@link GooglyEyeItem}
 * and {@link SlimyEyeItem} so the eye's look survives unchanged as it is crafted from one into the
 * other and finally applied to a mob. Item-to-mob transfer is a straight immutable value copy.
 */
public final class EyeItemProperties {

    private static final String LEGACY_PROPERTIES_KEY = "EyeProperties";

    /**
     * Tint index of the iris layer in {@code models/item/slimy_eye.json} ({@code layer2}); every
     * loader's Slimy Eye color handler keys the iris tint on it. Must match that model file.
     */
    public static final int SLIMY_EYE_IRIS_TINT_INDEX = 2;

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

    public static AppearanceOverride get(ItemStack stack) {
        AppearanceOverride properties =
                stack.getOrDefault(ModDataComponents.EYE_PROPERTIES.get(), AppearanceOverride.EMPTY);
        return properties.isValid() ? properties : AppearanceOverride.EMPTY;
    }

    /**
     * Converts the released 1.20.1 eye payload after Minecraft has moved it into custom data. A
     * current component is authoritative; malformed old data is retained so a later recovery remains
     * possible.
     */
    public static void migrateStoredAppearance(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(LEGACY_PROPERTIES_KEY)) {
            return;
        }

        CompoundTag customTag = customData.copyTag();
        if (stack.has(ModDataComponents.EYE_PROPERTIES.get())) {
            removeLegacyProperties(stack, customTag);
            return;
        }

        Tag payload = customTag.get(LEGACY_PROPERTIES_KEY);
        if (!(payload instanceof CompoundTag)) {
            return;
        }
        AppearanceOverride properties = AppearanceOverride.CODEC.parse(NbtOps.INSTANCE, payload)
                .result().orElse(null);
        if (properties == null || !properties.isValid()) {
            return;
        }

        set(stack, properties);
        removeLegacyProperties(stack, customTag);
    }

    private static void removeLegacyProperties(ItemStack stack, CompoundTag customTag) {
        customTag.remove(LEGACY_PROPERTIES_KEY);
        if (customTag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customTag));
        }
    }

    public static void set(ItemStack stack, AppearanceOverride properties) {
        if (properties.isEmpty()) {
            stack.remove(ModDataComponents.EYE_PROPERTIES.get());
        } else {
            stack.set(ModDataComponents.EYE_PROPERTIES.get(), properties);
        }
    }
}
