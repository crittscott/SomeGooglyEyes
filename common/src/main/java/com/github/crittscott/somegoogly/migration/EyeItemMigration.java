package com.github.crittscott.somegoogly.migration;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Converts eye items saved by the previous released build (Minecraft 1.20.1) as they load. That build
 * stored the appearance as an {@value #RELEASED_KEY} NBT compound, which Minecraft moves into the stack's
 * custom data; this decodes it into the appearance component and removes the key. A current component is
 * authoritative. A payload that does not decode stays in custom data and is logged, so it is never
 * silently discarded.
 */
public final class EyeItemMigration {

    public static final String RELEASED_KEY = "EyeProperties";

    private EyeItemMigration() {
    }

    /** Called from the eye items' {@code verifyComponentsAfterLoad}. */
    public static void migrate(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.contains(RELEASED_KEY)) {
            return;
        }

        CompoundTag customTag = customData.copyTag();
        if (stack.has(ModContent.EYE_PROPERTIES.get())) {
            removeReleasedKey(stack, customTag);
            return;
        }

        Tag payload = customTag.get(RELEASED_KEY);
        AppearanceOverride properties = payload instanceof CompoundTag
                ? AppearanceOverride.CODEC.parse(NbtOps.INSTANCE, payload).result().orElse(null)
                : null;
        if (properties == null) {
            SomeGooglyCommon.LOGGER.warn("Kept unrecognized released eye data on {} in its custom data under {}: {}",
                    stack.getItem(), RELEASED_KEY, payload);
            return;
        }

        EyeItemProperties.set(stack, properties);
        removeReleasedKey(stack, customTag);
    }

    private static void removeReleasedKey(ItemStack stack, CompoundTag customTag) {
        customTag.remove(RELEASED_KEY);
        if (customTag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customTag));
        }
    }
}
