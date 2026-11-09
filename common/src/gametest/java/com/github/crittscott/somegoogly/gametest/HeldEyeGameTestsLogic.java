package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.registry.ModContent;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Real server reactions feed the same per-hand presentation used by first- and third-person draws. */
public final class HeldEyeGameTestsLogic {
    private HeldEyeGameTestsLogic() { }

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
            ServerConfig.GROW_ON_HIT_PERCENT.set(100);
            ServerConfig.SWIRL_ON_HEAL.set(true);
            ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.set(200);
            ServerBehaviorScheduler.clear();
            if (player instanceof ServerPlayer serverPlayer) {
                ServerBehaviorScheduler.onPlayerJoined(serverPlayer);
            } else {
                ServerBehaviorScheduler.onStartTracking(player, template);
            }
            ClientEyeRuntime.onLevelChanged();
        }
        @Override public void close() {
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

    /** In game: hold a Googly Eye and take nonplayer damage; its bulge is visible to you and another viewer. */
    public static void heldEyeBulgesWhenHurt(GameTestHelper helper, ServerPlayer template) {
        try (Context context = new Context(template, helper.makeMockPlayer())) {
            context.player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModContent.GOOGLY_EYE.get()));
            hurtByFall(helper, context.player);
            for (InteractionHand hand : InteractionHand.values()) {
                var packet = ServerBehaviorScheduler.handBehavior(context.player, hand);
                helper.assertTrue(packet != null && packet.behavior() == EyeBehavior.GROW, "each held eye receives a server grow");
                ClientEyeRuntime.setHandBehavior(context.player, hand, packet.behavior(), packet.seed(), packet.elapsed());
            }
            var main = ClientEyeRuntime.handEyes(context.player, InteractionHand.MAIN_HAND, context.player.getMainHandItem());
            var off = ClientEyeRuntime.handEyes(context.player, InteractionHand.OFF_HAND, context.player.getOffhandItem());
            for (int time = 0; time < 4; time++) {
                ClientEyeRuntime.handEyes(context.player, InteractionHand.MAIN_HAND, context.player.getMainHandItem());
                ClientEyeRuntime.handEyes(context.player, InteractionHand.OFF_HAND, context.player.getOffhandItem());
                ClientEyeRuntime.tickWorld(time, BehaviorSettings.fromConfig());
            }
            helper.assertTrue(main != off && main.eye.scaleMul > 1F && off.eye.scaleMul > 1F, "both independent hands display the real reaction");
        }
        helper.succeed();
    }

    /** In game: turn hit growth and heal swirl off; damage and healing leave a held eye without reactions. */
    public static void heldEyeHonorsServerSettings(GameTestHelper helper, ServerPlayer template) {
        try (Context context = new Context(template, helper.makeMockPlayer())) {
            ServerConfig.GROW_ON_HIT_PERCENT.set(0);
            ServerConfig.SWIRL_ON_HEAL.set(false);
            hurtByFall(helper, context.player);
            context.player.heal(1F);
            helper.assertTrue(ServerBehaviorScheduler.handBehavior(context.player, InteractionHand.MAIN_HAND) == null,
                "real hurt/heal actions respect their switches");
        }
        helper.succeed();
    }

    /** In game: keep a held eye visible through F5; its stable ambient blink continues on the same pupil. */
    public static void heldEyePlaysIdleBehaviors(GameTestHelper helper, ServerPlayer template) {
        try (Context context = new Context(template)) {
            var stack = context.player.getMainHandItem();
            var eye = ClientEyeRuntime.handEyes(context.player, InteractionHand.MAIN_HAND, stack);
            var defaults = TestBehaviorSettings.DEFAULTS;
            var blink = new BehaviorSettings(true, 1, 1, List.of(EyeBehavior.BLINK), 0, false, 200, defaults.ongoing());
            for (int tick = 0; tick < 4; tick++) {
                helper.assertTrue(ClientEyeRuntime.handEyes(context.player, InteractionHand.MAIN_HAND, stack.copy()) == eye,
                    "stack copies and view changes select the same hand entry");
                ClientEyeRuntime.tickWorld(tick, blink);
            }
            helper.assertTrue(eye.active != null && eye.active.behavior == EyeBehavior.BLINK, "the hand uses deterministic ambient slots");
        }
        helper.succeed();
    }

    /** In game: put the eye away and equip it again; the old hand simulation and received reaction are retired. */
    public static void heldEyeResetsWhenPutAway(GameTestHelper helper, ServerPlayer template) {
        try (Context context = new Context(template)) {
            ItemStack eyeItem = context.player.getMainHandItem();
            var before = ClientEyeRuntime.handEyes(context.player, InteractionHand.MAIN_HAND, eyeItem);
            ClientEyeRuntime.setHandBehavior(context.player, InteractionHand.MAIN_HAND, EyeBehavior.GROW, 42L, 0);
            context.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            ClientEyeRuntime.tickWorld(1L, BehaviorSettings.NONE);
            context.player.setItemInHand(InteractionHand.MAIN_HAND, eyeItem);
            var after = ClientEyeRuntime.handEyes(context.player, InteractionHand.MAIN_HAND, eyeItem);
            ClientEyeRuntime.tickWorld(2L, BehaviorSettings.NONE);
            helper.assertTrue(after != before && after.active == null, "reequipping starts a fresh hand presentation");
        }
        helper.succeed();
    }

    /** In game: heal while holding an eye, then heal again after its swirl but before the cooldown; no second swirl. */
    public static void heldEyeSwirlsWhenHealed(GameTestHelper helper, ServerPlayer template) {
        Context context = new Context(template);
        try {
            context.player.setHealth(1F);
            context.player.heal(1F);
            var first = ServerBehaviorScheduler.handBehavior(context.player, InteractionHand.MAIN_HAND);
            helper.assertTrue(first != null && first.behavior() == EyeBehavior.SWIRL, "a real heal starts a server-held swirl");
            context.player.heal(1F);
            helper.assertTrue(ServerBehaviorScheduler.handBehavior(context.player, InteractionHand.MAIN_HAND).seed() == first.seed(),
                "a busy eye keeps its current reaction");
            helper.runAfterDelay(EyeBehavior.SWIRL.defaultDuration() + 1, () -> {
                try {
                    context.player.heal(1F);
                    helper.assertTrue(ServerBehaviorScheduler.handBehavior(context.player, InteractionHand.MAIN_HAND) == null,
                        "a heal after expression expiry still respects the cooldown");
                    helper.runAfterDelay(200 - EyeBehavior.SWIRL.defaultDuration(), () -> {
                        try {
                            context.player.heal(1F);
                            var next = ServerBehaviorScheduler.handBehavior(context.player, InteractionHand.MAIN_HAND);
                            helper.assertTrue(next != null && next.behavior() == EyeBehavior.SWIRL && next.seed() != first.seed(),
                                "a heal after the cooldown starts a fresh reaction");
                        } finally { context.close(); }
                        helper.succeed();
                    });
                } catch (RuntimeException | Error failure) { context.close(); throw failure; }
            });
        } catch (RuntimeException | Error failure) { context.close(); throw failure; }
    }

    /** In game: a single held eye blinks closed and never selects the paired cross-eye expression. */
    public static void heldEyeWinksAlone(GameTestHelper helper, ServerPlayer template) {
        try (Context context = new Context(template)) {
            var defaults = TestBehaviorSettings.DEFAULTS;
            var settings = new BehaviorSettings(true, 1, 1, List.of(EyeBehavior.BLINK, EyeBehavior.CROSS_EYE), 0, false, 200, defaults.ongoing());
            var eye = ClientEyeRuntime.handEyes(context.player, InteractionHand.MAIN_HAND, context.player.getMainHandItem());
            for (int tick = 0; tick <= EyeBehavior.BLINK.defaultDuration() / 2; tick++) {
                ClientEyeRuntime.handEyes(context.player, InteractionHand.MAIN_HAND, context.player.getMainHandItem());
                ClientEyeRuntime.tickWorld(tick, settings);
            }
            helper.assertTrue(eye.active != null && eye.active.behavior == EyeBehavior.BLINK && eye.eye.squashY < 0.5F,
                "a lone held eye closes for its blink and filters cross-eye");
        }
        helper.succeed();
    }

    /** Environmental damage avoids difficulty-scaled mob attacks and reaches the real player hurt hook. */
    private static void hurtByFall(GameTestHelper helper, Player player) {
        DamageSource fall = new DamageSource(helper.getLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.FALL));
        float health = player.getHealth();
        helper.assertTrue(player.hurt(fall, 1F) && player.getHealth() < health,
                "real environmental damage must lower the holder's health");
    }
}
