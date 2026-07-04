package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.client.GooglyEyeItemRenderer;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
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
 * A single eye, as an item. Its appearance lives in stack NBT as {@link AppearanceOverride} (the same
 * codec-serialized schema a mob carries), so it round-trips through drops and — once recipe
 * serializers exist — through crafting.
 *
 * <p>Geometry is intentionally NOT stored here: an eye item is placement-independent. Attaching it to
 * a mob reuses the mob's configured placement (see {@link AppearanceOverride}).
 */
public class GooglyEyeItem extends Item {

    public static final String TAG_EYE_PROPERTIES = "EyeProperties";

    public GooglyEyeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        AppearanceOverride props = getProperties(stack);
        props.iris().ifPresent(color ->
                tooltip.add(Component.literal("Iris: #" + String.format("%06X", color.toRgb24())).withStyle(ChatFormatting.GRAY)));
        props.cornea().ifPresent(color ->
                tooltip.add(Component.literal("Cornea: #" + String.format("%06X", color.toRgb24())).withStyle(ChatFormatting.GRAY)));
        props.glow().ifPresent(glow ->
                tooltip.add(Component.literal("Glow: " + glow).withStyle(ChatFormatting.GRAY)));
    }

    /** A new eye-item stack carrying {@code properties}. */
    public static ItemStack create(AppearanceOverride properties, int count) {
        ItemStack stack = new ItemStack(ModItems.GOOGLY_EYE.get(), count);
        setProperties(stack, properties);
        return stack;
    }

    public static AppearanceOverride getProperties(ItemStack stack) {
        return AppearanceOverride.fromNbt(stack.getTagElement(TAG_EYE_PROPERTIES));
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

    public static void setProperties(ItemStack stack, AppearanceOverride properties) {
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
