package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A googly eye bedded in a slimeball: the applicator. Right-clicking a living entity with it sticks
 * eyes on that entity, using the appearance the eye carried into the craft
 * ({@link EyeItemProperties}); sneak + use applies it to the player themselves, which is the only way
 * a player gets their own eyes (they are excluded from the at-spawn roll).
 *
 * <p>The slimy eye carries appearance only, never placement — where the eyes land comes from the
 * target's datapack config, exactly as it does for a mob that rolled eyes at spawn. Applying to an
 * already-eyed target recolors it rather than stacking a second set.
 *
 * <p>One eye in, one eye out: an application consumes a single slimy eye and a harvest yields a
 * single eye item, so a craft-apply-harvest loop costs a slimeball per turn and cannot multiply eyes.
 *
 * <p>The mob-apply verb ({@link #applyToTarget}) is dispatched from {@code EyeItemInteractions}'s
 * entity-interact handler — which claims the right-click before the target's own interaction can
 * consume it — not through {@code Item#interactLivingEntity}. Only the sneak self-apply
 * ({@link #use}) is dispatched through this class.
 */
public class SlimyEyeItem extends Item {

    public SlimyEyeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        EyeItemProperties.appendTooltip(stack, tooltip);
    }

    /**
     * Give {@code target} this stack's eyes, consuming one. Callers have already established that the
     * target is eligible; this is the shared body of the mob and self paths.
     */
    private static void apply(ItemStack stack, Player player, LivingEntity target) {
        EyeState.setProperties(target, EyeItemProperties.get(stack));
        EyeState.setHasEyes(target, true);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    /** A new slimy-eye stack carrying {@code properties}. */
    public static ItemStack create(AppearanceOverride properties, int count) {
        ItemStack stack = new ItemStack(ModItems.SLIMY_EYE.get(), count);
        EyeItemProperties.set(stack, properties);
        return stack;
    }

    /**
     * The mob-apply verb, server side: gate on the shared eligibility predicate, then give
     * {@code target} the stack's eyes, consuming one. {@code FAIL} refuses without consuming.
     */
    public static InteractionResult applyToTarget(ItemStack stack, Player player, LivingEntity target) {
        if (!ServerEyeConfigs.isEligible(target)) {
            return InteractionResult.FAIL;
        }
        apply(stack, player, target);
        return InteractionResult.SUCCESS;
    }

    /** Sneak + use: eye yourself. Without the sneak this would fire on every stray right-click. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide()) {
            return InteractionResultHolder.success(stack);
        }
        if (!ServerEyeConfigs.isEligible(player)) {
            return InteractionResultHolder.fail(stack);
        }
        apply(stack, player, player);
        return InteractionResultHolder.consume(stack);
    }
}
