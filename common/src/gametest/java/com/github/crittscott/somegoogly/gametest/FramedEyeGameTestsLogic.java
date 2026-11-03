package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorInstance;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeInfluence;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState.Snapshot;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Drives the mounted eye simulation without a client renderer. Actual frame mounting, render-context
 * isolation, lighting, pause, and unloading require the in-game checks in docs/framed-eyes-testing.md.
 */
public final class FramedEyeGameTestsLogic {

    private static final UUID FIRST_FRAME = new UUID(1L, 2L);
    /** Observe acceleration before the pupil's first bounce from the rim. */
    private static final int GRAVITY_OBSERVATION_TICKS = 8;
    private static final int OBSERVATION_TICKS = 2048;
    private static final UUID SECOND_FRAME = new UUID(3L, 4L);
    private static final int SLOT_TICKS = 200;

    private FramedEyeGameTestsLogic() {
    }

    private static ItemStack eyedHead(Item item, Snapshot snapshot) {
        ItemStack stack = new ItemStack(item);
        SkullEyeState.stampItem(stack, snapshot);
        return stack;
    }

    /**
     * Empty slots, Slimy Eyes, and ordinary items are ineligible. In game: replace a framed Googly Eye
     * with a Slimy Eye, a diamond, and nothing; only the Googly Eye has a simulated pupil.
     */
    public static void framedEyeFiltersItems(GameTestHelper helper) {
        helper.assertTrue(PresentationEyes.supports(new ItemStack(ModContent.GOOGLY_EYE.get())), Component.literal("Googly Eye is eligible"));
        helper.assertTrue(!PresentationEyes.supports(new ItemStack(ModContent.SLIMY_EYE.get())), Component.literal("Slimy Eye is static"));
        helper.assertTrue(!PresentationEyes.supports(new ItemStack(Items.DIAMOND)), Component.literal("ordinary items are static"));
        helper.assertTrue(!PresentationEyes.supports(ItemStack.EMPTY), Component.literal("empty frames have no pupil simulation"));
        helper.succeed();
    }

    /**
     * Mounted pupils follow supplied gravity instead of always falling along the item's own down.
     * In game: right-click a wall-mounted eye to rotate it; its pupil still falls toward the ground.
     */
    public static void framedEyeFollowsMountedGravity(GameTestHelper helper) {
        PresentationEyes left = new PresentationEyes(FIRST_FRAME);
        PresentationEyes right = new PresentationEyes(FIRST_FRAME);
        left.eye.gravX = -1F;
        right.eye.gravX = 1F;
        left.eye.gravY = right.eye.gravY = 0F;
        for (int tick = 0; tick < GRAVITY_OBSERVATION_TICKS; tick++) {
            left.tick(tick, BehaviorSettings.NONE);
            right.tick(tick, BehaviorSettings.NONE);
        }
        helper.assertTrue(left.eye.deltaX < -0.5F && right.eye.deltaX > 0.5F, Component.literal("opposite mounted gravity directions send pupils to opposite rims"));
        helper.assertTrue(Math.abs(left.eye.deltaY) < 0.1F && Math.abs(right.eye.deltaY) < 0.1F, Component.literal("eye-local down does not pull against the supplied gravity"));
        helper.succeed();
    }

    /**
     * Short ambient intervals cannot overlap expressions; a time jump reconstructs the current slot.
     * In game: set ambient min/max to their minimum and pool to blink; framed eyes keep blinking cleanly.
     */
    public static void framedEyeHandlesShortIntervalsAndTimeJumps(GameTestHelper helper) {
        BehaviorSettings shortSlots = settings(true, 1, List.of(EyeBehavior.BLINK));
        PresentationEyes eye = new PresentationEyes(FIRST_FRAME);
        eye.tick(0L, shortSlots);
        BehaviorInstance first = eye.active;
        helper.assertTrue(first != null && first.age == 0, Component.literal("first short slot starts a blink"));
        int duration = EyeBehavior.BLINK.defaultDuration();
        eye.tick(duration - 1L, shortSlots);
        helper.assertTrue(eye.active == first && eye.active.age == duration - 1, Component.literal("the same expression lasts to its final tick"));
        eye.tick(duration, shortSlots);
        helper.assertTrue(eye.active != null && eye.active != first && eye.active.age == 0, Component.literal("the next expression starts only when the previous one finishes"));
        long later = duration * 100L + duration / 2;
        eye.tick(later, shortSlots);
        helper.assertTrue(eye.active != null && eye.active.age == duration / 2, Component.literal("returning after a long absence joins the current expression"));
        helper.succeed();
    }

    /**
     * Ambient-off, empty, and partner-only pools suppress expressions, including one already active
     * when settings change. In game: reload each setting and observe that the framed eye stops expressing.
     */
    public static void framedEyeHonorsAmbientSettings(GameTestHelper helper) {
        PresentationEyes eye = new PresentationEyes(FIRST_FRAME);
        eye.tick(0L, settings(true, 1, List.of(EyeBehavior.BLINK)));
        helper.assertTrue(eye.active != null, Component.literal("begin with an active expression"));
        List<BehaviorSettings> inactive = List.of(
                settings(false, SLOT_TICKS, List.of(EyeBehavior.BLINK)),
                settings(true, SLOT_TICKS, List.of()),
                settings(true, SLOT_TICKS, List.of(EyeBehavior.CROSS_EYE)));
        for (BehaviorSettings settings : inactive) {
            for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
                eye.tick(tick, settings);
                helper.assertTrue(eye.active == null, Component.literal("inactive settings never start or retain an expression"));
            }
        }
        helper.succeed();
    }

    /**
     * Removing cross-eye from a mixed pool still leaves lone-eye expressions available. In game: use
     * ambient pool [cross_eye, blink]; the framed eye winks but never tries to cross toward another eye.
     */
    public static void framedEyePlaysSingleEyeExpressions(GameTestHelper helper) {
        BehaviorSettings settings = settings(true, SLOT_TICKS, List.of(EyeBehavior.CROSS_EYE, EyeBehavior.BLINK));
        PresentationEyes eye = new PresentationEyes(FIRST_FRAME);
        boolean winked = false;
        for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
            eye.tick(tick, settings);
            if (eye.active != null) {
                helper.assertTrue(eye.active.behavior == EyeBehavior.BLINK && eye.active.mask[0][0], Component.literal("a lone eye participates in the blink and never crosses"));
                winked |= eye.eye.squashY < 0.5F;
            }
        }
        helper.assertTrue(winked, Component.literal("the frame visibly winks"));
        helper.succeed();
    }

    /**
     * A newly created tracker joins a running expression with identical age and seeded parameters.
     * In game: two players view one frame, with the second arriving mid-expression; both see its same phase.
     */
    public static void framedEyeReconstructsExpressions(GameTestHelper helper) {
        BehaviorSettings settings = settings(true, SLOT_TICKS,
                List.of(EyeBehavior.SIDE_EYE, EyeBehavior.COLOR_CHANGE, EyeBehavior.BLINK));
        PresentationEyes original = new PresentationEyes(FIRST_FRAME);
        boolean reconstructed = false;
        for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
            original.tick(tick, settings);
            if (original.active != null && original.active.age > 0) {
                PresentationEyes arriving = new PresentationEyes(FIRST_FRAME);
                arriving.tick(tick, settings);
                BehaviorInstance a = original.active;
                BehaviorInstance b = arriving.active;
                helper.assertTrue(b != null && a.behavior == b.behavior && a.age == b.age
                                && a.duration == b.duration && a.dirSign == b.dirSign
                                && Arrays.equals(a.tintColor, b.tintColor) && Arrays.deepEquals(a.mask, b.mask), Component.literal("late viewers reconstruct the same expression and seeded parameters"));
                reconstructed = true;
            }
        }
        helper.assertTrue(reconstructed, Component.literal("at least one mid-expression arrival was checked"));
        helper.succeed();
    }

    /**
     * Expressions finish, leaving neutral overlays between slots. In game: restrict ambient pool to
     * blink and watch the frame reopen fully before its next wink.
     */
    public static void framedEyeReleasesExpressionOverlays(GameTestHelper helper) {
        BehaviorSettings settings = settings(true, SLOT_TICKS, List.of(EyeBehavior.BLINK));
        PresentationEyes eye = new PresentationEyes(FIRST_FRAME);
        boolean played = false;
        boolean released = false;
        for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
            eye.tick(tick, settings);
            played |= eye.active != null;
            if (played && eye.active == null) {
                helper.assertTrue(eye.eye.squashY == 1F && eye.eye.scaleMul == 1F, Component.literal("completed expressions release their overlays"));
                released = true;
            }
        }
        helper.assertTrue(released, Component.literal("an expression finished with idle time afterward"));
        helper.succeed();
    }

    /**
     * Frames use independent UUID schedules. In game: fill several neighboring frames with copies of
     * one eye; they express independently even though their displayed items match.
     */
    public static void framedEyeUsesIndependentSchedules(GameTestHelper helper) {
        BehaviorSettings settings = settings(true, SLOT_TICKS, List.of(EyeBehavior.SIDE_EYE, EyeBehavior.BLINK));
        PresentationEyes first = new PresentationEyes(FIRST_FRAME);
        PresentationEyes second = new PresentationEyes(SECOND_FRAME);
        boolean differed = false;
        for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
            first.tick(tick, settings);
            second.tick(tick, settings);
            differed |= (first.active == null) != (second.active == null)
                    || first.active != null && second.active != null
                    && (first.active.behavior != second.active.behavior || first.active.age != second.active.age);
        }
        helper.assertTrue(differed, Component.literal("neighboring frames do not share one global animation"));
        helper.succeed();
    }

    /**
     * Two head pupils can cross; the same pool is suppressed for a lone loose eye. In game: set the
     * ambient pool to cross_eye and frame an eyed skull beside a Googly Eye; only the skull crosses.
     */
    public static void framedHeadAllowsPairedExpressions(GameTestHelper helper) {
        withHeadDefinitions(() -> {
            PresentationEyes frame = PresentationEyes.create(FIRST_FRAME,
                    eyedHead(Items.SKELETON_SKULL, new Snapshot(true, 0F, AppearanceOverride.EMPTY)));
            helper.assertTrue(frame != null, Component.literal("an eyed skull has a frame simulation"));
            BehaviorSettings settings = settings(true, SLOT_TICKS, List.of(EyeBehavior.CROSS_EYE));
            boolean crossed = false;
            boolean converged = false;
            for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
                frame.tick(tick, settings);
                if (frame.active != null) {
                    helper.assertTrue(frame.active.behavior == EyeBehavior.CROSS_EYE
                                    && frame.active.helper == frame.helper, Component.literal("paired expressions receive the head's full placement definition"));
                    crossed = true;
                    if (frame.active.age == frame.active.duration / 2) {
                        int part = frame.parts[0];
                        EyeInfluence left = new EyeInfluence();
                        EyeInfluence right = new EyeInfluence();
                        frame.active.behavior.influence(frame.active, part, 0, left);
                        frame.active.behavior.influence(frame.active, part, 1, right);
                        converged |= left.anchorX * right.anchorX < 0F
                                && left.stiffness > 0F && right.stiffness > 0F;
                    }
                }
            }
            helper.assertTrue(crossed && converged, Component.literal("the configured pupils converge toward their partners"));
            helper.succeed();
        });
    }

    /**
     * Species, variant, and appearance changes invalidate a head tracker; plain heads have no tracker.
     * In game: replace an eyed framed head with another species, another eye color/glow, then a plain
     * head and a loose eye. No former head layout or expression remains attached to the new contents.
     */
    public static void framedHeadInvalidatesChangedItems(GameTestHelper helper) {
        withHeadDefinitions(() -> {
            Snapshot snapshot = new Snapshot(true, 0F, AppearanceOverride.EMPTY);
            ItemStack stack = eyedHead(Items.SKELETON_SKULL, snapshot);
            PresentationEyes frame = PresentationEyes.create(FIRST_FRAME, stack);
            helper.assertTrue(frame != null && frame.matches(stack.copy()), Component.literal("equal head state keeps its tracker"));
            helper.assertTrue(!frame.matches(eyedHead(Items.CREEPER_HEAD, snapshot)), Component.literal("a different species replaces the layout"));
            helper.assertTrue(!frame.matches(eyedHead(Items.SKELETON_SKULL,
                    new Snapshot(true, 0.5F, AppearanceOverride.EMPTY))), Component.literal("a different variant replaces the layout"));
            helper.assertTrue(!frame.matches(eyedHead(Items.SKELETON_SKULL,
                    new Snapshot(true, 0F, AppearanceOverride.EMPTY.withGlow(true)))), Component.literal("appearance changes replace the head snapshot"));
            helper.assertTrue(!PresentationEyes.supports(new ItemStack(Items.SKELETON_SKULL))
                            && PresentationEyes.create(FIRST_FRAME, new ItemStack(Items.SKELETON_SKULL)) == null, Component.literal("a plain skull has no animated eyes"));
            helper.assertTrue(!frame.matches(new ItemStack(ModContent.GOOGLY_EYE.get()))
                            && !new PresentationEyes(FIRST_FRAME).matches(stack), Component.literal("head and loose-eye simulations cannot be reused for each other"));
            helper.succeed();
        });
    }

    /**
     * Newly arriving viewers reconstruct a head's expression and blink participation. In game: join
     * another player's framed-head display mid-blink; the same eyes blink at the same expression age.
     */
    public static void framedHeadReconstructsExpressions(GameTestHelper helper) {
        withHeadDefinitions(() -> {
            ItemStack stack = eyedHead(Items.SKELETON_SKULL,
                    new Snapshot(true, 0F, AppearanceOverride.EMPTY));
            PresentationEyes original = PresentationEyes.create(FIRST_FRAME, stack);
            helper.assertTrue(original != null, Component.literal("an eyed skull has a frame simulation"));
            BehaviorSettings settings = settings(true, SLOT_TICKS, List.of(EyeBehavior.BLINK));
            boolean blinked = false;
            boolean reconstructed = false;
            for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
                original.tick(tick, settings);
                for (int part : original.parts) {
                    for (var pupil : original.eyes[part]) {
                        blinked |= pupil.squashY < 0.5F;
                    }
                }
                if (original.active != null && original.active.age > 0) {
                    PresentationEyes arriving = PresentationEyes.create(FIRST_FRAME, stack);
                    arriving.tick(tick, settings);
                    helper.assertTrue(arriving.active != null && arriving.active.age == original.active.age
                                    && Arrays.deepEquals(arriving.active.mask, original.active.mask), Component.literal("head viewers reconstruct the same blink age and participation"));
                    reconstructed = true;
                }
            }
            helper.assertTrue(blinked && reconstructed, Component.literal("configured head pupils blink and support late arrival"));
            helper.succeed();
        });
    }

    /**
     * Eyed heads carry their mob's configured head parts and pupil arrays, including mounted gravity.
     * In game: frame an eyed skeleton skull and a supported Heads-mod head; their eyes retain their
     * normal positions, colors, and sizes while their pupils animate.
     */
    public static void framedHeadUsesConfiguredLayout(GameTestHelper helper) {
        withHeadDefinitions(() -> {
            Snapshot snapshot = new Snapshot(true, 0F, AppearanceOverride.EMPTY.withGlow(true));
            ItemStack stack = eyedHead(Items.SKELETON_SKULL, snapshot);
            PresentationEyes frame = PresentationEyes.create(FIRST_FRAME, stack);
            helper.assertTrue(frame != null && frame.helper != null && frame.helper.hasConfig(), Component.literal("a framed eyed skull resolves its mob's definition"));
            helper.assertTrue(snapshot.equals(frame.snapshot), Component.literal("the head keeps its stored appearance and roll"));
            int pupils = 0;
            for (int part : frame.parts) {
                helper.assertTrue(frame.eyes[part].length == frame.helper.getEyeCount(part), Component.literal("each visible part has its configured pupil count"));
                for (var pupil : frame.eyes[part]) {
                    helper.assertTrue(pupil.itemGravity, Component.literal("mounted pupils remove item display scale from gravity"));
                    pupils++;
                }
            }
            helper.assertTrue(pupils >= 2, Component.literal("the skeleton head supplies its paired eyes"));
            helper.succeed();
        });
    }

    private static BehaviorSettings settings(boolean ambient, int interval, List<EyeBehavior> pool) {
        return new BehaviorSettings(ambient, interval, interval, pool, 0, false, 0,
                BehaviorSettings.NONE.ongoing());
    }

    /** Installs synced head definitions only for the synchronous assertions, restoring the client store. */
    private static void withHeadDefinitions(Runnable test) {
        var definitions = ClientEyeConfigs.all();
        BehaviorSettings behaviors = ClientEyeConfigs.behaviors();
        boolean enabled = ClientEyeConfigs.googlyEyesEnabled();
        try {
            ClientEyeConfigs.replaceAll(ServerEyeConfigs.all(), true, BehaviorSettings.DEFAULTS);
            test.run();
        } finally {
            ClientEyeConfigs.replaceAll(definitions, enabled, behaviors);
        }
    }
}
