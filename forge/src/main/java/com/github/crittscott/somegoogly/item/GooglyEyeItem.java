package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.client.GooglyEyeItemRenderer;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * A single eye, as an item. Its appearance lives in stack NBT as {@link AppearanceOverride} (see
 * {@link EyeItemProperties}), so it round-trips through drops and crafting.
 *
 * <p>Geometry is intentionally NOT stored here: an eye item is placement-independent. Attaching it to
 * a mob reuses the mob's configured placement (see {@link AppearanceOverride}).
 *
 * <p>This item is an ingredient, not an applicator: crafting it with a slimeball yields a
 * {@link SlimyEyeItem}, which is what actually sticks eyes onto a mob.
 */
public class GooglyEyeItem extends Item {

    public GooglyEyeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        EyeItemProperties.appendTooltip(stack, tooltip);
    }

    /** A new eye-item stack carrying {@code properties}. */
    public static ItemStack create(AppearanceOverride properties, int count) {
        ItemStack stack = new ItemStack(ModItems.GOOGLY_EYE.get(), count);
        EyeItemProperties.set(stack, properties);
        return stack;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        // Render the item as the real 3D eye model (tinted by its AppearanceOverride, googly when held).
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
}
