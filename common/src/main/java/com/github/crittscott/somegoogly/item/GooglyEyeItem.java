package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A single eye, as an item. Its appearance lives in a stack data component as
 * {@link AppearanceOverride} (see {@link EyeItemProperties}), so it round-trips through drops and
 * crafting.
 *
 * <p>Geometry is intentionally NOT stored here: an eye item is placement-independent. Attaching it to
 * a mob reuses the mob's configured placement (see {@link AppearanceOverride}).
 *
 * <p>This item is an ingredient, not an applicator: crafting it with a slimeball yields a
 * {@link SlimyEyeItem}, which is what actually sticks eyes onto a mob. The Slimy Eye extends this class
 * for the stored appearance and tooltip.
 *
 * <p>Its 3D look comes from its item definition, which selects the {@code GooglyEyeItemRenderer}
 * special model, not from this class.
 */
public class GooglyEyeItem extends Item {

    public GooglyEyeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        EyeItemProperties.appendTooltip(stack, tooltip);
    }

    /** A new eye-item stack carrying {@code properties}. */
    public static ItemStack create(AppearanceOverride properties, int count) {
        ItemStack stack = new ItemStack(ModContent.GOOGLY_EYE.get(), count);
        EyeItemProperties.set(stack, properties);
        return stack;
    }
}
