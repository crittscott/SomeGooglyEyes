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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.Piglin;
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
 * state, is covered here too, since sleep is the one state that blocks the schedule, and so is the
 * swelling creeper's rattle, derived the same way from vanilla's synced swell, the ramming goat's squint,
 * the angry red pupils, the creepy enderman's pulse, and the teleport jolt's fling. The mid-effect
 * start-tracking catch-up packet and the anger packet need a connected client and stay source-verified.
 */
public final class BehaviorSchedulerGameTestsLogic {

    // Float slack for comparing eased squash values, which accumulate a 1/SLEEP_EASE_TICKS step.
    private static final float SQUASH_TOLERANCE = 1e-4F;

    // Creeper rattle: ticks for swell-driven motion to die down, and swell ticks before and during the
    // measured window (20 in all, short of the 30-tick fuse). Pupil steps are unit-disk distance per tick.
    private static final int CALM_SETTLE_TICKS = 200;
    private static final int SWELL_RAMP_TICKS = 10;
    private static final int SWELL_WINDOW_TICKS = 10;
    private static final float CALM_PUPIL_STEP = 0.01F;
    private static final float AGITATED_PUPIL_STEP = 0.05F;

    // Jolt: pupil speed (unit-disk units per tick) a fresh fling stays above even after a wall bounce, and
    // how far into a jolt a late viewer joins.
    private static final float JOLTED_PUPIL_SPEED = 0.5F;
    private static final int JOLT_LATE_JOIN_TICKS = 10;
    // Goat ram: ticks for the lowered head to come back up after the ram ends (it rises two steps a tick).
    private static final int RAM_HEAD_RISE_TICKS = 10;

    // Float slack for comparing eased tints and pulsed pupil sizes.
    private static final float TINT_TOLERANCE = 1e-4F;
    // Creepy pulse: ticks covering two cycles of the slowest pulse, and a behavior lasting half of one.
    private static final int PULSE_SAMPLE_TICKS = (int) (2 * 20 / GooglyTracker.CREEPY_MIN_HZ);
    private static final int PULSE_BEHAVIOR_TICKS = (int) (20 / GooglyTracker.CREEPY_MIN_HZ / 2);

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
     * A teleport plays jolt, through the server's entity-event broadcast, and only while
     * {@code joltOnTeleport} is on. In game: provoke an eyed enderman until it teleports, or watch an eyed
     * player eat a chorus fruit, and its pupils are flung around its eyes. With {@code joltOnTeleport = false}
     * they are not.
     */
    public static void teleportPlaysJolt(GameTestHelper helper, ServerPlayer player) {
        boolean ambient = ServerConfig.AMBIENT_BEHAVIORS.get();
        boolean onTeleport = ServerConfig.JOLT_ON_TELEPORT.get();
        ServerBehaviorScheduler.clear();
        try {
            ServerConfig.AMBIENT_BEHAVIORS.set(false);
            EnderMan enderMan = eyedTrackedMob(helper, player, EntityType.ENDERMAN, new BlockPos(2, 2, 2));

            ServerConfig.JOLT_ON_TELEPORT.set(false);
            helper.getLevel().broadcastEntityEvent(enderMan, EntityEvent.TELEPORT);
            helper.assertTrue(ServerBehaviorScheduler.trigger(enderMan, EyeBehavior.BLINK, 3, 1L),
                    "joltOnTeleport=false suppresses the jolt");
            advance(3);

            ServerConfig.JOLT_ON_TELEPORT.set(true);
            helper.getLevel().broadcastEntityEvent(enderMan, EntityEvent.TELEPORT);
            helper.assertTrue(!ServerBehaviorScheduler.trigger(enderMan, EyeBehavior.BLINK, 3, 2L),
                    "a teleport plays jolt");
        } finally {
            ServerBehaviorScheduler.clear();
            ServerConfig.AMBIENT_BEHAVIORS.set(ambient);
            ServerConfig.JOLT_ON_TELEPORT.set(onTeleport);
        }
        helper.succeed();
    }

    /**
     * A jolt flings every resting pupil off at speed on its first tick, and does the same for a viewer who
     * starts watching partway into it. Drives the client-side {@link GooglyTracker} directly against a server
     * enderman. In game: as an operator, look at an eyed enderman and run {@code /sg admin behavior jolt};
     * its pupils shoot off and ricochet around its eyes before settling.
     */
    public static void joltFlingsPupils(GameTestHelper helper) {
        EnderMan enderMan = helper.spawnWithNoFreeWill(EntityType.ENDERMAN, new BlockPos(2, 2, 2));
        HeadInfo headInfo = headInfo(helper, enderMan);

        GooglyTracker tracker = new GooglyTracker(enderMan, headInfo, false);
        stepTracker(tracker, CALM_SETTLE_TICKS);
        helper.assertTrue(!allPupilsFasterThan(tracker, JOLTED_PUPIL_SPEED), "resting pupils are slow");
        helper.assertTrue(tracker.startBehavior(EyeBehavior.JOLT, EyeBehavior.JOLT.defaultDuration(), 1L, 0),
                "the jolt starts");
        tracker.update();
        helper.assertTrue(allPupilsFasterThan(tracker, JOLTED_PUPIL_SPEED), "a jolt flings every pupil");

        GooglyTracker joined = new GooglyTracker(enderMan, headInfo, false);
        stepTracker(joined, CALM_SETTLE_TICKS);
        helper.assertTrue(joined.startBehavior(EyeBehavior.JOLT, EyeBehavior.JOLT.defaultDuration(), 1L,
                JOLT_LATE_JOIN_TICKS), "a late viewer's jolt starts partway in");
        joined.update();
        helper.assertTrue(allPupilsFasterThan(joined, JOLTED_PUPIL_SPEED), "a late viewer still sees the fling");
        helper.succeed();
    }

    /**
     * A goat lowering its head to ram squints, still plays behaviors while it does, and opens its eyes again
     * once the ram ends and its head comes back up. Drives the client-side {@link GooglyTracker} directly
     * against a server goat given the ram entity events by hand, since the squint is derived from the head
     * position vanilla sets from them rather than sent. In game: stand in front of an eyed goat until it
     * lowers its head and charges; its eyes narrow for the charge and open again afterwards.
     */
    public static void rammingGoatSquints(GameTestHelper helper) {
        Goat goat = helper.spawnWithNoFreeWill(EntityType.GOAT, new BlockPos(2, 2, 2));
        GooglyTracker tracker = new GooglyTracker(goat, headInfo(helper, goat), false);
        helper.assertTrue(allSquash(tracker, 1F), "a calm goat's eyes are open");

        goat.handleEntityEvent(EntityEvent.START_RAM);
        stepGoat(goat, tracker, 1);
        helper.assertTrue(!allSquash(tracker, 1F) && !allSquash(tracker, GooglyTracker.SLEEP_SQUASH_Y),
                "lowering the head eases into the squint rather than snapping");
        stepGoat(goat, tracker, GooglyTracker.SLEEP_EASE_TICKS);
        helper.assertTrue(allSquash(tracker, GooglyTracker.SLEEP_SQUASH_Y), "a ramming goat squints");
        helper.assertTrue(tracker.startBehavior(EyeBehavior.GROW, 5, 1L, 0), "a ramming goat still plays a behavior");

        goat.handleEntityEvent(EntityEvent.END_RAM);
        stepGoat(goat, tracker, RAM_HEAD_RISE_TICKS + GooglyTracker.SLEEP_EASE_TICKS + 1);
        helper.assertTrue(allSquash(tracker, 1F), "once the ram ends the eyes open again");
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

        GooglyTracker tracker = new GooglyTracker(villager, headInfo, false);
        helper.assertTrue(allSquash(tracker, 1F), "an awake mob's eyes are open");

        villager.startSleeping(villager.blockPosition());
        tracker.update();
        helper.assertTrue(!allSquash(tracker, 1F) && !allSquash(tracker, GooglyTracker.SLEEP_SQUASH_Y),
                "falling asleep eases into the squint rather than snapping");
        stepTracker(tracker, GooglyTracker.SLEEP_EASE_TICKS);
        helper.assertTrue(allSquash(tracker, GooglyTracker.SLEEP_SQUASH_Y), "a sleeping mob squints");
        helper.assertTrue(!tracker.startBehavior(EyeBehavior.BLINK, 5, 1L, 0),
                "no behavior starts on the client while the mob sleeps");

        GooglyTracker joined = new GooglyTracker(villager, headInfo, false);
        helper.assertTrue(allSquash(joined, GooglyTracker.SLEEP_SQUASH_Y)
                        && allPrevSquash(joined, GooglyTracker.SLEEP_SQUASH_Y),
                "a tracker built for an already-sleeping mob starts squinted");

        villager.stopSleeping();
        stepTracker(tracker, GooglyTracker.SLEEP_EASE_TICKS + 1);
        helper.assertTrue(allSquash(tracker, 1F), "waking eases the eyes back open");
        helper.succeed();
    }

    /**
     * A swelling creeper's pupils keep rattling around its eyes, and settle again once it shrinks back; at
     * rest they hang still. Drives the client-side {@link GooglyTracker} directly against a server creeper
     * ticked by hand, since the agitation is derived from vanilla's synced swell rather than sent; the swell
     * is reversed well before the fuse runs out. In game: walk up to an eyed creeper until it hisses, then
     * back away; its pupils fly around frantically while it swells and drop back to rest after it shrinks.
     */
    public static void swellingCreeperRattles(GameTestHelper helper) {
        Creeper creeper = helper.spawnWithNoFreeWill(EntityType.CREEPER, new BlockPos(2, 2, 2));
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(creeper.getType());
        HeadInfo headInfo = ServerEyeConfigs.resolve(type, creeper, EyeState.getVariantRoll(creeper));
        helper.assertTrue(headInfo != null, "creepers have an eye config");

        GooglyTracker tracker = new GooglyTracker(creeper, headInfo, false);
        helper.assertTrue(meanPupilStep(creeper, tracker, CALM_SETTLE_TICKS) < CALM_PUPIL_STEP,
                "a calm creeper's pupils hang still");

        creeper.setSwellDir(1);
        meanPupilStep(creeper, tracker, SWELL_RAMP_TICKS);
        helper.assertTrue(meanPupilStep(creeper, tracker, SWELL_WINDOW_TICKS) > AGITATED_PUPIL_STEP,
                "a swelling creeper's pupils keep moving fast");

        creeper.setSwellDir(-1);
        meanPupilStep(creeper, tracker, CALM_SETTLE_TICKS);
        helper.assertTrue(meanPupilStep(creeper, tracker, CALM_SETTLE_TICKS) < CALM_PUPIL_STEP,
                "once the creeper shrinks back its pupils settle");
        helper.succeed();
    }

    /**
     * An attacking mob's pupils ease to red, stay red under a behavior, and ease back when it stops; the
     * server's anger report does the same, and a tracker built for a mob already reported angry starts red.
     * Drives the client-side {@link GooglyTracker} directly against a server zombie. In game: let an eyed
     * zombie chase you and its pupils turn red; outrun it until it gives up and they fade back.
     */
    public static void angryMobsHaveRedPupils(GameTestHelper helper) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        HeadInfo headInfo = headInfo(helper, zombie);

        GooglyTracker tracker = new GooglyTracker(zombie, headInfo, false);
        helper.assertTrue(allIrisTint(tracker, 0F), "a calm mob's pupils keep their color");

        zombie.setAggressive(true);
        tracker.update();
        helper.assertTrue(!allIrisTint(tracker, 0F) && !allIrisTint(tracker, 1F),
                "turning aggressive eases toward red rather than snapping");
        stepTracker(tracker, GooglyTracker.ANGER_EASE_TICKS);
        helper.assertTrue(allIrisTint(tracker, 1F), "an aggressive mob's pupils are red");
        helper.assertTrue(tracker.startBehavior(EyeBehavior.CROSS_EYE, 5, 1L, 0),
                "anger doesn't block a behavior");
        tracker.update();
        helper.assertTrue(allIrisTint(tracker, 1F), "the pupils stay red under a behavior");

        zombie.setAggressive(false);
        stepTracker(tracker, GooglyTracker.ANGER_EASE_TICKS + 1);
        helper.assertTrue(allIrisTint(tracker, 0F), "calming down eases the red away");

        tracker.angry = true;
        stepTracker(tracker, GooglyTracker.ANGER_EASE_TICKS + 1);
        helper.assertTrue(allIrisTint(tracker, 1F), "server-reported anger turns the pupils red");

        GooglyTracker joined = new GooglyTracker(zombie, headInfo, true);
        helper.assertTrue(allIrisTint(joined, 1F) && allPrevIrisTint(joined, 1F),
                "a tracker built for a mob already reported angry starts red");
        helper.succeed();
    }

    /**
     * The server counts a provoked neutral mob or a piglin with someone to be angry at as angry, and nothing
     * as angry while {@code angryEyes} is off. In game: hit an eyed iron golem and its pupils turn red even
     * when it is not swinging at you; with {@code angryEyes = false} they are red only while it attacks.
     */
    public static void serverOnlyAngerIsShared(GameTestHelper helper) {
        boolean angryEyes = ServerConfig.ANGRY_EYES.get();
        try {
            ServerConfig.ANGRY_EYES.set(true);
            IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(1, 2, 1));
            Piglin piglin = helper.spawnWithNoFreeWill(EntityType.PIGLIN, new BlockPos(3, 2, 1));
            Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(1, 2, 3));

            helper.assertTrue(!ServerBehaviorScheduler.isAngry(golem), "a calm golem isn't angry");
            golem.setRemainingPersistentAngerTime(100);
            helper.assertTrue(ServerBehaviorScheduler.isAngry(golem), "a provoked golem is angry");
            helper.assertTrue(!ServerBehaviorScheduler.isAngry(piglin), "a calm piglin isn't angry");
            piglin.getBrain().setMemory(MemoryModuleType.ANGRY_AT, cow.getUUID());
            helper.assertTrue(ServerBehaviorScheduler.isAngry(piglin), "a piglin with an anger target is angry");
            helper.assertTrue(!ServerBehaviorScheduler.isAngry(cow), "a cow is never angry");

            ServerConfig.ANGRY_EYES.set(false);
            helper.assertTrue(!ServerBehaviorScheduler.isAngry(golem) && !ServerBehaviorScheduler.isAngry(piglin),
                    "angryEyes=false shares no anger");
        } finally {
            ServerConfig.ANGRY_EYES.set(angryEyes);
        }
        helper.succeed();
    }

    /**
     * A creepy enderman's pupils swell and shrink around their size, keep pulsing under a behavior, and settle
     * when it calms down. Drives the client-side {@link GooglyTracker} directly against a server enderman made
     * creepy by giving it a target. In game: look an eyed enderman in the face; while it screams its pupils
     * throb, and they settle once it loses interest.
     */
    public static void creepyEndermanPulses(GameTestHelper helper) {
        EnderMan enderMan = helper.spawnWithNoFreeWill(EntityType.ENDERMAN, new BlockPos(1, 2, 1));
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(3, 2, 3));
        GooglyTracker tracker = new GooglyTracker(enderMan, headInfo(helper, enderMan), false);
        helper.assertTrue(irisScaleRange(tracker, PULSE_SAMPLE_TICKS)[1] <= 1F + TINT_TOLERANCE,
                "a calm enderman's pupils hold their size");

        enderMan.setTarget(cow);
        helper.assertTrue(enderMan.isCreepy(), "an enderman with a target is creepy");
        stepTracker(tracker, GooglyTracker.CREEPY_EASE_TICKS);
        float[] range = irisScaleRange(tracker, PULSE_SAMPLE_TICKS);
        float amplitude = GooglyTracker.CREEPY_PULSE_AMPLITUDE;
        helper.assertTrue(range[0] < 1F - amplitude / 2 && range[1] > 1F + amplitude / 2,
                "a creepy enderman's pupils swell and shrink");
        helper.assertTrue(range[0] >= 1F - amplitude - TINT_TOLERANCE
                        && range[1] <= 1F + amplitude + TINT_TOLERANCE,
                "the pulse stays within its amplitude");

        helper.assertTrue(tracker.startBehavior(EyeBehavior.CROSS_EYE, PULSE_BEHAVIOR_TICKS, 1L, 0),
                "a creepy enderman still plays a behavior");
        range = irisScaleRange(tracker, PULSE_BEHAVIOR_TICKS - 1);
        helper.assertTrue(range[1] - range[0] > amplitude / 2, "the pulse keeps going under the behavior");

        enderMan.setTarget(null);
        stepTracker(tracker, GooglyTracker.CREEPY_EASE_TICKS + 1);
        range = irisScaleRange(tracker, PULSE_SAMPLE_TICKS);
        helper.assertTrue(range[0] >= 1F - TINT_TOLERANCE && range[1] <= 1F + TINT_TOLERANCE,
                "once it calms down the pupils settle back to their size");
        helper.succeed();
    }

    private static HeadInfo headInfo(GameTestHelper helper, LivingEntity mob) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        HeadInfo headInfo = ServerEyeConfigs.resolve(type, mob, EyeState.getVariantRoll(mob));
        helper.assertTrue(headInfo != null, type + " has an eye config");
        return headInfo;
    }

    /** Steps the tracker; returns the smallest and largest pupil size multiplier seen on any eye. */
    private static float[] irisScaleRange(GooglyTracker tracker, int ticks) {
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int t = 0; t < ticks; t++) {
            tracker.update();
            for (GooglyTracker.EyeInfo[] head : tracker.eyes) {
                for (GooglyTracker.EyeInfo info : head) {
                    min = Math.min(min, info.irisScaleMul);
                    max = Math.max(max, info.irisScaleMul);
                }
            }
        }
        return new float[]{min, max};
    }

    private static boolean allIrisTint(GooglyTracker tracker, float expected) {
        for (GooglyTracker.EyeInfo[] head : tracker.eyes) {
            for (GooglyTracker.EyeInfo info : head) {
                if (Math.abs(info.irisTintAmount - expected) > TINT_TOLERANCE) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean allPrevIrisTint(GooglyTracker tracker, float expected) {
        for (GooglyTracker.EyeInfo[] head : tracker.eyes) {
            for (GooglyTracker.EyeInfo info : head) {
                if (Math.abs(info.prevIrisTintAmount - expected) > TINT_TOLERANCE) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Ticks {@code creeper} and its tracker together; returns the mean per-eye pupil step per tick. */
    private static float meanPupilStep(Creeper creeper, GooglyTracker tracker, int ticks) {
        float total = 0F;
        int samples = 0;
        for (int t = 0; t < ticks; t++) {
            creeper.tick();
            tracker.update();
            for (GooglyTracker.EyeInfo[] head : tracker.eyes) {
                for (GooglyTracker.EyeInfo info : head) {
                    float dx = info.deltaX - info.prevDeltaX;
                    float dy = info.deltaY - info.prevDeltaY;
                    total += (float) Math.sqrt(dx * dx + dy * dy);
                    samples++;
                }
            }
        }
        return total / samples;
    }

    /** Ticks {@code goat} and its tracker together, so its lowered head moves. */
    private static void stepGoat(Goat goat, GooglyTracker tracker, int ticks) {
        for (int t = 0; t < ticks; t++) {
            goat.tick();
            tracker.update();
        }
    }

    private static boolean allPupilsFasterThan(GooglyTracker tracker, float speed) {
        for (GooglyTracker.EyeInfo[] head : tracker.eyes) {
            for (GooglyTracker.EyeInfo info : head) {
                if (info.momentumX * info.momentumX + info.momentumY * info.momentumY <= speed * speed) {
                    return false;
                }
            }
        }
        return true;
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
