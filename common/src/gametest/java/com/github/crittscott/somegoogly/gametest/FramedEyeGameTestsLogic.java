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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
     * Only a Googly Eye in a frame gets a moving pupil; a Slimy Eye, an ordinary item, and an empty frame get none. In
     * game: frame a Googly Eye, then replace it with a Slimy Eye, then a diamond, then nothing; only the Googly Eye's
     * pupil moves.
     */
    public static void framedEyeFiltersItems(GameTestHelper helper) {
        helper.assertTrue(PresentationEyes.supports(new ItemStack(ModContent.GOOGLY_EYE.get())), "Googly Eye is eligible");
        helper.assertTrue(!PresentationEyes.supports(new ItemStack(ModContent.SLIMY_EYE.get())), "Slimy Eye is static");
        helper.assertTrue(!PresentationEyes.supports(new ItemStack(Items.DIAMOND)), "ordinary items are static");
        helper.assertTrue(!PresentationEyes.supports(ItemStack.EMPTY), "empty frames have no pupil simulation");
        helper.succeed();
    }

    /**
     * A framed eye's pupil falls toward the ground however the frame or the item in it is turned. In game: frame a
     * Googly Eye on a wall and right-click it to rotate it through each position; its pupil always settles at the
     * bottom.
     */
    public static void framedEyeFollowsMountedGravity(GameTestHelper helper) {
        PresentationEyes left = PresentationEyes.create(FIRST_FRAME, new ItemStack(ModContent.GOOGLY_EYE.get()));
        PresentationEyes right = PresentationEyes.create(FIRST_FRAME, new ItemStack(ModContent.GOOGLY_EYE.get()));
        left.eye.gravX = -1F;
        right.eye.gravX = 1F;
        left.eye.gravY = right.eye.gravY = 0F;
        for (int tick = 0; tick < GRAVITY_OBSERVATION_TICKS; tick++) {
            left.tick(tick, BehaviorSettings.NONE);
            right.tick(tick, BehaviorSettings.NONE);
        }
        helper.assertTrue(left.eye.deltaX < -0.5F && right.eye.deltaX > 0.5F,
                "opposite mounted gravity directions send pupils to opposite rims");
        helper.assertTrue(Math.abs(left.eye.deltaY) < 0.1F && Math.abs(right.eye.deltaY) < 0.1F,
                "eye-local down does not pull against the supplied gravity");
        helper.succeed();
    }

    /**
     * Expressions never overlap, even with the shortest ambient interval, and an eye seen again after a long absence
     * joins the expression now playing. In game: set {@code ambientMinTicks = 1} and {@code ambientMaxTicks = 1} and
     * {@code ambientBehaviorPool = ["somegoogly:blink"]} in {@code somegoogly-server.toml}, then {@code /reload}; a
     * framed Googly Eye blinks back to back, each blink finishing before the next starts.
     */
    public static void framedEyeHandlesShortIntervalsAndTimeJumps(GameTestHelper helper) {
        BehaviorSettings shortSlots = settings(true, 1, List.of(EyeBehavior.BLINK));
        PresentationEyes eye = PresentationEyes.create(FIRST_FRAME, new ItemStack(ModContent.GOOGLY_EYE.get()));
        eye.tick(0L, shortSlots);
        BehaviorInstance first = eye.active;
        helper.assertTrue(first != null && first.age == 0, "first short slot starts a blink");
        int duration = EyeBehavior.BLINK.defaultDuration();
        eye.tick(duration - 1L, shortSlots);
        helper.assertTrue(eye.active == first && eye.active.age == duration - 1,
                "the same expression lasts to its final tick");
        eye.tick(duration, shortSlots);
        helper.assertTrue(eye.active != null && eye.active != first && eye.active.age == 0,
                "the next expression starts only when the previous one finishes");
        long later = duration * 100L + duration / 2;
        eye.tick(later, shortSlots);
        helper.assertTrue(eye.active != null && eye.active.age == duration / 2,
                "returning after a long absence joins the current expression");
        helper.succeed();
    }

    /**
     * A framed eye stops expressing, even mid-expression, when ambient expressions are off, the pool is empty, or the
     * pool holds only expressions that need a second eye. In game: with {@code ambientMinTicks = 1} and {@code
     * ambientMaxTicks = 1} and {@code ambientBehaviorPool = ["somegoogly:blink"]} in {@code somegoogly-server.toml},
     * watch a framed Googly Eye blink. Then set each in turn and {@code /reload}: {@code ambientBehaviors = false};
     * {@code ambientBehaviorPool = []}; {@code ambientBehaviorPool = ["somegoogly:cross_eye"]}. Each time the eye stops
     * blinking and plays nothing.
     */
    public static void framedEyeHonorsAmbientSettings(GameTestHelper helper) {
        PresentationEyes eye = PresentationEyes.create(FIRST_FRAME, new ItemStack(ModContent.GOOGLY_EYE.get()));
        eye.tick(0L, settings(true, 1, List.of(EyeBehavior.BLINK)));
        helper.assertTrue(eye.active != null, "begin with an active expression");
        List<BehaviorSettings> inactive = List.of(
                settings(false, SLOT_TICKS, List.of(EyeBehavior.BLINK)),
                settings(true, SLOT_TICKS, List.of()),
                settings(true, SLOT_TICKS, List.of(EyeBehavior.CROSS_EYE)));
        for (BehaviorSettings settings : inactive) {
            for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
                eye.tick(tick, settings);
                helper.assertTrue(eye.active == null, "inactive settings never start or retain an expression");
            }
        }
        helper.succeed();
    }

    /**
     * A lone eye skips expressions that need a partner and still plays the rest. In game: set {@code
     * ambientBehaviorPool = ["somegoogly:cross_eye", "somegoogly:blink"]} in {@code somegoogly-server.toml} and {@code
     * /reload}; a framed Googly Eye winks from time to time and never tries to cross.
     */
    public static void framedEyePlaysSingleEyeExpressions(GameTestHelper helper) {
        BehaviorSettings settings = settings(true, SLOT_TICKS, List.of(EyeBehavior.CROSS_EYE, EyeBehavior.BLINK));
        PresentationEyes eye = PresentationEyes.create(FIRST_FRAME, new ItemStack(ModContent.GOOGLY_EYE.get()));
        boolean winked = false;
        for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
            eye.tick(tick, settings);
            if (eye.active != null) {
                helper.assertTrue(eye.active.behavior == EyeBehavior.BLINK && eye.active.mask[0][0],
                        "a lone eye participates in the blink and never crosses");
                winked |= eye.eye.squashY < 0.5F;
            }
        }
        helper.assertTrue(winked, "the frame visibly winks");
        helper.succeed();
    }

    /**
     * A player who starts watching a frame mid-expression sees the same expression at the same point as everyone else.
     * In game: with two players, have one watch a framed Googly Eye while the other approaches from out of render
     * range; when it comes into view, both see the same expression at the same moment.
     */
    public static void framedEyeReconstructsExpressions(GameTestHelper helper) {
        BehaviorSettings settings = settings(true, SLOT_TICKS,
                List.of(EyeBehavior.SIDE_EYE, EyeBehavior.COLOR_CHANGE, EyeBehavior.BLINK));
        PresentationEyes original = PresentationEyes.create(FIRST_FRAME, new ItemStack(ModContent.GOOGLY_EYE.get()));
        boolean reconstructed = false;
        for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
            original.tick(tick, settings);
            if (original.active != null && original.active.age > 0) {
                PresentationEyes arriving = PresentationEyes.create(FIRST_FRAME, new ItemStack(ModContent.GOOGLY_EYE.get()));
                arriving.tick(tick, settings);
                BehaviorInstance a = original.active;
                BehaviorInstance b = arriving.active;
                helper.assertTrue(b != null && a.behavior == b.behavior && a.age == b.age
                                && a.duration == b.duration && a.dirSign == b.dirSign
                                && Arrays.equals(a.tintColor, b.tintColor) && Arrays.deepEquals(a.mask, b.mask),
                        "late viewers reconstruct the same expression and seeded parameters");
                reconstructed = true;
            }
        }
        helper.assertTrue(reconstructed, "at least one mid-expression arrival was checked");
        helper.succeed();
    }

    /**
     * Each expression ends fully, leaving the eye as it was. In game: set {@code ambientBehaviorPool =
     * ["somegoogly:blink"]} in {@code somegoogly-server.toml} and {@code /reload}; between blinks a framed Googly Eye
     * opens fully to its normal size.
     */
    public static void framedEyeReleasesExpressionOverlays(GameTestHelper helper) {
        BehaviorSettings settings = settings(true, SLOT_TICKS, List.of(EyeBehavior.BLINK));
        PresentationEyes eye = PresentationEyes.create(FIRST_FRAME, new ItemStack(ModContent.GOOGLY_EYE.get()));
        boolean played = false;
        boolean released = false;
        for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
            eye.tick(tick, settings);
            played |= eye.active != null;
            if (played && eye.active == null) {
                helper.assertTrue(eye.eye.squashY == 1F && eye.eye.scaleMul == 1F,
                        "completed expressions release their overlays");
                released = true;
            }
        }
        helper.assertTrue(released, "an expression finished with idle time afterward");
        helper.succeed();
    }

    /**
     * Each frame keeps its own expression schedule, even when frames hold identical eyes. In game: fill several
     * neighboring frames with Googly Eyes from one stack; they blink and glance at different times.
     */
    public static void framedEyeUsesIndependentSchedules(GameTestHelper helper) {
        BehaviorSettings settings = settings(true, SLOT_TICKS, List.of(EyeBehavior.SIDE_EYE, EyeBehavior.BLINK));
        PresentationEyes first = PresentationEyes.create(FIRST_FRAME, new ItemStack(ModContent.GOOGLY_EYE.get()));
        PresentationEyes second = PresentationEyes.create(SECOND_FRAME, new ItemStack(ModContent.GOOGLY_EYE.get()));
        boolean differed = false;
        for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
            first.tick(tick, settings);
            second.tick(tick, settings);
            differed |= (first.active == null) != (second.active == null)
                    || first.active != null && second.active != null
                    && (first.active.behavior != second.active.behavior || first.active.age != second.active.age);
        }
        helper.assertTrue(differed, "neighboring frames do not share one global animation");
        helper.succeed();
    }

    /**
     * A framed eyed head's two eyes can cross toward each other; a lone Googly Eye under the same settings cannot. In
     * game: set {@code ambientBehaviorPool = ["somegoogly:cross_eye"]} and {@code ambientMinTicks = 1} and {@code
     * ambientMaxTicks = 1} in {@code somegoogly-server.toml} and {@code /reload}, then frame an eyed skeleton skull
     * beside a Googly Eye; the skull's pupils cross and the lone eye stays still.
     */
    public static void framedHeadAllowsPairedExpressions(GameTestHelper helper) {
        withHeadDefinitions(() -> {
            PresentationEyes frame = PresentationEyes.create(FIRST_FRAME,
                    eyedHead(Items.SKELETON_SKULL, new Snapshot(true, 0F, AppearanceOverride.EMPTY)));
            helper.assertTrue(frame != null, "an eyed skull has a frame simulation");
            BehaviorSettings settings = settings(true, SLOT_TICKS, List.of(EyeBehavior.CROSS_EYE));
            boolean crossed = false;
            boolean converged = false;
            for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
                frame.tick(tick, settings);
                if (frame.active != null) {
                    helper.assertTrue(frame.active.behavior == EyeBehavior.CROSS_EYE
                                    && frame.active.helper == frame.helper,
                            "paired expressions receive the head's full placement definition");
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
            helper.assertTrue(crossed && converged, "the configured pupils converge toward their partners");
            helper.succeed();
        });
    }

    /**
     * A framed head's eyes follow what the frame now holds: a different species, eye arrangement, or appearance gets a
     * fresh layout, and a plain head or a loose eye shows nothing of the head before it. In game: frame an eyed
     * skeleton skull, then swap in an eyed creeper head, an eyed skeleton skull with glowing eyes, a plain skull, and a
     * Googly Eye; each shows only its own eyes, if any.
     */
    public static void framedHeadInvalidatesChangedItems(GameTestHelper helper) {
        withHeadDefinitions(() -> {
            Snapshot snapshot = new Snapshot(true, 0F, AppearanceOverride.EMPTY);
            ItemStack stack = eyedHead(Items.SKELETON_SKULL, snapshot);
            PresentationEyes frame = PresentationEyes.create(FIRST_FRAME, stack);
            helper.assertTrue(frame != null && frame.matches(stack.copy()), "equal head state keeps its tracker");
            helper.assertTrue(!frame.matches(eyedHead(Items.CREEPER_HEAD, snapshot)),
                    "a different species replaces the layout");
            helper.assertTrue(!frame.matches(eyedHead(Items.SKELETON_SKULL,
                    new Snapshot(true, 0.5F, AppearanceOverride.EMPTY))), "a different variant replaces the layout");
            helper.assertTrue(!frame.matches(eyedHead(Items.SKELETON_SKULL,
                    new Snapshot(true, 0F, AppearanceOverride.EMPTY.withGlow(true)))),
                    "appearance changes replace the head snapshot");
            helper.assertTrue(!PresentationEyes.supports(new ItemStack(Items.SKELETON_SKULL))
                            && PresentationEyes.create(FIRST_FRAME, new ItemStack(Items.SKELETON_SKULL)) == null,
                    "a plain skull has no animated eyes");
            helper.assertTrue(!frame.matches(new ItemStack(ModContent.GOOGLY_EYE.get()))
                            && !PresentationEyes.create(FIRST_FRAME, new ItemStack(ModContent.GOOGLY_EYE.get())).matches(stack),
                    "head and loose-eye simulations cannot be reused for each other");
            helper.succeed();
        });
    }

    /**
     * A player who starts watching a framed head mid-blink sees the same eyes blinking at the same point as everyone
     * else. In game: with two players and {@code ambientBehaviorPool = ["somegoogly:blink"]} in {@code
     * somegoogly-server.toml}, have one watch a framed eyed skeleton skull while the other approaches from out of
     * render range; both see each blink together.
     */
    public static void framedHeadReconstructsExpressions(GameTestHelper helper) {
        withHeadDefinitions(() -> {
            ItemStack stack = eyedHead(Items.SKELETON_SKULL,
                    new Snapshot(true, 0F, AppearanceOverride.EMPTY));
            PresentationEyes original = PresentationEyes.create(FIRST_FRAME, stack);
            helper.assertTrue(original != null, "an eyed skull has a frame simulation");
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
                                    && Arrays.deepEquals(arriving.active.mask, original.active.mask),
                            "head viewers reconstruct the same blink age and participation");
                    reconstructed = true;
                }
            }
            helper.assertTrue(blinked && reconstructed, "configured head pupils blink and support late arrival");
            helper.succeed();
        });
    }

    /**
     * A framed eyed head shows its mob's eyes where they sit on the placed head, with its stored appearance, and its
     * pupils fall toward the ground. In game: frame an eyed skeleton skull, and with the Heads mod installed an eyed
     * Heads-mod head; each shows its eyes in the same places, colors, and sizes as when placed, and the pupils move.
     */
    public static void framedHeadUsesConfiguredLayout(GameTestHelper helper) {
        withHeadDefinitions(() -> {
            Snapshot snapshot = new Snapshot(true, 0F, AppearanceOverride.EMPTY.withGlow(true));
            ItemStack stack = eyedHead(Items.SKELETON_SKULL, snapshot);
            PresentationEyes frame = PresentationEyes.create(FIRST_FRAME, stack);
            helper.assertTrue(frame != null && frame.helper != null && frame.helper.hasConfig(),
                    "a framed eyed skull resolves its mob's definition");
            helper.assertTrue(snapshot.equals(frame.snapshot), "the head keeps its stored appearance and roll");
            int pupils = 0;
            for (int part : frame.parts) {
                helper.assertTrue(frame.eyes[part].length == frame.helper.getEyeCount(part),
                        "each visible part has its configured pupil count");
                for (var pupil : frame.eyes[part]) {
                    helper.assertTrue(pupil.itemGravity, "mounted pupils remove item display scale from gravity");
                    pupils++;
                }
            }
            helper.assertTrue(pupils >= 2, "the skeleton head supplies its paired eyes");
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
            ClientEyeConfigs.replaceAll(ServerEyeConfigs.all());
            ClientEyeConfigs.applySettings(true, TestBehaviorSettings.DEFAULTS);
            test.run();
        } finally {
            ClientEyeConfigs.replaceAll(definitions);
            ClientEyeConfigs.applySettings(enabled, behaviors);
        }
    }
}
