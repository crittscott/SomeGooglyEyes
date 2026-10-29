package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.client.HeldEye;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorInstance;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeInfluence;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Coverage for {@link HeldEye}, the live eye a player sees in first person while holding a Googly Eye.
 * Drives it directly against a mock player, setting the hurt animation and health by hand, since the held
 * eye derives its behaviors from the holder's own vanilla-synced state on the holder's client. How the eye
 * looks in the hand is verified in game.
 */
public final class HeldEyeGameTestsLogic {

    // Half of a blink, where the eye is most closed, and how far it must be closed there.
    private static final int BLINK_PEAK_TICKS = EyeBehavior.BLINK.defaultDuration() / 2;
    private static final float BLINK_PEAK_SQUASH = 0.5F;
    // Ticks into a grow by which the eye has visibly bulged.
    private static final int GROW_RISE_TICKS = 3;

    private HeldEyeGameTestsLogic() {
    }

    /** A held eye ticked once for a fresh mock player holding a Googly Eye in {@code hand}. */
    private static HeldEye heldBy(Player player, InteractionHand hand) {
        player.setItemInHand(hand, new ItemStack(ModContent.GOOGLY_EYE.get()));
        HeldEye held = new HeldEye(RandomSource.create(1L));
        held.tick(player);
        return held;
    }

    private static void step(HeldEye held, Player player, int ticks) {
        for (int i = 0; i < ticks; i++) {
            held.tick(player);
        }
    }

    /** Steps until nothing is playing. */
    private static void stepUntilIdle(HeldEye held, Player player) {
        while (held.active != null) {
            held.tick(player);
        }
    }

    /**
     * A held eye bulges when its holder is hurt. In game: hold a Googly Eye in first person and take damage
     * (a fall, a cactus); the eye in your hand bulges and settles back.
     */
    public static void heldEyeBulgesWhenHurt(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        HeldEye held = heldBy(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(held.eye != null && held.active == null, "a freshly held eye is live and plays nothing");

        player.hurtTime = player.hurtDuration = 10;
        held.tick(player);
        helper.assertTrue(held.active != null && held.active.behavior == EyeBehavior.GROW, "being hurt plays grow");
        step(held, player, GROW_RISE_TICKS);
        helper.assertTrue(held.eye.scaleMul > 1F, "the eye bulges");
        helper.succeed();
    }

    /**
     * A held eye swirls when its holder heals, at most once per {@link HeldEye#HEAL_COOLDOWN_TICKS}, and a
     * heal while something else plays is dropped. In game: hold a Googly Eye in first person, get hurt, and
     * eat a golden apple or drink a potion of healing; the eye swirls. Heal again soon after and it doesn't.
     */
    public static void heldEyeSwirlsWhenHealed(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        player.setHealth(10F);
        HeldEye held = heldBy(player, InteractionHand.MAIN_HAND);

        player.heal(1F);
        held.tick(player);
        helper.assertTrue(held.active != null && held.active.behavior == EyeBehavior.SWIRL, "healing plays swirl");
        BehaviorInstance swirl = held.active;
        player.heal(1F);
        held.tick(player);
        helper.assertTrue(held.active == swirl, "a heal while the swirl plays is dropped");

        // The idle timer can't fire before AMBIENT_MIN_TICKS, past the end of the swirl.
        stepUntilIdle(held, player);
        player.heal(1F);
        held.tick(player);
        helper.assertTrue(held.active == null, "a heal inside the cooldown doesn't swirl");

        step(held, player, HeldEye.HEAL_COOLDOWN_TICKS);
        stepUntilIdle(held, player);
        player.heal(1F);
        held.tick(player);
        helper.assertTrue(held.active != null && held.active.behavior == EyeBehavior.SWIRL,
                "once the cooldown passes healing swirls again");
        helper.succeed();
    }

    /**
     * A held eye left alone winks, stares, or glances aside now and then, and never crosses. In game: hold a
     * Googly Eye in first person and stand still for up to 40 seconds; the eye in your hand winks, stares,
     * or glances aside.
     */
    public static void heldEyePlaysIdleBehaviors(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        HeldEye held = heldBy(player, InteractionHand.MAIN_HAND);
        EyeBehavior played = null;
        for (int i = 0; i < HeldEye.AMBIENT_MAX_TICKS && played == null; i++) {
            held.tick(player);
            if (held.active != null) {
                played = held.active.behavior;
            }
        }
        helper.assertTrue(played != null, "an idle behavior plays within the longest idle wait");
        helper.assertTrue(played == EyeBehavior.BLINK || played == EyeBehavior.STARE || played == EyeBehavior.SIDE_EYE,
                "the idle behavior is a wink, stare, or glance, not " + played.id());
        helper.succeed();
    }

    /**
     * A lone eye's blink closes it, and cross-eye, which needs a partner eye, leaves it alone. In game: hold a
     * Googly Eye in first person until it winks; the whole eye closes and opens again.
     */
    public static void heldEyeWinksAlone(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        HeldEye held = heldBy(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(held.start(EyeBehavior.BLINK), "a blink starts on an idle held eye");
        step(held, player, BLINK_PEAK_TICKS);
        helper.assertTrue(held.eye.squashY < BLINK_PEAK_SQUASH, "the lone eye closes for the blink");

        BehaviorInstance cross = new BehaviorInstance(EyeBehavior.CROSS_EYE, null,
                EyeBehavior.CROSS_EYE.defaultDuration(), 1L);
        EyeBehavior.CROSS_EYE.onStart(cross);
        cross.age = cross.duration / 2;
        EyeInfluence influence = new EyeInfluence();
        EyeBehavior.CROSS_EYE.influence(cross, 0, 0, influence);
        helper.assertTrue(influence.stiffness == 0F && influence.anchorX == 0F && influence.anchorY == 0F,
                "a lone eye has no partner to cross toward");
        helper.succeed();
    }

    /**
     * A held eye is dropped when it leaves the holder's hands and starts fresh when picked up again, in either
     * hand. In game: hold a Googly Eye in first person while it is mid-wink, switch to another item and back,
     * and it is at rest; move it to your off hand and it is live there too.
     */
    public static void heldEyeResetsWhenPutAway(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        HeldEye held = heldBy(player, InteractionHand.MAIN_HAND);
        held.start(EyeBehavior.BLINK);

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        held.tick(player);
        helper.assertTrue(held.eye == null && held.active == null, "putting the eye away drops it");

        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModContent.GOOGLY_EYE.get()));
        held.tick(player);
        helper.assertTrue(held.eye != null && held.active == null, "an eye in the off hand starts fresh");

        Player other = helper.makeMockPlayer();
        other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModContent.GOOGLY_EYE.get()));
        held.start(EyeBehavior.STARE);
        held.tick(other);
        helper.assertTrue(held.active == null, "a new holder starts fresh");
        helper.succeed();
    }
}
