package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Server-side coverage for {@link ServerBehaviorScheduler} — the authority for which behavior a mob is
 * playing and when. {@link BehaviorDeterminismGameTestsLogic} covers the client's animation derivation;
 * this drives the server clock by hand ({@code serverTick()}) and probes the schedule through
 * {@code trigger()}'s return (a dropped trigger means the mob is busy or asleep). Every test forces the
 * scheduler to a clean slate with {@code clear()} and force-and-restores the behavior config it touches;
 * ambient play is disabled so only the events under test can move the schedule, except where ambient
 * rolls are what's under test. The sleeping squint, which the client derives from vanilla's synced sleep
 * state, is covered here too, since sleep is the one state that blocks the schedule. The mid-effect
 * start-tracking catch-up packet needs a connected client and stays source-verified.
 */
public final class BehaviorSchedulerGameTestsLogic {

    // Float slack for comparing eased squash values, which accumulate a 1/SLEEP_EASE_TICKS step.
    private static final float SQUASH_TOLERANCE = 1e-4F;

    private BehaviorSchedulerGameTestsLogic() {
    }

    private static void advance(int ticks) {
        for (int i = 0; i < ticks; i++) {
            ServerBehaviorScheduler.serverTick();
        }
    }

    /**
     * "One at a time, non-interruptable": a trigger is dropped while another behavior plays, allowed once it
     * elapses. In game: as a creative operator, look at an eyed mob and run {@code /sg admin behavior swirl},
     * then {@code /sg admin behavior blink} while it swirls; the blink does not play. Run it again after the
     * swirl ends and it does.
     */
    public static void oneBehaviorAtATimeUntilItElapses(GameTestHelper helper, ServerPlayer player) {
        boolean ambient = ServerConfig.AMBIENT_BEHAVIORS.get();
        ServerBehaviorScheduler.clear();
        try {
            ServerConfig.AMBIENT_BEHAVIORS.set(false);
            Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
            EyeState.setHasEyes(cow, true);
            ServerBehaviorScheduler.onStartTracking(cow, player);

            helper.assertTrue(ServerBehaviorScheduler.trigger(cow, EyeBehavior.STARE, 10, 1L),
                    "the first trigger starts");
            helper.assertTrue(!ServerBehaviorScheduler.trigger(cow, EyeBehavior.BLINK, 10, 2L),
                    "a second trigger is dropped while one is playing");
            advance(10);
            helper.assertTrue(ServerBehaviorScheduler.trigger(cow, EyeBehavior.BLINK, 10, 3L),
                    "once the active behavior elapses a new trigger starts");
        } finally {
            ServerBehaviorScheduler.clear();
            ServerConfig.AMBIENT_BEHAVIORS.set(ambient);
        }
        helper.succeed();
    }

    /**
     * A heal swirl is rate-limited per mob: a second heal inside the cooldown is dropped even while the mob
     * is idle. In game: hurt an eyed cow, throw a splash potion of healing at it, and its eyes swirl; hurt
     * and heal it again within {@code swirlHealCooldownTicks} (default 200) after the first heal, and they do
     * not.
     */
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
            helper.assertTrue(!ServerBehaviorScheduler.trigger(cow, EyeBehavior.BLINK, 5, 1L),
                    "the first heal starts a swirl");

            advance(EyeBehavior.SWIRL.defaultDuration());
            ServerBehaviorScheduler.onHealed(cow);
            helper.assertTrue(ServerBehaviorScheduler.trigger(cow, EyeBehavior.BLINK, 5, 2L),
                    "a heal inside the cooldown is dropped even though the swirl has elapsed and the mob is idle");
        } finally {
            ServerBehaviorScheduler.clear();
            ServerConfig.AMBIENT_BEHAVIORS.set(ambient);
            ServerConfig.SWIRL_ON_HEAL.set(onHeal);
            ServerConfig.SWIRL_HEAL_COOLDOWN_TICKS.set(cooldown);
        }
        helper.succeed();
    }

    /**
     * {@code swirlOnHeal}, {@code swirlOnTrade}, and {@code growOnHitPercent} gate their respective
     * triggers. In game: set {@code swirlOnHeal = false}, {@code swirlOnTrade = false}, and
     * {@code growOnHitPercent = 0} in the server config and apply it; healing an eyed cow, trading with an
     * eyed villager, and hitting either never changes their eyes.
     */
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
            helper.assertTrue(ServerBehaviorScheduler.trigger(cow, EyeBehavior.BLINK, 3, 1L),
                    "swirlOnHeal=false suppresses the heal swirl");
            advance(3);

            ServerConfig.SWIRL_ON_TRADE.set(false);
            ServerBehaviorScheduler.onTrade(cow);
            helper.assertTrue(ServerBehaviorScheduler.trigger(cow, EyeBehavior.BLINK, 3, 2L),
                    "swirlOnTrade=false suppresses the trade swirl");
            advance(3);

            ServerConfig.GROW_ON_HIT_PERCENT.set(0);
            ServerBehaviorScheduler.onPlayerHurt(cow);
            helper.assertTrue(ServerBehaviorScheduler.trigger(cow, EyeBehavior.BLINK, 3, 3L),
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

    /**
     * The vanilla affection events play love, through the server's entity-event broadcast, and only while
     * {@code loveOnAffection} is on; other entity events play nothing. In game: feed wheat to an eyed cow,
     * watch an eyed iron golem offer a villager a flower, breed eyed villagers, or tame an eyed ocelot with
     * raw fish, and the mob's pupils swell to fill its eyes and shrink back. With
     * {@code loveOnAffection = false} none of them change the eyes.
     */
    public static void affectionEventsPlayLove(GameTestHelper helper, ServerPlayer player) {
        boolean ambient = ServerConfig.AMBIENT_BEHAVIORS.get();
        boolean onAffection = ServerConfig.LOVE_ON_AFFECTION.get();
        ServerBehaviorScheduler.clear();
        try {
            ServerConfig.AMBIENT_BEHAVIORS.set(false);
            Cow cow = eyedTrackedMob(helper, player, EntityType.COW, new BlockPos(1, 2, 1));
            IronGolem golem = eyedTrackedMob(helper, player, EntityType.IRON_GOLEM, new BlockPos(3, 2, 1));
            Villager villager = eyedTrackedMob(helper, player, EntityType.VILLAGER, new BlockPos(1, 2, 3));
            Ocelot ocelot = eyedTrackedMob(helper, player, EntityType.OCELOT, new BlockPos(3, 2, 3));

            ServerConfig.LOVE_ON_AFFECTION.set(false);
            cow.setInLove(player);
            helper.assertTrue(ServerBehaviorScheduler.trigger(cow, EyeBehavior.BLINK, 3, 1L),
                    "loveOnAffection=false suppresses love");
            advance(3);

            ServerConfig.LOVE_ON_AFFECTION.set(true);
            cow.setInLove(player);
            helper.assertTrue(!ServerBehaviorScheduler.trigger(cow, EyeBehavior.BLINK, 3, 2L),
                    "an animal entering love mode plays love");
            golem.offerFlower(true);
            helper.assertTrue(!ServerBehaviorScheduler.trigger(golem, EyeBehavior.BLINK, 3, 3L),
                    "an iron golem offering a flower plays love");
            helper.getLevel().broadcastEntityEvent(villager, EntityEvent.LOVE_HEARTS);
            helper.assertTrue(!ServerBehaviorScheduler.trigger(villager, EyeBehavior.BLINK, 3, 4L),
                    "villager love hearts play love");
            helper.getLevel().broadcastEntityEvent(ocelot, EntityEvent.TRUSTING_SUCCEEDED);
            helper.assertTrue(!ServerBehaviorScheduler.trigger(ocelot, EyeBehavior.BLINK, 3, 5L),
                    "an ocelot gaining trust plays love");

            advance(EyeBehavior.LOVE.defaultDuration());
            helper.getLevel().broadcastEntityEvent(cow, EntityEvent.TAMING_FAILED);
            helper.assertTrue(ServerBehaviorScheduler.trigger(cow, EyeBehavior.BLINK, 3, 6L),
                    "an entity event that is not affection plays nothing");
        } finally {
            ServerBehaviorScheduler.clear();
            ServerConfig.AMBIENT_BEHAVIORS.set(ambient);
            ServerConfig.LOVE_ON_AFFECTION.set(onAffection);
        }
        helper.succeed();
    }

    /**
     * Milking a cow, mooshroom, or goat with a bucket plays grow, and only while {@code growOnMilk} is on.
     * In game: use an empty bucket on an eyed cow, mooshroom, or goat, and its eyes bulge as the bucket
     * fills. With {@code growOnMilk = false} they do not.
     */
    public static void milkingPlaysGrow(GameTestHelper helper, ServerPlayer player) {
        boolean ambient = ServerConfig.AMBIENT_BEHAVIORS.get();
        boolean onMilk = ServerConfig.GROW_ON_MILK.get();
        ItemStack held = player.getMainHandItem().copy();
        ServerBehaviorScheduler.clear();
        try {
            ServerConfig.AMBIENT_BEHAVIORS.set(false);
            Cow cow = eyedTrackedMob(helper, player, EntityType.COW, new BlockPos(1, 2, 1));
            MushroomCow mooshroom = eyedTrackedMob(helper, player, EntityType.MOOSHROOM, new BlockPos(3, 2, 1));
            Goat goat = eyedTrackedMob(helper, player, EntityType.GOAT, new BlockPos(1, 2, 3));

            ServerConfig.GROW_ON_MILK.set(false);
            milk(player, cow);
            helper.assertTrue(ServerBehaviorScheduler.trigger(cow, EyeBehavior.BLINK, 3, 1L),
                    "growOnMilk=false suppresses the milking grow");
            advance(3);

            ServerConfig.GROW_ON_MILK.set(true);
            milk(player, cow);
            helper.assertTrue(!ServerBehaviorScheduler.trigger(cow, EyeBehavior.BLINK, 3, 2L),
                    "milking a cow plays grow");
            milk(player, mooshroom);
            helper.assertTrue(!ServerBehaviorScheduler.trigger(mooshroom, EyeBehavior.BLINK, 3, 3L),
                    "milking a mooshroom plays grow");
            milk(player, goat);
            helper.assertTrue(!ServerBehaviorScheduler.trigger(goat, EyeBehavior.BLINK, 3, 4L),
                    "milking a goat plays grow");
        } finally {
            ServerBehaviorScheduler.clear();
            ServerConfig.AMBIENT_BEHAVIORS.set(ambient);
            ServerConfig.GROW_ON_MILK.set(onMilk);
            player.setItemInHand(InteractionHand.MAIN_HAND, held);
        }
        helper.succeed();
    }

    /**
     * A sleeping mob plays nothing: the admin trigger, heal, trade, affection, and ambient rolls are all
     * dropped until it wakes. In game: as an operator at night, look at a sleeping eyed villager and run
     * {@code /sg admin behavior swirl}; it reports the behavior dropped. Hit the villager to wake it and run
     * the command again, and it plays.
     */
    public static void sleepingMobPlaysNoBehaviors(GameTestHelper helper, ServerPlayer player) {
        boolean ambient = ServerConfig.AMBIENT_BEHAVIORS.get();
        int ambientMin = ServerConfig.AMBIENT_MIN_TICKS.get();
        int ambientMax = ServerConfig.AMBIENT_MAX_TICKS.get();
        boolean onHeal = ServerConfig.SWIRL_ON_HEAL.get();
        boolean onTrade = ServerConfig.SWIRL_ON_TRADE.get();
        boolean onAffection = ServerConfig.LOVE_ON_AFFECTION.get();
        ServerBehaviorScheduler.clear();
        try {
            // Ambient on with a one-tick cooldown, so every advanced tick tries an ambient roll.
            ServerConfig.AMBIENT_BEHAVIORS.set(true);
            ServerConfig.AMBIENT_MIN_TICKS.set(ServerConfig.TICKS_MIN);
            ServerConfig.AMBIENT_MAX_TICKS.set(ServerConfig.TICKS_MIN);
            ServerConfig.SWIRL_ON_HEAL.set(true);
            ServerConfig.SWIRL_ON_TRADE.set(true);
            ServerConfig.LOVE_ON_AFFECTION.set(true);
            Villager villager = eyedTrackedMob(helper, player, EntityType.VILLAGER, new BlockPos(2, 2, 2));
            villager.startSleeping(villager.blockPosition());

            helper.assertTrue(!ServerBehaviorScheduler.trigger(villager, EyeBehavior.STARE, 3, 1L),
                    "a trigger on a sleeping mob is dropped");
            ServerBehaviorScheduler.onHealed(villager);
            ServerBehaviorScheduler.onTrade(villager);
            helper.getLevel().broadcastEntityEvent(villager, EntityEvent.LOVE_HEARTS);
            advance(5);

            villager.stopSleeping();
            helper.assertTrue(ServerBehaviorScheduler.trigger(villager, EyeBehavior.BLINK, 3, 2L),
                    "no heal, trade, affection, or ambient behavior started while the mob slept");
        } finally {
            ServerBehaviorScheduler.clear();
            ServerConfig.AMBIENT_BEHAVIORS.set(ambient);
            ServerConfig.AMBIENT_MIN_TICKS.set(ambientMin);
            ServerConfig.AMBIENT_MAX_TICKS.set(ambientMax);
            ServerConfig.SWIRL_ON_HEAL.set(onHeal);
            ServerConfig.SWIRL_ON_TRADE.set(onTrade);
            ServerConfig.LOVE_ON_AFFECTION.set(onAffection);
        }
        helper.succeed();
    }

    /**
     * A sleeping mob's eyes ease into a squint, hold it until it wakes, and ease back open; a tracker built
     * for an already-sleeping mob starts squinted, and no behavior starts on the client while it sleeps.
     * Drives the client-side {@link GooglyTracker} directly against a server villager, since the squint is
     * derived from vanilla's synced sleep state rather than sent. In game: at night, watch an eyed villager
     * go to bed; its eyes narrow to a squint, stay that way until it wakes, then open again.
     */
    public static void sleepingMobSquints(GameTestHelper helper) {
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(villager.getType());
        HeadInfo headInfo = ServerEyeConfigs.resolve(type, villager, EyeState.getVariantRoll(villager));
        helper.assertTrue(headInfo != null, "villagers have an eye config");

        GooglyTracker tracker = new GooglyTracker(villager, headInfo);
        helper.assertTrue(allSquash(tracker, 1F), "an awake mob's eyes are open");

        villager.startSleeping(villager.blockPosition());
        tracker.update();
        helper.assertTrue(!allSquash(tracker, 1F) && !allSquash(tracker, GooglyTracker.SLEEP_SQUASH_Y),
                "falling asleep eases into the squint rather than snapping");
        stepTracker(tracker, GooglyTracker.SLEEP_EASE_TICKS);
        helper.assertTrue(allSquash(tracker, GooglyTracker.SLEEP_SQUASH_Y), "a sleeping mob squints");
        helper.assertTrue(!tracker.startBehavior(EyeBehavior.BLINK, 5, 1L, 0),
                "no behavior starts on the client while the mob sleeps");

        GooglyTracker joined = new GooglyTracker(villager, headInfo);
        helper.assertTrue(allSquash(joined, GooglyTracker.SLEEP_SQUASH_Y)
                        && allPrevSquash(joined, GooglyTracker.SLEEP_SQUASH_Y),
                "a tracker built for an already-sleeping mob starts squinted");

        villager.stopSleeping();
        stepTracker(tracker, GooglyTracker.SLEEP_EASE_TICKS + 1);
        helper.assertTrue(allSquash(tracker, 1F), "waking eases the eyes back open");
        helper.succeed();
    }

    private static void stepTracker(GooglyTracker tracker, int ticks) {
        for (int i = 0; i < ticks; i++) {
            tracker.update();
        }
    }

    private static boolean allSquash(GooglyTracker tracker, float expected) {
        for (GooglyTracker.EyeInfo[] head : tracker.eyes) {
            for (GooglyTracker.EyeInfo info : head) {
                if (Math.abs(info.squashY - expected) > SQUASH_TOLERANCE) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean allPrevSquash(GooglyTracker tracker, float expected) {
        for (GooglyTracker.EyeInfo[] head : tracker.eyes) {
            for (GooglyTracker.EyeInfo info : head) {
                if (Math.abs(info.prevSquashY - expected) > SQUASH_TOLERANCE) {
                    return false;
                }
            }
        }
        return true;
    }

    private static <T extends Mob> T eyedTrackedMob(GameTestHelper helper, ServerPlayer player,
                                                    EntityType<T> type, BlockPos pos) {
        T mob = helper.spawnWithNoFreeWill(type, pos);
        EyeState.setHasEyes(mob, true);
        ServerBehaviorScheduler.onStartTracking(mob, player);
        return mob;
    }

    /** Use an empty bucket on {@code mob} the way a player's right-click does. */
    private static void milk(ServerPlayer player, Mob mob) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        mob.interact(player, InteractionHand.MAIN_HAND);
    }

    /**
     * A tracked mob with no eyes is not an event target: heal and trade triggers no-op. No in-game form,
     * since an eyeless mob has nothing to show; guards that heal and trade hooks send nothing for eyeless
     * mobs.
     */
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
            helper.assertTrue(ServerBehaviorScheduler.trigger(cow, EyeBehavior.BLINK, 5, 1L),
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
