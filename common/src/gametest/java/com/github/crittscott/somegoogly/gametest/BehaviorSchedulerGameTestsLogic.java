package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehaviors;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;

/**
 * Server-side coverage for {@link ServerBehaviorScheduler} — the authority for which behavior a mob is
 * playing and when. {@link BehaviorDeterminismGameTestsLogic} covers the client's animation derivation;
 * this drives the server clock by hand ({@code serverTick()}) and probes the schedule through
 * {@code trigger()}'s return (a dropped trigger means the mob is busy). Every test forces the scheduler
 * to a clean slate with {@code clear()} and force-and-restores the behavior config it touches; ambient
 * play is disabled so only the events under test can move the schedule. The mid-effect start-tracking
 * catch-up packet needs a connected client and stays source-verified.
 */
public final class BehaviorSchedulerGameTestsLogic {

    private BehaviorSchedulerGameTestsLogic() {
    }

    private static void advance(int ticks) {
        for (int i = 0; i < ticks; i++) {
            ServerBehaviorScheduler.serverTick();
        }
    }

    /** "One at a time, non-interruptable": a trigger is dropped while another behavior plays, allowed once it elapses. */
    public static void oneBehaviorAtATimeUntilItElapses(GameTestHelper helper, ServerPlayer player) {
        boolean ambient = ServerConfig.AMBIENT_BEHAVIORS.get();
        ServerBehaviorScheduler.clear();
        try {
            ServerConfig.AMBIENT_BEHAVIORS.set(false);
            Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
            EyeState.setHasEyes(cow, true);
            ServerBehaviorScheduler.onStartTracking(cow, player);

            helper.assertTrue(ServerBehaviorScheduler.trigger(cow, EyeBehaviors.STARE, 10, 1L),
                    "the first trigger starts");
            helper.assertTrue(!ServerBehaviorScheduler.trigger(cow, EyeBehaviors.BLINK, 10, 2L),
                    "a second trigger is dropped while one is playing");
            advance(10);
            helper.assertTrue(ServerBehaviorScheduler.trigger(cow, EyeBehaviors.BLINK, 10, 3L),
                    "once the active behavior elapses a new trigger starts");
        } finally {
            ServerBehaviorScheduler.clear();
            ServerConfig.AMBIENT_BEHAVIORS.set(ambient);
        }
        helper.succeed();
    }

    /** A heal swirl is rate-limited per mob: a second heal inside the cooldown is dropped even while the mob is idle. */
    public static void healSwirlIsRateLimited(GameTestHelper helper, ServerPlayer player) {
        boolean ambient = ServerConfig.AMBIENT_BEHAVIORS.get();
        boolean onHeal = ServerConfig.SWIRL_ON_HEAL.get();
        int cooldown = ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.get();
        ServerBehaviorScheduler.clear();
        try {
            ServerConfig.AMBIENT_BEHAVIORS.set(false);
            ServerConfig.SWIRL_ON_HEAL.set(true);
            ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.set(1000);
            Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
            EyeState.setHasEyes(cow, true);
            ServerBehaviorScheduler.onStartTracking(cow, player);

            ServerBehaviorScheduler.onHealed(cow);
            helper.assertTrue(!ServerBehaviorScheduler.trigger(cow, EyeBehaviors.BLINK, 5, 1L),
                    "the first heal starts a swirl");

            advance(EyeBehaviors.SWIRL.defaultDuration());
            ServerBehaviorScheduler.onHealed(cow);
            helper.assertTrue(ServerBehaviorScheduler.trigger(cow, EyeBehaviors.BLINK, 5, 2L),
                    "a heal inside the cooldown is dropped even though the swirl has elapsed and the mob is idle");
        } finally {
            ServerBehaviorScheduler.clear();
            ServerConfig.AMBIENT_BEHAVIORS.set(ambient);
            ServerConfig.SWIRL_ON_HEAL.set(onHeal);
            ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.set(cooldown);
        }
        helper.succeed();
    }

    /** {@code swirlOnHeal}, {@code swirlOnTrade}, and {@code growOnHitPercent} gate their respective triggers. */
    public static void gameEventTriggersRespectConfig(GameTestHelper helper, ServerPlayer player) {
        boolean ambient = ServerConfig.AMBIENT_BEHAVIORS.get();
        boolean onHeal = ServerConfig.SWIRL_ON_HEAL.get();
        boolean onTrade = ServerConfig.SWIRL_ON_TRADE.get();
        int growPercent = ServerConfig.GROW_ON_HIT_PERCENT.get();
        ServerBehaviorScheduler.clear();
        try {
            ServerConfig.AMBIENT_BEHAVIORS.set(false);
            Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
            EyeState.setHasEyes(cow, true);
            ServerBehaviorScheduler.onStartTracking(cow, player);

            ServerConfig.SWIRL_ON_HEAL.set(false);
            ServerBehaviorScheduler.onHealed(cow);
            helper.assertTrue(ServerBehaviorScheduler.trigger(cow, EyeBehaviors.BLINK, 3, 1L),
                    "swirlOnHeal=false suppresses the heal swirl");
            advance(3);

            ServerConfig.SWIRL_ON_TRADE.set(false);
            ServerBehaviorScheduler.onTrade(cow);
            helper.assertTrue(ServerBehaviorScheduler.trigger(cow, EyeBehaviors.BLINK, 3, 2L),
                    "swirlOnTrade=false suppresses the trade swirl");
            advance(3);

            ServerConfig.GROW_ON_HIT_PERCENT.set(0);
            ServerBehaviorScheduler.onPlayerHurt(cow);
            helper.assertTrue(ServerBehaviorScheduler.trigger(cow, EyeBehaviors.BLINK, 3, 3L),
                    "growOnHitPercent=0 never rolls the hurt grow");
        } finally {
            ServerBehaviorScheduler.clear();
            ServerConfig.AMBIENT_BEHAVIORS.set(ambient);
            ServerConfig.SWIRL_ON_HEAL.set(onHeal);
            ServerConfig.SWIRL_ON_TRADE.set(onTrade);
            ServerConfig.GROW_ON_HIT_PERCENT.set(growPercent);
        }
        helper.succeed();
    }

    /** A tracked mob with no eyes is not an event target: heal and trade triggers no-op. */
    public static void eyelessTrackedMobIgnoresGameEvents(GameTestHelper helper, ServerPlayer player) {
        boolean ambient = ServerConfig.AMBIENT_BEHAVIORS.get();
        boolean onHeal = ServerConfig.SWIRL_ON_HEAL.get();
        boolean onTrade = ServerConfig.SWIRL_ON_TRADE.get();
        ServerBehaviorScheduler.clear();
        try {
            ServerConfig.AMBIENT_BEHAVIORS.set(false);
            ServerConfig.SWIRL_ON_HEAL.set(true);
            ServerConfig.SWIRL_ON_TRADE.set(true);
            Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
            ServerBehaviorScheduler.onStartTracking(cow, player);
            EyeState.setHasEyes(cow, false);

            ServerBehaviorScheduler.onHealed(cow);
            ServerBehaviorScheduler.onTrade(cow);
            helper.assertTrue(ServerBehaviorScheduler.trigger(cow, EyeBehaviors.BLINK, 5, 1L),
                    "an eyeless mob's game events never start a behavior even while it is tracked");
        } finally {
            ServerBehaviorScheduler.clear();
            ServerConfig.AMBIENT_BEHAVIORS.set(ambient);
            ServerConfig.SWIRL_ON_HEAL.set(onHeal);
            ServerConfig.SWIRL_ON_TRADE.set(onTrade);
        }
        helper.succeed();
    }
}
