package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.OngoingBehavior;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Server-side coverage for {@link ServerBehaviorScheduler} — the authority for which behavior a mob is
 * playing and when. {@link BehaviorDeterminismGameTestsLogic} covers the client's animation derivation;
 * this drives the server clock by hand ({@code serverTick()}) and probes the schedule through
 * {@code trigger()}'s return (a dropped trigger means the mob is busy or asleep). Every test forces the
 * scheduler to a clean slate with {@code clear()} and force-and-restores the behavior config it touches;
 * ambient play is disabled so only the events under test can move the schedule, except where ambient
 * rolls are what's under test. The sleeping villager's lids, which the client derives from vanilla's synced sleep
 * state, is covered here too, since sleep is the one state that blocks the schedule, and so is the
 * swelling creeper's rattle, derived the same way from vanilla's synced swell, the ramming goat's squint,
 * the aiming archer's and player's one-eyed squint, the angry red pupils, and the creepy enderman's pulse, each
 * also with its server switch off. The
 * mid-effect start-tracking catch-up packet and the anger packet need a connected client and stay
 * source-verified.
 */
public final class BehaviorSchedulerGameTestsLogic {

    // Every ongoing behavior switched on, as by default.
    private static final BehaviorSettings ON = BehaviorSettings.DEFAULTS;

    // Float slack for comparing eased squash and lid values, which accumulate a 1/ticks easing step.
    private static final float SQUASH_TOLERANCE = 1e-4F;

    // Creeper rattle: separate settling and measurement windows; swell ramp and measurement total
    // 20 ticks, short of the 30-tick fuse. Pupil steps are unit-disk distance per tick.
    private static final int CALM_SETTLE_TICKS = 200;
    private static final int CALM_WINDOW_TICKS = 200;
    private static final long CREEPER_RATTLE_SEED = 1L;
    private static final int SWELL_RAMP_TICKS = 10;
    private static final int SWELL_WINDOW_TICKS = 10;
    private static final float CALM_PUPIL_STEP = 0.01F;
    private static final float AGITATED_PUPIL_STEP = 0.05F;

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
     * A skeleton drawing its bow squints the eye on its off-hand side and keeps the other open, still plays
     * behaviors while it does, and opens the eye again once it lets go; a left-handed skeleton squints the
     * other eye. Other archers don't: a pillager charging its crossbow keeps both eyes open. Drives the
     * client-side {@link GooglyTracker} directly against server mobs whose item use is set by hand, since
     * the squint is derived from the item use vanilla syncs rather than sent. In game: in survival, approach
     * an eyed skeleton; one eye narrows each time it draws its bow and opens when it fires.
     */
    public static void aimingArcherWinks(GameTestHelper helper) {
        Skeleton skeleton = armed(helper, EntityType.SKELETON, new ItemStack(Items.BOW), false,
                new BlockPos(1, 2, 1));
        GooglyTracker tracker = new GooglyTracker(skeleton, headInfo(helper, skeleton), false, ON);
        helper.assertTrue(allSquash(tracker, 1F), "an idle skeleton's eyes are open");

        skeleton.startUsingItem(InteractionHand.MAIN_HAND);
        stepTracker(tracker, 1);
        helper.assertTrue(!allSquash(tracker, 1F) && !winking(tracker, true),
                "drawing the bow eases into the squint rather than snapping");
        stepTracker(tracker, GooglyTracker.AIM_EASE_TICKS);
        helper.assertTrue(winking(tracker, true), "a right-handed skeleton drawing its bow squints its left eye");
        helper.assertTrue(tracker.startBehavior(EyeBehavior.GROW, 5, 1L, 0),
                "an aiming skeleton still plays a behavior");

        skeleton.stopUsingItem();
        stepTracker(tracker, 5 + GooglyTracker.AIM_EASE_TICKS + 1);
        helper.assertTrue(allSquash(tracker, 1F), "letting go of the bow opens the eye again");

        Skeleton lefty = armed(helper, EntityType.SKELETON, new ItemStack(Items.BOW), true,
                new BlockPos(3, 2, 1));
        GooglyTracker leftyTracker = new GooglyTracker(lefty, headInfo(helper, lefty), false, ON);
        lefty.startUsingItem(InteractionHand.MAIN_HAND);
        stepTracker(leftyTracker, GooglyTracker.AIM_EASE_TICKS);
        helper.assertTrue(winking(leftyTracker, false), "a left-handed skeleton squints its right eye");

        Pillager pillager = armed(helper, EntityType.PILLAGER, new ItemStack(Items.CROSSBOW), false,
                new BlockPos(1, 2, 3));
        GooglyTracker pillagerTracker = new GooglyTracker(pillager, headInfo(helper, pillager), false, ON);
        pillager.startUsingItem(InteractionHand.MAIN_HAND);
        stepTracker(pillagerTracker, GooglyTracker.AIM_EASE_TICKS);
        helper.assertTrue(allSquash(pillagerTracker, 1F),
                "only skeletons squint to aim: a pillager charging its crossbow keeps both eyes open");
        helper.succeed();
    }

    /**
     * A player drawing a bow closes the eye opposite the arm holding it for as long as the draw is held,
     * and opens it again on release: the left eye for a bow in a right-handed player's main hand, the right
     * eye for a bow in the off hand or a left-handed player's main hand. Charging a crossbow keeps both eyes
     * open. Drives the client-side {@link GooglyTracker} directly against mock players whose item use is set
     * by hand. In game: apply a Slimy Eye to yourself, switch to third person, and draw and hold a bow; the
     * eye on the side away from the bow narrows until you loose the arrow.
     */
    public static void aimingPlayerWinks(GameTestHelper helper) {
        Player archer = playerHolding(helper, new ItemStack(Items.BOW), InteractionHand.MAIN_HAND,
                HumanoidArm.RIGHT);
        GooglyTracker tracker = new GooglyTracker(archer, headInfo(helper, archer), false, ON);
        helper.assertTrue(allSquash(tracker, 1F), "an idle player's eyes are open");

        archer.startUsingItem(InteractionHand.MAIN_HAND);
        stepTracker(tracker, GooglyTracker.AIM_EASE_TICKS);
        helper.assertTrue(winking(tracker, true), "a right-handed player drawing a bow squints the left eye");
        stepTracker(tracker, GooglyTracker.AIM_EASE_TICKS);
        helper.assertTrue(winking(tracker, true), "holding the full draw keeps the squint");

        archer.stopUsingItem();
        stepTracker(tracker, GooglyTracker.AIM_EASE_TICKS + 1);
        helper.assertTrue(allSquash(tracker, 1F), "loosing the arrow opens the eye again");

        Player offHand = playerHolding(helper, new ItemStack(Items.BOW), InteractionHand.OFF_HAND,
                HumanoidArm.RIGHT);
        GooglyTracker offHandTracker = new GooglyTracker(offHand, headInfo(helper, offHand), false, ON);
        offHand.startUsingItem(InteractionHand.OFF_HAND);
        stepTracker(offHandTracker, GooglyTracker.AIM_EASE_TICKS);
        helper.assertTrue(winking(offHandTracker, false), "a bow drawn from the off hand squints the right eye");

        Player lefty = playerHolding(helper, new ItemStack(Items.BOW), InteractionHand.MAIN_HAND,
                HumanoidArm.LEFT);
        GooglyTracker leftyTracker = new GooglyTracker(lefty, headInfo(helper, lefty), false, ON);
        lefty.startUsingItem(InteractionHand.MAIN_HAND);
        stepTracker(leftyTracker, GooglyTracker.AIM_EASE_TICKS);
        helper.assertTrue(winking(leftyTracker, false), "a left-handed player squints the right eye");

        Player crossbowman = playerHolding(helper, new ItemStack(Items.CROSSBOW), InteractionHand.MAIN_HAND,
                HumanoidArm.RIGHT);
        GooglyTracker crossbowTracker = new GooglyTracker(crossbowman, headInfo(helper, crossbowman), false, ON);
        crossbowman.startUsingItem(InteractionHand.MAIN_HAND);
        stepTracker(crossbowTracker, GooglyTracker.AIM_EASE_TICKS);
        helper.assertTrue(allSquash(crossbowTracker, 1F),
                "a player charging a crossbow keeps both eyes open");
        helper.succeed();
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

    private static boolean allLid(GooglyTracker tracker, float expected) {
        for (GooglyTracker.EyeInfo[] head : tracker.eyes) {
            for (GooglyTracker.EyeInfo info : head) {
                if (Math.abs(info.lid - expected) > SQUASH_TOLERANCE) {
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

    private static boolean allPrevLid(GooglyTracker tracker, float expected) {
        for (GooglyTracker.EyeInfo[] head : tracker.eyes) {
            for (GooglyTracker.EyeInfo info : head) {
                if (Math.abs(info.prevLid - expected) > SQUASH_TOLERANCE) {
                    return false;
                }
            }
        }
        return true;
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

    /**
     * Server-reported anger survives renderer resets but ends with the entity or client level. In game:
     * provoke an eyed golem, leave tracking distance, let it calm, and return; its pupils are calm.
     * Repeat while another player stays near it, then change dimensions. Resource reload while the
     * angry golem remains loaded must keep its red pupils. This test drives the runtime operations;
     * real packet lookup and loader unload delivery require the connected-client reproduction.
     */
    public static void angerFollowsEntityLifetime(GameTestHelper helper) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        zombie.setAggressive(false);
        HeadInfo info = headInfo(helper, zombie);
        ClientEyeRuntime.onLevelChanged();
        BehaviorSettings originalBehaviors = ClientEyeConfigs.behaviors();
        ClientEyeConfigs.replaceAll(ClientEyeConfigs.all(), ClientEyeConfigs.googlyEyesEnabled(), ON);
        try {
            ClientEyeRuntime.setAngry(zombie, true);
            GooglyTracker initial = ClientEyeRuntime.get(zombie, info);
            helper.assertTrue(initial.angry && allIrisTint(initial, 1F),
                    "anger received before first render initializes red pupils");
            ClientEyeRuntime.setAngry(zombie, false);
            helper.assertTrue(!initial.angry, "a false report updates the existing tracker");
            stepTracker(initial, GooglyTracker.ANGER_EASE_TICKS + 1);
            helper.assertTrue(allIrisTint(initial, 0F), "pupils return to calm after a false report");
            ClientEyeRuntime.setAngry(zombie, true);
            ClientEyeRuntime.clear();
            helper.assertTrue(ClientEyeRuntime.get(zombie, info).angry,
                    "render tracker eviction must retain anger for a loaded entity");
            ClientEyeRuntime.onEntityRemoved(zombie);
            helper.assertTrue(ClientEyeRuntime.peek(zombie) == null, "entity removal deletes its tracker");
            Zombie replacement = EntityType.ZOMBIE.create(helper.getLevel());
            replacement.setId(zombie.getId());
            GooglyTracker calm = ClientEyeRuntime.get(replacement, info);
            helper.assertTrue(!calm.angry && allIrisTint(calm, 0F),
                    "a replacement entity with the same id must not inherit unloaded anger");
            ClientEyeRuntime.setAngry(replacement, true);
            ClientEyeRuntime.onLevelChanged();
            helper.assertTrue(ClientEyeRuntime.peek(replacement) == null
                            && !ClientEyeRuntime.get(replacement, info).angry,
                    "client world replacement drops both trackers and anger");
        } finally {
            ClientEyeRuntime.onLevelChanged();
            ClientEyeConfigs.replaceAll(ClientEyeConfigs.all(), ClientEyeConfigs.googlyEyesEnabled(), originalBehaviors);
        }
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

        GooglyTracker tracker = new GooglyTracker(zombie, headInfo, false, ON);
        helper.assertTrue(allIrisTint(tracker, 0F), "a calm mob's pupils keep their color");

        zombie.setAggressive(true);
        tracker.update(ON);
        helper.assertTrue(!allIrisTint(tracker, 0F) && !allIrisTint(tracker, 1F),
                "turning aggressive eases toward red rather than snapping");
        stepTracker(tracker, GooglyTracker.ANGER_EASE_TICKS);
        helper.assertTrue(allIrisTint(tracker, 1F), "an aggressive mob's pupils are red");
        helper.assertTrue(tracker.startBehavior(EyeBehavior.CROSS_EYE, 5, 1L, 0),
                "anger doesn't block a behavior");
        tracker.update(ON);
        helper.assertTrue(allIrisTint(tracker, 1F), "the pupils stay red under a behavior");

        zombie.setAggressive(false);
        stepTracker(tracker, GooglyTracker.ANGER_EASE_TICKS + 1);
        helper.assertTrue(allIrisTint(tracker, 0F), "calming down eases the red away");

        tracker.angry = true;
        stepTracker(tracker, GooglyTracker.ANGER_EASE_TICKS + 1);
        helper.assertTrue(allIrisTint(tracker, 1F), "server-reported anger turns the pupils red");

        GooglyTracker joined = new GooglyTracker(zombie, headInfo, true, ON);
        helper.assertTrue(allIrisTint(joined, 1F) && allPrevIrisTint(joined, 1F),
                "a tracker built for a mob already reported angry starts red");
        helper.succeed();
    }

    /** A mob of {@code type} holding {@code weapon} in its main hand, with the given handedness. */
    private static <T extends Mob> T armed(GameTestHelper helper, EntityType<T> type, ItemStack weapon,
                                           boolean leftHanded, BlockPos pos) {
        T mob = helper.spawnWithNoFreeWill(type, pos);
        mob.setLeftHanded(leftHanded);
        mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        return mob;
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
        GooglyTracker tracker = new GooglyTracker(enderMan, headInfo(helper, enderMan), false, ON);
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

    /**
     * Disabling the master switch suspends automatic reactions and ambient work but keeps tracked mobs
     * ready to resume. In game: turn googlyEyesEnabled off near an eyed cow, milk it, and turn the switch
     * back on; no hidden expression is waiting, and later milking starts a new grow normally.
     */
    public static void disabledEyesSuspendAutomaticBehaviors(GameTestHelper helper, ServerPlayer player) {
        boolean enabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
        boolean ambient = ServerConfig.AMBIENT_BEHAVIORS.get();
        int min = ServerConfig.AMBIENT_MIN_TICKS.get();
        int max = ServerConfig.AMBIENT_MAX_TICKS.get();
        boolean milk = ServerConfig.GROW_ON_MILK.get();
        ServerBehaviorScheduler.clear();
        try {
            ServerConfig.GOOGLY_EYES_ENABLED.set(false);
            ServerConfig.GROW_ON_MILK.set(true);
            ServerConfig.AMBIENT_BEHAVIORS.set(true);
            ServerConfig.AMBIENT_MIN_TICKS.set(ServerConfig.TICKS_MIN);
            ServerConfig.AMBIENT_MAX_TICKS.set(ServerConfig.TICKS_MIN);
            Cow cow = eyedTrackedMob(helper, player, EntityType.COW, new BlockPos(2, 2, 2));
            ServerBehaviorScheduler.onMilked(cow);
            advance(ServerConfig.TICKS_MIN + 1);
            helper.assertTrue(ServerBehaviorScheduler.trigger(cow, EyeBehavior.BLINK, 3, 1L),
                    "neither automatic reaction nor ambient work starts while disabled; explicit commands remain usable");
            advance(3);
            ServerConfig.AMBIENT_BEHAVIORS.set(false);
            ServerConfig.GOOGLY_EYES_ENABLED.set(true);
            ServerBehaviorScheduler.onMilked(cow);
            helper.assertTrue(!ServerBehaviorScheduler.trigger(cow, EyeBehavior.BLINK, 3, 2L),
                    "re-enabling resumes reactions using retained watcher bookkeeping");
        } finally {
            ServerBehaviorScheduler.clear();
            ServerConfig.GOOGLY_EYES_ENABLED.set(enabled);
            ServerConfig.AMBIENT_BEHAVIORS.set(ambient);
            ServerConfig.AMBIENT_MIN_TICKS.set(min);
            ServerConfig.AMBIENT_MAX_TICKS.set(max);
            ServerConfig.GROW_ON_MILK.set(milk);
        }
        helper.succeed();
    }

    private static <T extends Mob> T eyedTrackedMob(GameTestHelper helper, ServerPlayer player,
                                                    EntityType<T> type, BlockPos pos) {
        T mob = helper.spawnWithNoFreeWill(type, pos);
        EyeState.setHasEyes(mob, true);
        ServerBehaviorScheduler.onStartTracking(mob, player);
        return mob;
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

    private static HeadInfo headInfo(GameTestHelper helper, LivingEntity mob) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        HeadInfo headInfo = ServerEyeConfigs.resolve(type, mob, EyeState.getVariantRoll(mob));
        helper.assertTrue(headInfo != null, type + " has an eye config");
        return headInfo;
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

    /** Steps the tracker; returns the smallest and largest pupil size multiplier seen on any eye. */
    private static float[] irisScaleRange(GooglyTracker tracker, int ticks) {
        return irisScaleRange(tracker, ticks, ON);
    }

    private static float[] irisScaleRange(GooglyTracker tracker, int ticks, BehaviorSettings settings) {
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int t = 0; t < ticks; t++) {
            tracker.update(settings);
            for (GooglyTracker.EyeInfo[] head : tracker.eyes) {
                for (GooglyTracker.EyeInfo info : head) {
                    min = Math.min(min, info.irisScaleMul);
                    max = Math.max(max, info.irisScaleMul);
                }
            }
        }
        return new float[]{min, max};
    }

    /** Ticks {@code creeper} and its tracker together; returns the mean per-eye pupil step per tick. */
    private static float meanPupilStep(Creeper creeper, GooglyTracker tracker, int ticks) {
        return meanPupilStep(creeper, tracker, ticks, ON);
    }

    private static float meanPupilStep(Creeper creeper, GooglyTracker tracker, int ticks, BehaviorSettings settings) {
        float total = 0F;
        int samples = 0;
        for (int t = 0; t < ticks; t++) {
            creeper.tick();
            tracker.update(settings);
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

    /** Use an empty bucket on {@code mob} the way a player's right-click does. */
    private static void milk(ServerPlayer player, Mob mob) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        mob.interact(player, InteractionHand.MAIN_HAND);
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

    /** A fresh mock player with the given handedness, holding {@code weapon} in {@code hand}. */
    private static Player playerHolding(GameTestHelper helper, ItemStack weapon, InteractionHand hand,
                                        HumanoidArm mainArm) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setMainArm(mainArm);
        player.setItemInHand(hand, weapon);
        return player;
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
        GooglyTracker tracker = new GooglyTracker(goat, headInfo(helper, goat), false, ON);
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
     * A received expression survives first-render delay and render-cache replacement and expires while
     * offscreen. In game: watch an eyed cow with another client, trigger a swirl, and approach it on the
     * second client mid-expression; both see the same progress. Look away and back, and it has not paused.
     */
    public static void receivedExpressionsOutliveRenderTrackers(GameTestHelper helper) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        HeadInfo info = headInfo(helper, zombie);
        try {
            ClientEyeRuntime.onLevelChanged();
            ClientEyeRuntime.setBehavior(zombie, EyeBehavior.SWIRL, 12, 42L, 3);
            helper.assertTrue(ClientEyeRuntime.peek(zombie) == null, "receipt must not create a drawing cache");
            ClientEyeRuntime.tickBehaviors();
            GooglyTracker first = ClientEyeRuntime.get(zombie, info);
            helper.assertTrue(first.active != null && first.active.age == 4,
                    "first render catches up a previously received expression");
            ClientEyeRuntime.clear();
            ClientEyeRuntime.tickBehaviors();
            GooglyTracker rebuilt = ClientEyeRuntime.get(zombie, info);
            helper.assertTrue(rebuilt != first && rebuilt.active != null && rebuilt.active.age == 5,
                    "replacing a render tracker preserves expression progress");
            ClientEyeRuntime.tick(false);
            helper.assertTrue(ClientEyeRuntime.get(zombie, info).active.age == 5,
                    "paused client ticks do not advance expression time");
            ClientEyeRuntime.clear();
            for (int tick = 0; tick < 7; tick++) {
                ClientEyeRuntime.tickBehaviors();
            }
            helper.assertTrue(ClientEyeRuntime.get(zombie, info).active == null,
                    "unrendered expressions expire without a tracker");
            ClientEyeRuntime.setBehavior(zombie, EyeBehavior.BLINK, 8, 7L, 0);
            ClientEyeRuntime.onEntityRemoved(zombie);
            helper.assertTrue(ClientEyeRuntime.get(zombie, info).active == null,
                    "entity unloading removes received expression state");
            ClientEyeRuntime.setBehavior(zombie, EyeBehavior.BLINK, 8, 7L, 0);
            ClientEyeRuntime.onLevelChanged();
            helper.assertTrue(ClientEyeRuntime.get(zombie, info).active == null,
                    "world replacement removes received expression state");
        } finally {
            ClientEyeRuntime.onLevelChanged();
        }
        helper.succeed();
    }

    /**
     * The server counts a provoked neutral mob or a piglin with someone to be angry at as angry, and nothing
     * as angry while {@code angryEyes} is off. In game: hit an eyed iron golem and its pupils turn red even
     * when it is not swinging at you; with {@code angryEyes = false} neither attacking nor server-only anger makes them red.
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
     * Use shears on {@code mob} the way a player's right-click does. Goes through {@code Player#interactOn}
     * because Forge and NeoForge shear from the shears item rather than the sheep's {@code mobInteract}.
     */
    private static void shear(ServerPlayer player, Mob mob) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        player.interactOn(mob, InteractionHand.MAIN_HAND);
    }

    /**
     * Shearing a sheep plays grow, and only while {@code growOnShear} is on. In game: use shears on an eyed
     * sheep, and its eyes bulge as the wool drops. With {@code growOnShear = false} they do not.
     */
    public static void shearingPlaysGrow(GameTestHelper helper, ServerPlayer player) {
        boolean ambient = ServerConfig.AMBIENT_BEHAVIORS.get();
        boolean onShear = ServerConfig.GROW_ON_SHEAR.get();
        ItemStack held = player.getMainHandItem().copy();
        ServerBehaviorScheduler.clear();
        try {
            ServerConfig.AMBIENT_BEHAVIORS.set(false);
            Sheep quiet = eyedTrackedMob(helper, player, EntityType.SHEEP, new BlockPos(1, 2, 1));
            Sheep bulging = eyedTrackedMob(helper, player, EntityType.SHEEP, new BlockPos(3, 2, 1));

            ServerConfig.GROW_ON_SHEAR.set(false);
            shear(player, quiet);
            helper.assertTrue(quiet.isSheared(), "the shears sheared the first sheep");
            helper.assertTrue(ServerBehaviorScheduler.trigger(quiet, EyeBehavior.BLINK, 3, 1L),
                    "growOnShear=false suppresses the shearing grow");

            ServerConfig.GROW_ON_SHEAR.set(true);
            shear(player, bulging);
            helper.assertTrue(bulging.isSheared(), "the shears sheared the second sheep");
            helper.assertTrue(!ServerBehaviorScheduler.trigger(bulging, EyeBehavior.BLINK, 3, 2L),
                    "shearing a sheep plays grow");
        } finally {
            ServerBehaviorScheduler.clear();
            ServerConfig.AMBIENT_BEHAVIORS.set(ambient);
            ServerConfig.GROW_ON_SHEAR.set(onShear);
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
     * A sleeping villager's eyelids close to a slit over its full-size eyes, stay closed until it wakes, and
     * open back out of sight; a tracker built for an already-sleeping villager starts with them closed, and
     * no behavior starts on the client while it sleeps. Drives the client-side {@link GooglyTracker}
     * directly against a server villager, since the lids are derived from vanilla's synced sleep state rather
     * than sent. In game: at night, watch an eyed villager go to bed; tan lids slide in over each eye from
     * above and below, leaving a slit across the middle, stay that way until it wakes, then slide back out
     * and vanish.
     */
    public static void sleepingVillagerClosesLids(GameTestHelper helper) {
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(2, 2, 2));
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(villager.getType());
        HeadInfo headInfo = ServerEyeConfigs.resolve(type, villager, EyeState.getVariantRoll(villager));
        helper.assertTrue(headInfo != null, "villagers have an eye config");

        GooglyTracker tracker = new GooglyTracker(villager, headInfo, false, ON);
        helper.assertTrue(allLid(tracker, 0F), "an awake villager has no lids");

        villager.startSleeping(villager.blockPosition());
        tracker.update(ON);
        helper.assertTrue(!allLid(tracker, 0F) && !allLid(tracker, GooglyTracker.SLEEP_LID_COVERAGE),
                "falling asleep slides the lids closed rather than snapping them shut");
        stepTracker(tracker, GooglyTracker.SLEEP_LID_EASE_TICKS);
        helper.assertTrue(allLid(tracker, GooglyTracker.SLEEP_LID_COVERAGE), "a sleeping villager's lids are closed");
        helper.assertTrue(allSquash(tracker, 1F), "a sleeping villager's eyes keep their shape under the lids");
        helper.assertTrue(!tracker.startBehavior(EyeBehavior.BLINK, 5, 1L, 0),
                "no behavior starts on the client while the mob sleeps");

        GooglyTracker joined = new GooglyTracker(villager, headInfo, false, ON);
        helper.assertTrue(allLid(joined, GooglyTracker.SLEEP_LID_COVERAGE)
                        && allPrevLid(joined, GooglyTracker.SLEEP_LID_COVERAGE),
                "a tracker built for an already-sleeping villager starts with its lids closed");

        villager.stopSleeping();
        stepTracker(tracker, GooglyTracker.SLEEP_LID_EASE_TICKS + 1);
        helper.assertTrue(allLid(tracker, 0F), "waking slides the lids back open out of sight");
        helper.succeed();
    }

    /** Ticks {@code goat} and its tracker together, so its lowered head moves. */
    private static void stepGoat(Goat goat, GooglyTracker tracker, int ticks) {
        stepGoat(goat, tracker, ticks, ON);
    }

    private static void stepGoat(Goat goat, GooglyTracker tracker, int ticks, BehaviorSettings settings) {
        for (int t = 0; t < ticks; t++) {
            goat.tick();
            tracker.update(settings);
        }
    }

    private static void stepTracker(GooglyTracker tracker, int ticks) {
        stepTracker(tracker, ticks, ON);
    }

    private static void stepTracker(GooglyTracker tracker, int ticks, BehaviorSettings settings) {
        for (int i = 0; i < ticks; i++) {
            tracker.update(settings);
        }
    }

    /**
     * A swelling creeper's pupils keep rattling around its eyes, and settle again once it shrinks back; at
     * rest they hang still. Drives the client-side {@link GooglyTracker} directly against a server creeper
     * ticked by hand, since the agitation is derived from vanilla's synced swell rather than sent; the swell
     * is reversed well before the fuse runs out. The fixture floats motionless, uses seeded pupil noise,
     * and discards settling ticks before measuring either calm window.
     * In game: walk up to an eyed creeper until it hisses, then
     * back away; its pupils fly around frantically while it swells and drop back to rest after it shrinks.
     */
    public static void swellingCreeperRattles(GameTestHelper helper) {
        Creeper creeper = helper.spawnWithNoFreeWill(EntityType.CREEPER, new BlockPos(2, 2, 2));
        creeper.setNoGravity(true);
        creeper.setDeltaMovement(Vec3.ZERO);
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(creeper.getType());
        HeadInfo headInfo = ServerEyeConfigs.resolve(type, creeper, EyeState.getVariantRoll(creeper));
        helper.assertTrue(headInfo != null && headInfo.hasConfig(), "creepers have usable eye geometry");

        GooglyTracker tracker = new GooglyTracker(creeper, headInfo, false, ON);
        tracker.rand.setSeed(CREEPER_RATTLE_SEED);
        meanPupilStep(creeper, tracker, CALM_SETTLE_TICKS);
        float calmStep = meanPupilStep(creeper, tracker, CALM_WINDOW_TICKS);
        helper.assertTrue(calmStep < CALM_PUPIL_STEP,
                "a calm creeper's mean pupil step must be below " + CALM_PUPIL_STEP + ", got " + calmStep);

        creeper.setSwellDir(1);
        meanPupilStep(creeper, tracker, SWELL_RAMP_TICKS);
        float swellingStep = meanPupilStep(creeper, tracker, SWELL_WINDOW_TICKS);
        helper.assertTrue(swellingStep > AGITATED_PUPIL_STEP,
                "a swelling creeper's mean pupil step must exceed " + AGITATED_PUPIL_STEP + ", got " + swellingStep);

        creeper.setSwellDir(-1);
        meanPupilStep(creeper, tracker, CALM_SETTLE_TICKS);
        float settledStep = meanPupilStep(creeper, tracker, CALM_WINDOW_TICKS);
        helper.assertTrue(settledStep < CALM_PUPIL_STEP,
                "a settled creeper's mean pupil step must be below " + CALM_PUPIL_STEP + ", got " + settledStep);
        helper.succeed();
    }

    /**
     * Each ongoing behavior stays neutral while its server switch is off, and a sleeping mob still plays no
     * expression with its squint off. Drives the client-side {@link GooglyTracker} directly with settings that
     * switch one behavior off at a time, against the same vanilla states the "on" tests use. In game, with
     * each key set to {@code false} in {@code somegoogly-server.toml}: with {@code squintOnSleep}, a sleeping
     * eyed villager shows no lids and a sleeping player with a Slimy Eye keeps their eyes open; with
     * {@code squintOnRam}, a charging eyed goat keeps its eyes open; with {@code winkOnAim}, an eyed skeleton
     * drawing its bow keeps both eyes open; with {@code rattleOnSwell}, a hissing eyed creeper's pupils hang
     * still; with {@code pulseWhenCreepy}, a screaming eyed enderman's pupils hold their size; with
     * {@code angryEyes}, an eyed zombie chasing you keeps its pupil color.
     */
    public static void switchedOffOngoingBehaviorsStayNeutral(GameTestHelper helper) {
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        BehaviorSettings noSleep = without(OngoingBehavior.SLEEP_SQUINT);
        GooglyTracker villagerTracker = new GooglyTracker(villager, headInfo(helper, villager), false, noSleep);
        villager.startSleeping(villager.blockPosition());
        stepTracker(villagerTracker, GooglyTracker.SLEEP_LID_EASE_TICKS + 1, noSleep);
        helper.assertTrue(allLid(villagerTracker, 0F) && allSquash(villagerTracker, 1F),
                "squintOnSleep=false: a sleeping villager shows no lids");
        helper.assertTrue(!villagerTracker.startBehavior(EyeBehavior.BLINK, 5, 1L, 0),
                "squintOnSleep=false: a sleeping mob still starts no behavior");

        Player sleeper = helper.makeMockPlayer(GameType.SURVIVAL);
        GooglyTracker sleeperTracker = new GooglyTracker(sleeper, headInfo(helper, sleeper), false, ON);
        sleeper.startSleeping(sleeper.blockPosition());
        stepTracker(sleeperTracker, GooglyTracker.SLEEP_EASE_TICKS + 1);
        helper.assertTrue(allSquash(sleeperTracker, GooglyTracker.SLEEP_SQUASH_Y), "a sleeping player squints");
        stepTracker(sleeperTracker, GooglyTracker.SLEEP_EASE_TICKS + 1, noSleep);
        helper.assertTrue(allSquash(sleeperTracker, 1F), "squintOnSleep=false: a sleeping player's eyes open");

        Goat goat = helper.spawnWithNoFreeWill(EntityType.GOAT, new BlockPos(3, 2, 1));
        BehaviorSettings noRam = without(OngoingBehavior.RAM_SQUINT);
        GooglyTracker goatTracker = new GooglyTracker(goat, headInfo(helper, goat), false, noRam);
        goat.handleEntityEvent(EntityEvent.START_RAM);
        stepGoat(goat, goatTracker, GooglyTracker.SLEEP_EASE_TICKS + 1, noRam);
        helper.assertTrue(allSquash(goatTracker, 1F), "squintOnRam=false: a ramming goat's eyes stay open");

        Skeleton skeleton = armed(helper, EntityType.SKELETON, new ItemStack(Items.BOW), false,
                new BlockPos(1, 2, 3));
        BehaviorSettings noAim = without(OngoingBehavior.AIM_WINK);
        GooglyTracker skeletonTracker = new GooglyTracker(skeleton, headInfo(helper, skeleton), false, noAim);
        skeleton.startUsingItem(InteractionHand.MAIN_HAND);
        stepTracker(skeletonTracker, GooglyTracker.AIM_EASE_TICKS + 1, noAim);
        helper.assertTrue(allSquash(skeletonTracker, 1F), "winkOnAim=false: an aiming skeleton keeps both eyes open");

        Creeper creeper = helper.spawnWithNoFreeWill(EntityType.CREEPER, new BlockPos(3, 2, 3));
        BehaviorSettings noRattle = without(OngoingBehavior.SWELL_RATTLE);
        GooglyTracker creeperTracker = new GooglyTracker(creeper, headInfo(helper, creeper), false, noRattle);
        meanPupilStep(creeper, creeperTracker, CALM_SETTLE_TICKS, noRattle);
        creeper.setSwellDir(1);
        meanPupilStep(creeper, creeperTracker, SWELL_RAMP_TICKS, noRattle);
        helper.assertTrue(meanPupilStep(creeper, creeperTracker, SWELL_WINDOW_TICKS, noRattle) < CALM_PUPIL_STEP,
                "rattleOnSwell=false: a swelling creeper's pupils hang still");
        creeper.setSwellDir(-1);

        EnderMan enderMan = helper.spawnWithNoFreeWill(EntityType.ENDERMAN, new BlockPos(2, 2, 2));
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 3));
        BehaviorSettings noPulse = without(OngoingBehavior.CREEPY_PULSE);
        GooglyTracker enderTracker = new GooglyTracker(enderMan, headInfo(helper, enderMan), false, noPulse);
        enderMan.setTarget(cow);
        float[] range = irisScaleRange(enderTracker, PULSE_SAMPLE_TICKS, noPulse);
        helper.assertTrue(range[0] >= 1F - TINT_TOLERANCE && range[1] <= 1F + TINT_TOLERANCE,
                "pulseWhenCreepy=false: a creepy enderman's pupils hold their size");
        enderMan.setTarget(null);

        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 1));
        BehaviorSettings noAnger = without(OngoingBehavior.ANGER);
        GooglyTracker zombieTracker = new GooglyTracker(zombie, headInfo(helper, zombie), true, noAnger);
        helper.assertTrue(allIrisTint(zombieTracker, 0F), "angryEyes=false: a mob reported angry starts calm");
        zombie.setAggressive(true);
        stepTracker(zombieTracker, GooglyTracker.ANGER_EASE_TICKS + 1, noAnger);
        helper.assertTrue(allIrisTint(zombieTracker, 0F), "angryEyes=false: an attacking mob keeps its pupil color");
        helper.succeed();
    }

    /**
     * Whether head 0 is aim-squinting: the eye farthest to the mob's left ({@code left}) or right is fully
     * squinted for aiming, and every other eye is open. Eye positions are in head model space, where +X is
     * the mob's right.
     */
    private static boolean winking(GooglyTracker tracker, boolean left) {
        GooglyTracker.EyeInfo[] head = tracker.eyes[0];
        int closed = 0;
        for (int i = 1; i < head.length; i++) {
            double x = tracker.helper.placementAt(0, i).position().x;
            double closedX = tracker.helper.placementAt(0, closed).position().x;
            if (left ? x < closedX : x > closedX) {
                closed = i;
            }
        }
        for (int i = 0; i < head.length; i++) {
            float expected = i == closed ? GooglyTracker.AIM_SQUASH_Y : 1F;
            if (Math.abs(head[i].squashY - expected) > SQUASH_TOLERANCE) {
                return false;
            }
        }
        return true;
    }

    /** The default settings with {@code off} switched off. */
    private static BehaviorSettings without(OngoingBehavior off) {
        EnumSet<OngoingBehavior> on = EnumSet.noneOf(OngoingBehavior.class);
        on.addAll(ON.ongoing());
        on.remove(off);
        return new BehaviorSettings(ON.ambient(), ON.minTicks(), ON.maxTicks(), ON.pool(), ON.growOnHitPercent(),
                ON.swirlOnHeal(), ON.swirlHealCooldownTicks(), on);
    }



}
