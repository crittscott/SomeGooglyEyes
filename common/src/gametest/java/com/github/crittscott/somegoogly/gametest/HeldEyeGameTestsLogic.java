package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.network.EyeBehaviorTriggerPacket;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.List;

/**
 * Coverage for a loose Googly Eye held in a hand. The server schedules its hurt and heal reactions from the real
 * damage and heal hooks and reports them per hand; the client shows them on the same per-hand simulation that
 * first- and third-person draws share, beside its deterministic ambient expressions. How the eye looks in the
 * hand is verified in game.
 */
public final class HeldEyeGameTestsLogic {

    // Half of a blink, where the eye is most closed, and how far it must be closed there.
    private static final int BLINK_PEAK_TICKS = EyeBehavior.BLINK.defaultDuration() / 2;
    private static final float BLINK_PEAK_SQUASH = 0.5F;
    private static final int HEAL_COOLDOWN_TICKS = 200;

    private HeldEyeGameTestsLogic() {
    }

    /** A holder of a Googly Eye watched by the scheduler, under known reaction settings; restores them on close. */
    private static final class Context implements AutoCloseable {
        final boolean ambient = ServerConfig.AMBIENT_BEHAVIORS.get();
        final int cooldown = ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.get();
        final boolean enabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
        final int grow = ServerConfig.GROW_ON_HIT_PERCENT.get();
        final boolean heal = ServerConfig.SWIRL_ON_HEAL.get();
        final Player player;

        Context(ServerPlayer template) {
            this(template, TestPlayers.create(template, false, 0));
        }

        Context(ServerPlayer template, Player player) {
            this.player = player;
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModContent.GOOGLY_EYE.get()));
            ServerConfig.AMBIENT_BEHAVIORS.set(false);
            ServerConfig.GOOGLY_EYES_ENABLED.set(true);
            ServerConfig.GROW_ON_HIT_PERCENT.set(ServerConfig.PERCENT_MAX);
            ServerConfig.SWIRL_ON_HEAL.set(true);
            ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.set(HEAL_COOLDOWN_TICKS);
            ServerBehaviorScheduler.clear();
            if (player instanceof ServerPlayer serverPlayer) {
                ServerBehaviorScheduler.onPlayerJoined(serverPlayer);
            } else {
                ServerBehaviorScheduler.onStartTracking(player, template);
            }
            ClientEyeRuntime.onLevelChanged();
        }

        @Override
        public void close() {
            ServerConfig.AMBIENT_BEHAVIORS.set(ambient);
            ServerConfig.GOOGLY_EYES_ENABLED.set(enabled);
            ServerConfig.GROW_ON_HIT_PERCENT.set(grow);
            ServerConfig.SWIRL_ON_HEAL.set(heal);
            ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.set(cooldown);
            ServerBehaviorScheduler.clear();
            ClientEyeRuntime.onLevelChanged();
            player.discard();
        }
    }

    /**
     * A held eye bulges when its holder takes any damage, at the server's {@code growOnHitPercent}; here it is 100,
     * and each hand's eye reacts on its own. In game: with {@code growOnHitPercent = 100}, hold a Googly Eye in each
     * hand and take fall damage; both eyes bulge and settle back, in first person, in F5, and as another player sees
     * them.
     */
    public static void heldEyeBulgesWhenHurt(GameTestHelper helper, ServerPlayer template) {
        try (Context context = new Context(template, helper.makeMockPlayer(GameType.SURVIVAL))) {
            context.player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModContent.GOOGLY_EYE.get()));
            hurtByFall(helper, context.player);
            for (InteractionHand hand : InteractionHand.values()) {
                EyeBehaviorTriggerPacket packet = ServerBehaviorScheduler.handBehavior(context.player, hand);
                helper.assertTrue(packet != null && packet.behavior() == EyeBehavior.GROW,
                        "each held eye receives a server grow");
                ClientEyeRuntime.setHandBehavior(context.player, hand, packet.behavior(), packet.seed(), packet.elapsed());
            }
            PresentationEyes main = ClientEyeRuntime.handEyes(context.player, InteractionHand.MAIN_HAND,
                    context.player.getMainHandItem());
            PresentationEyes off = ClientEyeRuntime.handEyes(context.player, InteractionHand.OFF_HAND,
                    context.player.getOffhandItem());
            for (int time = 0; time < 4; time++) {
                ClientEyeRuntime.handEyes(context.player, InteractionHand.MAIN_HAND, context.player.getMainHandItem());
                ClientEyeRuntime.handEyes(context.player, InteractionHand.OFF_HAND, context.player.getOffhandItem());
                ClientEyeRuntime.tickWorld(time, BehaviorSettings.fromConfig());
            }
            helper.assertTrue(main != off && main.eye.scaleMul > 1F && off.eye.scaleMul > 1F,
                    "both independent hands display the real reaction");
        }
        helper.succeed();
    }

    /**
     * The server's switches govern held reactions: with hit growth at 0 and heal swirl off, damage and healing
     * start nothing. In game: set {@code growOnHitPercent = 0} and {@code swirlOnHeal = false} in
     * {@code somegoogly-server.toml}, hold a Googly Eye, take damage and eat a golden apple; the eye only idles.
     */
    public static void heldEyeHonorsServerSettings(GameTestHelper helper, ServerPlayer template) {
        try (Context context = new Context(template, helper.makeMockPlayer(GameType.SURVIVAL))) {
            ServerConfig.GROW_ON_HIT_PERCENT.set(0);
            ServerConfig.SWIRL_ON_HEAL.set(false);
            hurtByFall(helper, context.player);
            context.player.heal(1F);
            helper.assertTrue(ServerBehaviorScheduler.handBehavior(context.player, InteractionHand.MAIN_HAND) == null,
                    "real hurt and heal respect their switches");
        }
        helper.succeed();
    }

    /**
     * A held eye plays the server's ambient expressions from deterministic world-time slots, on one simulation per
     * hand that stack copies keep. In game: hold a Googly Eye and stand still for up to 40 seconds, switching
     * between first person and F5; it blinks, stares, or glances on the same pupil in both views.
     */
    public static void heldEyePlaysIdleBehaviors(GameTestHelper helper, ServerPlayer template) {
        try (Context context = new Context(template)) {
            ItemStack stack = context.player.getMainHandItem();
            PresentationEyes eye = ClientEyeRuntime.handEyes(context.player, InteractionHand.MAIN_HAND, stack);
            BehaviorSettings defaults = TestBehaviorSettings.DEFAULTS;
            BehaviorSettings blink = new BehaviorSettings(true, 1, 1, List.of(EyeBehavior.BLINK), 0, false,
                    HEAL_COOLDOWN_TICKS, defaults.ongoing());
            for (int tick = 0; tick < 4; tick++) {
                helper.assertTrue(ClientEyeRuntime.handEyes(context.player, InteractionHand.MAIN_HAND, stack.copy()) == eye,
                        "stack copies and view changes select the same hand simulation");
                ClientEyeRuntime.tickWorld(tick, blink);
            }
            helper.assertTrue(eye.active != null && eye.active.behavior == EyeBehavior.BLINK,
                    "the hand plays its ambient slot");
        }
        helper.succeed();
    }

    /**
     * Putting the eye away retires its simulation and any received reaction; equipping it again starts afresh. In
     * game: hold a Googly Eye while it bulges from a hit, switch to another item and back; it is at rest.
     */
    public static void heldEyeResetsWhenPutAway(GameTestHelper helper, ServerPlayer template) {
        try (Context context = new Context(template)) {
            ItemStack eyeItem = context.player.getMainHandItem();
            PresentationEyes before = ClientEyeRuntime.handEyes(context.player, InteractionHand.MAIN_HAND, eyeItem);
            ClientEyeRuntime.setHandBehavior(context.player, InteractionHand.MAIN_HAND, EyeBehavior.GROW, 42L, 0);
            context.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            ClientEyeRuntime.tickWorld(1L, BehaviorSettings.NONE);
            context.player.setItemInHand(InteractionHand.MAIN_HAND, eyeItem);
            PresentationEyes after = ClientEyeRuntime.handEyes(context.player, InteractionHand.MAIN_HAND, eyeItem);
            ClientEyeRuntime.tickWorld(2L, BehaviorSettings.NONE);
            helper.assertTrue(after != before && after.active == null, "reequipping starts a fresh hand simulation");
        }
        helper.succeed();
    }

    /**
     * A held eye swirls when its holder heals, then not again until the cooldown passes, and a heal while it is
     * busy keeps the current reaction. In game: hold a Googly Eye, get hurt, and eat a golden apple; the eye
     * swirls once, and further regeneration within ten seconds starts no second swirl.
     */
    public static void heldEyeSwirlsWhenHealed(GameTestHelper helper, ServerPlayer template) {
        Context context = new Context(template);
        try {
            context.player.setHealth(1F);
            context.player.heal(1F);
            EyeBehaviorTriggerPacket first = ServerBehaviorScheduler.handBehavior(context.player, InteractionHand.MAIN_HAND);
            helper.assertTrue(first != null && first.behavior() == EyeBehavior.SWIRL, "a real heal starts a server swirl");
            context.player.heal(1F);
            helper.assertTrue(ServerBehaviorScheduler.handBehavior(context.player, InteractionHand.MAIN_HAND).seed()
                    == first.seed(), "a busy eye keeps its current reaction");
            helper.runAfterDelay(EyeBehavior.SWIRL.defaultDuration() + 1, () -> {
                try {
                    context.player.heal(1F);
                    helper.assertTrue(ServerBehaviorScheduler.handBehavior(context.player, InteractionHand.MAIN_HAND) == null,
                            "a heal after the swirl ends still respects the cooldown");
                    helper.runAfterDelay(HEAL_COOLDOWN_TICKS - EyeBehavior.SWIRL.defaultDuration(), () -> {
                        try {
                            context.player.heal(1F);
                            EyeBehaviorTriggerPacket next = ServerBehaviorScheduler.handBehavior(
                                    context.player, InteractionHand.MAIN_HAND);
                            helper.assertTrue(next != null && next.behavior() == EyeBehavior.SWIRL
                                    && next.seed() != first.seed(), "a heal after the cooldown starts a fresh swirl");
                        } finally {
                            context.close();
                        }
                        helper.succeed();
                    });
                } catch (RuntimeException | Error failure) {
                    context.close();
                    throw failure;
                }
            });
        } catch (RuntimeException | Error failure) {
            context.close();
            throw failure;
        }
    }

    /**
     * A lone eye's blink closes it, and cross-eye, which needs a partner eye, is never chosen. In game: hold a
     * Googly Eye and watch it idle; it winks shut now and then but never crosses.
     */
    public static void heldEyeWinksAlone(GameTestHelper helper, ServerPlayer template) {
        try (Context context = new Context(template)) {
            BehaviorSettings defaults = TestBehaviorSettings.DEFAULTS;
            BehaviorSettings settings = new BehaviorSettings(true, 1, 1, List.of(EyeBehavior.BLINK, EyeBehavior.CROSS_EYE),
                    0, false, HEAL_COOLDOWN_TICKS, defaults.ongoing());
            PresentationEyes eye = ClientEyeRuntime.handEyes(context.player, InteractionHand.MAIN_HAND,
                    context.player.getMainHandItem());
            for (int tick = 0; tick <= BLINK_PEAK_TICKS; tick++) {
                ClientEyeRuntime.handEyes(context.player, InteractionHand.MAIN_HAND, context.player.getMainHandItem());
                ClientEyeRuntime.tickWorld(tick, settings);
            }
            helper.assertTrue(eye.active != null && eye.active.behavior == EyeBehavior.BLINK
                    && eye.eye.squashY < BLINK_PEAK_SQUASH, "a lone held eye closes for its blink and skips cross-eye");
        }
        helper.succeed();
    }

    /** Environmental damage avoids difficulty-scaled mob attacks and reaches the real damage hook. */
    private static void hurtByFall(GameTestHelper helper, Player player) {
        DamageSource fall = helper.getLevel().damageSources().fall();
        float health = player.getHealth();
        helper.assertTrue(player.hurtServer(helper.getLevel(), fall, 1F) && player.getHealth() < health,
                "real environmental damage must lower the holder's health");
    }
}
