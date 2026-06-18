package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.client.GooglyEyeItemRenderer;
import com.github.crittscott.somegoogly.state.EyeProperties;
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
 * A single eye, as an item. Its appearance lives in stack NBT as {@link EyeProperties} (the same
 * codec-serialized schema a mob carries), so it round-trips through drops and — once recipe
 * serializers exist — through crafting.
 *
 * <p>Geometry is intentionally NOT stored here: an eye item is placement-independent. Attaching it to
 * a mob reuses the mob's configured placement (see {@link EyeProperties}).
 */
public class GooglyEyeItem extends Item {

    public static final String TAG_EYE_PROPERTIES = "EyeProperties";

    public GooglyEyeItem(Properties properties) {
        super(properties);
    }

    public static EyeProperties getProperties(ItemStack stack) {
        return EyeProperties.fromNbt(stack.getTagElement(TAG_EYE_PROPERTIES));
    }

    public static void setProperties(ItemStack stack, EyeProperties properties) {
        if (properties.isEmpty()) {
            CompoundTag tag = stack.getTag();
            if (tag != null) {
                tag.remove(TAG_EYE_PROPERTIES);
            }
        } else {
            stack.getOrCreateTag().put(TAG_EYE_PROPERTIES, properties.toNbt());
        }
    }

    /** A new eye-item stack carrying {@code properties}. */
    public static ItemStack create(EyeProperties properties, int count) {
        ItemStack stack = new ItemStack(ModItems.GOOGLY_EYE.get(), count);
        setProperties(stack, properties);
        return stack;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        // Render the item as the real 3D eye model (tinted by its EyeProperties, googly when held).
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

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        EyeProperties props = getProperties(stack);
        props.irisColor().ifPresent(rgb ->
                tooltip.add(Component.literal("Iris: #" + String.format("%06X", rgb)).withStyle(ChatFormatting.GRAY)));
        props.corneaColor().ifPresent(rgb ->
                tooltip.add(Component.literal("Cornea: #" + String.format("%06X", rgb)).withStyle(ChatFormatting.GRAY)));
        props.glow().ifPresent(glow ->
                tooltip.add(Component.literal("Glow: " + glow).withStyle(ChatFormatting.GRAY)));
    }
}
