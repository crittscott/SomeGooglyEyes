package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.registry.ModContent;
import com.github.crittscott.somegoogly.server.EyeItemService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A googly eye bedded in a slimeball: the applicator. Right-clicking a living entity with it sticks
 * eyes on that entity, using the appearance the eye carried into the craft
 * ({@link EyeItemProperties}); sneak + use applies it to the player themselves. A Slimy Eye, applied by
 * the player or by another player, is the only way a player gets eyes (players are excluded from the
 * at-spawn roll). It shares the Googly Eye's
 * stored appearance and tooltip.
 *
 * <p>The slimy eye carries appearance only, never placement — where the eyes land comes from the
 * target's datapack config, on a placement variant freshly rolled by each application. An
 * already-eyed target refuses the application and consumes nothing, so recoloring an eyed mob means
 * harvesting its eye, modifying it, and re-applying.
 *
 * <p>One eye in, one eye out: an application consumes a single slimy eye and a harvest yields a
 * single eye item, so a craft-apply-harvest loop costs a slimeball per turn and cannot multiply eyes.
 *
 * <p>The application itself is {@link EyeItemService#applySlimyEye}. The mob path is dispatched from
 * each loader's entity-interact adapter — which claims the right-click before the target's own
 * interaction can consume it — not through {@code Item#interactLivingEntity}; only the sneak
 * self-apply ({@link #use}) is dispatched through this class.
 */
public class SlimyEyeItem extends GooglyEyeItem {

    public SlimyEyeItem(Properties properties) {
        super(properties);
    }

    /** A new slimy-eye stack carrying {@code properties}. */
    public static ItemStack create(AppearanceOverride properties, int count) {
        ItemStack stack = new ItemStack(ModContent.SLIMY_EYE.get(), count);
        EyeItemProperties.set(stack, properties);
        return stack;
    }

    /** Sneak + use: eye yourself. Without the sneak this would fire on every stray right-click. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return EyeItemService.applySlimyEye(player.getItemInHand(hand), (ServerPlayer) player, player)
                .consumesAction()
                ? InteractionResult.CONSUME
                : InteractionResult.FAIL;
    }
}
