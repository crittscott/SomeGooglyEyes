package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;

/**
 * A googly eye bedded in a slimeball: the applicator. Right-clicking a living entity with it sticks
 * eyes on that entity, using the appearance the eye carried into the craft
 * ({@link EyeItemProperties}); sneak + use applies it to the player themselves, which is the only way
 * a player gets their own eyes (they are excluded from the at-spawn roll).
 *
 * <p>The slimy eye carries appearance only, never placement — where the eyes land comes from the
 * target's datapack config, on a placement variant freshly rolled by each application. An
 * already-eyed target refuses the application and consumes nothing, so recoloring an eyed mob means
 * harvesting its eye, modifying it, and re-applying.
 *
 * <p>One eye in, one eye out: an application consumes a single slimy eye and a harvest yields a
 * single eye item, so a craft-apply-harvest loop costs a slimeball per turn and cannot multiply eyes.
 *
 * <p>The mob-apply verb ({@link #applyToTarget}) is dispatched from each loader's entity-interact
 * adapter — which claims the right-click before the target's own interaction can consume it — not
 * through {@code Item#interactLivingEntity}. Only the sneak self-apply ({@link #use}) is dispatched
 * through this class.
 */
public class SlimyEyeItem extends Item {

    public SlimyEyeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        EyeItemProperties.appendTooltip(stack, tooltip);
    }

    /** A new slimy-eye stack carrying {@code properties}. */
    public static ItemStack create(AppearanceOverride properties, int count) {
        ItemStack stack = new ItemStack(ModItems.SLIMY_EYE.get(), count);
        EyeItemProperties.set(stack, properties);
        return stack;
    }

    /**
     * The apply verb, server side: an eyeless target passing the shared eligibility predicate gains
     * eyes carrying the stack's appearance on a freshly rolled placement variant, consuming one eye.
     * An already-eyed or ineligible target refuses ({@code FAIL}) and consumes nothing. Applying to
     * another player additionally requires server PvP to be enabled and {@code canHarmPlayer} to hold,
     * so it can't be used to restyle a teammate or anyone in a PvP-off world. Both the mob path
     * (the loader entity-interact adapter) and the sneak self-apply ({@link #use}) route through here.
     */
    public static InteractionResult applyToTarget(ItemStack stack, ServerPlayer player, LivingEntity target) {
        if (EyeState.hasEyes(target) || !ServerEyeConfigs.isEligible(target)) {
            return InteractionResult.FAIL;
        }
        if (target instanceof Player victim && victim != player) {
            if (!player.serverLevel().getServer().isPvpAllowed() || !player.canHarmPlayer(victim)) {
                return InteractionResult.FAIL;
            }
        }
        EyeState.enableWithProperties(target, EyeItemProperties.get(stack));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0F, 1.0F);
        target.gameEvent(GameEvent.ENTITY_INTERACT, player);
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
        return applyToTarget(stack, (ServerPlayer) player, player).consumesAction()
                ? InteractionResultHolder.consume(stack)
                : InteractionResultHolder.fail(stack);
    }
}
