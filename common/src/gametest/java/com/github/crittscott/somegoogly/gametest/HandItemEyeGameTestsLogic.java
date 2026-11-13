package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState.Snapshot;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.registry.ModContent;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3f;

/** Hand simulation checks; holder attachment and extracted draws need the client checklist. */
public final class HandItemEyeGameTestsLogic {

    private static final UUID FIRST = new UUID(1L, 2L);
    private static final int GRAVITY_TICKS = 8;
    private static final int IDLE_TICKS = 12;
    private static final double MOTION_STEP = 0.05D;
    private static final float PHYSICS_TOLERANCE = 0.0001F;
    private static final int OBSERVATION_TICKS = 400;
    private static final UUID SECOND = new UUID(3L, 4L);

    private HandItemEyeGameTestsLogic() {
    }

    private static ItemStack eye() {
        return new ItemStack(ModContent.GOOGLY_EYE.get());
    }

    /**
     * A held eye's pupil falls toward world-down whatever the camera angle or the item's display scale, a new pupil
     * starts already resting toward the ground, and an eye facing straight up rests centered. In game: set {@code
     * ambientBehaviors = false} in {@code somegoogly-server.toml} and {@code /reload}, hold a Googly Eye, and switch to
     * both third-person views with F5; as you look up and down the pupil stays toward the ground. Switch the eye away
     * and back; its pupil is already down rather than dropping from the center.
     */
    public static void handEyesFollowWorldGravityAcrossViews(GameTestHelper helper) {
        var localToWorld = new Matrix3f().rotationZ((float) (Math.PI / 2D)).scale(4F);
        var rotated = PresentationEyes.create(FIRST, new ItemStack(ModContent.GOOGLY_EYE.get()));
        rotated.eye.capturePose(localToWorld);
        helper.assertTrue(Math.abs(rotated.eye.gravX - 1F) < PHYSICS_TOLERANCE
                        && Math.abs(rotated.eye.gravY) < PHYSICS_TOLERANCE,
                Component.literal("a world-aligned pose keeps its gravity and display scale does not weaken it"));
        helper.assertTrue(Math.abs(rotated.eye.deltaX - 1F) < PHYSICS_TOLERANCE
                        && Math.abs(rotated.eye.prevDeltaX - 1F) < PHYSICS_TOLERANCE
                        && Math.abs(rotated.eye.deltaY) < PHYSICS_TOLERANCE,
                Component.literal("the first capture rests a new pupil on the rim toward gravity"));
        for (int tick = 0; tick < GRAVITY_TICKS; tick++) {
            rotated.tick(tick, BehaviorSettings.NONE);
        }
        helper.assertTrue(rotated.eye.deltaX > 0.5F && Math.abs(rotated.eye.deltaY) < 0.1F,
                Component.literal("the pupil falls along projected world-down"));
        var horizontal = PresentationEyes.create(FIRST, new ItemStack(ModContent.GOOGLY_EYE.get()));
        horizontal.eye.capturePose(new Matrix3f().rotationX((float) (Math.PI / 2D)));
        helper.assertTrue(Math.abs(horizontal.eye.gravX) < PHYSICS_TOLERANCE
                        && Math.abs(horizontal.eye.gravY) < PHYSICS_TOLERANCE,
                Component.literal("an upward-facing eye has no in-plane gravity"));
        helper.assertTrue(horizontal.eye.deltaX == 0F && horizontal.eye.deltaY == 0F,
                Component.literal("with no in-plane gravity a new pupil stays centered"));
        helper.succeed();
    }

    /**
     * A held eye's pupil reacts to its holder walking, jumping, and turning, and a holder who reappears far away does
     * not jolt it. In game: set {@code ambientBehaviors = false} in {@code somegoogly-server.toml} and {@code /reload},
     * hold a Googly Eye, press F5, and start and stop walking or jump; the pupil lags each change of speed and settles
     * when you stop.
     */
    public static void handEyesReactToHolderMotion(GameTestHelper helper) {
        TestEyeRuntime.HandCache cache = new TestEyeRuntime.HandCache();
        LivingEntity moving = holder(helper, FIRST);
        LivingEntity still = holder(helper, SECOND);
        var movingEyes = cache.get(moving, InteractionHand.MAIN_HAND, eye());
        var stillEyes = cache.get(still, InteractionHand.MAIN_HAND, eye());
        movingEyes.eye.capturePose(new Matrix3f().scaling(-1F, -1F, 1F));
        stillEyes.eye.capturePose(new Matrix3f().scaling(-1F, -1F, 1F));
        for (int tick = 0; tick < 2; tick++) {
            cache.get(moving, InteractionHand.MAIN_HAND, eye());
            cache.get(still, InteractionHand.MAIN_HAND, eye());
            cache.tick(tick, true, BehaviorSettings.NONE);
        }
        moving.setPos(moving.getX() + MOTION_STEP, moving.getY(), moving.getZ());
        cache.get(moving, InteractionHand.MAIN_HAND, eye());
        cache.get(still, InteractionHand.MAIN_HAND, eye());
        cache.tick(2L, true, BehaviorSettings.NONE);
        helper.assertTrue(movingEyes.eye.deltaX < stillEyes.eye.deltaX - 0.2F,
                Component.literal("movement acceleration reaches the actual holder's pupil simulation"));
        moving.setYHeadRot(35F);
        moving.setXRot(20F);
        cache.get(moving, InteractionHand.MAIN_HAND, eye());
        cache.tick(3L, true, BehaviorSettings.NONE);
        helper.assertTrue(movingEyes.eye.rotationYaw == moving.getYHeadRot()
                        && movingEyes.eye.rotationPitch == moving.getXRot(), Component.literal("holder rotation reaches the physics inputs"));
        float before = movingEyes.eye.deltaX;
        moving.setPos(moving.getX() + 100D, moving.getY(), moving.getZ());
        cache.get(moving, InteractionHand.MAIN_HAND, eye());
        cache.tick(100L, true, BehaviorSettings.NONE);
        helper.assertTrue(movingEyes.eye.deltaX == before, Component.literal("offscreen time gaps re-prime motion instead of kicking from stale positions"));
        helper.succeed();
    }

    /**
     * A held eye keeps its pupil when the stack's count changes and starts a new one when its appearance changes or
     * eyes are shown again; plain heads, ordinary items, and empty hands have none. In game: in third person, hold a
     * stack of Googly Eyes, drop one, and watch the pupil keep moving; swap to a glowing eye, then a plain skull, then
     * a diamond; only the eyes show pupils, each with its own look.
     */
    public static void handEyesReplaceContentsAndHonorGates(GameTestHelper helper) {
        TestEyeRuntime.HandCache cache = new TestEyeRuntime.HandCache();
        LivingEntity firstHolder = holder(helper, FIRST);
        ItemStack stack = eye();
        var first = cache.get(firstHolder, InteractionHand.MAIN_HAND, stack);
        ItemStack copy = stack.copy();
        copy.setCount(2);
        helper.assertTrue(cache.get(firstHolder, InteractionHand.MAIN_HAND, copy) == first,
                Component.literal("stack copies and counts retain the hand simulation"));
        EyeItemProperties.set(copy, AppearanceOverride.EMPTY.withGlow(true));
        var changed = cache.get(firstHolder, InteractionHand.MAIN_HAND, copy);
        helper.assertTrue(changed != first, Component.literal("appearance changes replace the hand simulation"));
        cache.clear();
        helper.assertTrue(cache.get(firstHolder, InteractionHand.MAIN_HAND, copy) != changed,
                Component.literal("hiding eyes clears hand animation, so showing them again starts fresh"));
        helper.assertTrue(cache.get(firstHolder, InteractionHand.MAIN_HAND, new ItemStack(Items.SKELETON_SKULL)) == null,
                Component.literal("plain heads do not acquire eyes"));
        helper.assertTrue(cache.get(firstHolder, InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND)) == null,
                Component.literal("ordinary items are unsupported"));
        helper.assertTrue(cache.get(firstHolder, InteractionHand.MAIN_HAND, ItemStack.EMPTY) == null,
                Component.literal("empty hands are unsupported"));
        helper.succeed();
    }

    /**
     * A holder that unloads drops both hands' eye state, unseen hands expire, and resource or world resets clear it
     * all. No visible in-game form, since a fresh pupil starts at rest where the old one settled; guards that removed
     * and unseen holders leave no state behind.
     */
    public static void handEyesRetireUnloadedAndHiddenHolders(GameTestHelper helper) {
        TestEyeRuntime.HandCache cache = new TestEyeRuntime.HandCache();
        LivingEntity firstHolder = holder(helper, FIRST);
        LivingEntity secondHolder = holder(helper, SECOND);
        var first = cache.get(firstHolder, InteractionHand.MAIN_HAND, eye());
        var offhand = cache.get(firstHolder, InteractionHand.OFF_HAND, eye());
        var other = cache.get(secondHolder, InteractionHand.MAIN_HAND, eye());
        cache.remove(FIRST);
        helper.assertTrue(cache.get(firstHolder, InteractionHand.MAIN_HAND, eye()) != first
                        && cache.get(firstHolder, InteractionHand.OFF_HAND, eye()) != offhand,
                Component.literal("entity removal drops both hands"));
        helper.assertTrue(cache.get(secondHolder, InteractionHand.MAIN_HAND, eye()) == other,
                Component.literal("another holder survives removal"));
        for (int tick = 0; tick < IDLE_TICKS; tick++) {
            cache.tick(tick, true, BehaviorSettings.NONE);
        }
        helper.assertTrue(cache.get(secondHolder, InteractionHand.MAIN_HAND, eye()) != other,
                Component.literal("unseen hands expire"));
        var current = cache.get(secondHolder, InteractionHand.MAIN_HAND, eye());
        cache.clear();
        helper.assertTrue(cache.get(secondHolder, InteractionHand.MAIN_HAND, eye()) != current,
                Component.literal("resource/world reset drops hand state"));
        helper.succeed();
    }

    /**
     * Each holder and each hand has its own pupil and expression schedule, and held pupils use world gravity. In game:
     * with {@code ambientBehaviorPool = ["somegoogly:blink"]} in {@code somegoogly-server.toml}, hold Googly Eyes in
     * both hands beside another player who holds one, and press F5; all three eyes blink at different times.
     */
    public static void handEyesSeparateHoldersAndHands(GameTestHelper helper) {
        TestEyeRuntime.HandCache cache = new TestEyeRuntime.HandCache();
        LivingEntity firstHolder = holder(helper, FIRST);
        LivingEntity secondHolder = holder(helper, SECOND);
        var main = cache.get(firstHolder, InteractionHand.MAIN_HAND, eye());
        var offhand = cache.get(firstHolder, InteractionHand.OFF_HAND, eye());
        var other = cache.get(secondHolder, InteractionHand.MAIN_HAND, eye());
        helper.assertTrue(main != offhand && main != other && offhand != other,
                Component.literal("each holder and hand owns its pupils"));
        helper.assertTrue(main.eye.itemGravity && !main.eye.guiGravity
                        && offhand.eye.itemGravity && !offhand.eye.guiGravity,
                Component.literal("hand pupils use world orientation"));
        helper.succeed();
    }

    /**
     * Held eyes follow world time: pausing freezes their expressions, resuming continues them, and turning ambient
     * expressions off stops them while the pupils keep moving. In game: in single player with {@code
     * ambientBehaviorPool = ["somegoogly:blink"]} and {@code ambientMinTicks = 1} and {@code ambientMaxTicks = 1} in
     * {@code somegoogly-server.toml}, hold a Googly Eye in F5 and press Esc; the held eye stops mid-blink while the
     * same eye in your inventory keeps blinking.
     */
    public static void handEyesUseWorldClockAndPause(GameTestHelper helper) {
        TestEyeRuntime.HandCache cache = new TestEyeRuntime.HandCache();
        LivingEntity firstHolder = holder(helper, FIRST);
        var eye = cache.get(firstHolder, InteractionHand.MAIN_HAND, eye());
        BehaviorSettings defaults = TestBehaviorSettings.DEFAULTS;
        BehaviorSettings blink = new BehaviorSettings(true, 1, 1, List.of(EyeBehavior.BLINK),
                0, false, defaults.swirlHealCooldownTicks(), defaults.ongoing());
        cache.tick(1L, true, blink);
        helper.assertTrue(eye.active != null && eye.active.age == 1, Component.literal("world time selects expression age"));
        cache.get(firstHolder, InteractionHand.MAIN_HAND, eye());
        cache.tick(2L, false, blink);
        helper.assertTrue(eye.active.age == 1, Component.literal("pause freezes the hand expression"));
        cache.get(firstHolder, InteractionHand.MAIN_HAND, eye());
        cache.tick(2L, true, blink);
        helper.assertTrue(eye.active.age == 2, Component.literal("resume follows world time"));
        cache.get(firstHolder, InteractionHand.MAIN_HAND, eye());
        cache.tick(3L, true, BehaviorSettings.NONE);
        helper.assertTrue(eye.active == null, Component.literal("ambient off removes expressions while physics remains"));
        helper.succeed();
    }

    /**
     * A held eyed head shows its mob's eyes in place, and both can cross. In game: set {@code ambientBehaviorPool =
     * ["somegoogly:cross_eye"]} and {@code ambientMinTicks = 1} and {@code ambientMaxTicks = 1} in {@code
     * somegoogly-server.toml}, {@code /reload}, then hold an eyed skeleton skull and press F5; its pupils cross toward
     * each other.
     */
    public static void handHeadUsesConfiguredLayout(GameTestHelper helper) {
        var definitions = ClientEyeConfigs.all();
        BehaviorSettings behaviors = ClientEyeConfigs.behaviors();
        boolean enabled = ClientEyeConfigs.googlyEyesEnabled();
        try {
            ClientEyeConfigs.replaceAll(ServerEyeConfigs.all());
            ClientEyeConfigs.applySettings(true, TestBehaviorSettings.DEFAULTS);
            TestEyeRuntime.HandCache cache = new TestEyeRuntime.HandCache();
            LivingEntity firstHolder = holder(helper, FIRST);
            ItemStack head = new ItemStack(Items.SKELETON_SKULL);
            SkullEyeState.stampItem(head, new Snapshot(true, 0F, AppearanceOverride.EMPTY.withGlow(true)));
            var eyes = cache.get(firstHolder, InteractionHand.MAIN_HAND, head);
            helper.assertTrue(eyes != null && eyes.helper != null, Component.literal("eyed heads resolve their configured layout"));
            for (int part : eyes.parts) {
                for (var pupil : eyes.eyes[part]) {
                    helper.assertTrue(pupil.itemGravity && !pupil.guiGravity, Component.literal("each head pupil uses world gravity"));
                }
            }
            BehaviorSettings defaults = TestBehaviorSettings.DEFAULTS;
            BehaviorSettings cross = new BehaviorSettings(true, 200, 200, List.of(EyeBehavior.CROSS_EYE),
                    0, false, defaults.swirlHealCooldownTicks(), defaults.ongoing());
            boolean played = false;
            for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
                cache.get(firstHolder, InteractionHand.MAIN_HAND, head);
                cache.tick(tick, true, cross);
                played |= eyes.active != null;
            }
            helper.assertTrue(played, Component.literal("a paired hand-held head can play cross-eye"));
            helper.succeed();
        } finally {
            ClientEyeConfigs.replaceAll(definitions);
            ClientEyeConfigs.applySettings(enabled, behaviors);
        }
    }

    private static LivingEntity holder(GameTestHelper helper, UUID uuid) {
        LivingEntity holder = Objects.requireNonNull(EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.COMMAND));
        holder.setUUID(uuid);
        return holder;
    }
}
