package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.behavior.AmbientSettings;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorInstance;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeInfluence;
import com.github.crittscott.somegoogly.eye.state.EyeState.Snapshot;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;

/**
 * The live eyes of one placed mob head: a {@link GooglyTracker.EyeInfo} pupil per configured eye, stepped with
 * the same physics as mob eyes for a head that never moves, plus the expression playing on them. Only the
 * eyes of {@link #parts} are stepped and drawn.
 *
 * <p>A head has no triggers of its own, so it plays only ambient expressions, and every client derives them
 * from what all clients share: the head's position and the level's game time. Time is cut into slots of the
 * server's mean ambient interval; a random source seeded from the position and the slot picks the slot's
 * expression and its start, so every player sees the same expression at about the same tick with nothing
 * sent. A tracker created mid-expression joins it where it already is. The ongoing mob states (squints, lids,
 * anger, pulse, rattle) don't apply.
 */
public final class SkullEyes {

    /** Spreads consecutive slots' seeds apart (the 64-bit golden-ratio constant). */
    private static final long SLOT_SEED_STEP = 0x9E3779B97F4A7C15L;

    /** The behavior playing, or {@code null}. */
    @Nullable
    public BehaviorInstance active;
    /** The mob whose definition the head uses. */
    public final ResourceLocation entity;
    /** Per head and eye of {@link #helper}; the heads outside {@link #parts} sit unused. */
    public final GooglyTracker.EyeInfo[][] eyes;
    /** The head's placement view: its mob's adult definition, at the head's variant roll. */
    public final HeadInfo helper;
    // Reused each tick; head eyes are stepped only on the client tick thread.
    private final EyeInfluence influence = new EyeInfluence();
    /** Last {@link ClientEyeRuntime#clientTicks()} value at which these eyes were rendered. */
    int lastRenderTick;
    /** The {@link #helper} heads on the model's head part, the ones drawn. */
    public final int[] parts;
    private final RandomSource rand;
    private final long seed;
    /** The eye state this tracker was built from; a change replaces the tracker. */
    public final Snapshot snapshot;

    private SkullEyes(BlockPos pos, ResourceLocation entity, Snapshot snapshot, HeadInfo helper, int[] parts) {
        this.entity = entity;
        this.snapshot = snapshot;
        this.helper = helper;
        this.parts = parts;
        this.seed = Mth.getSeed(pos);
        this.rand = RandomSource.create(seed);
        this.eyes = new GooglyTracker.EyeInfo[helper.getHeadCount()][];
        for (int head = 0; head < eyes.length; head++) {
            eyes[head] = new GooglyTracker.EyeInfo[helper.getEyeCount(head)];
            for (int eye = 0; eye < eyes[head].length; eye++) {
                eyes[head][eye] = new GooglyTracker.EyeInfo();
            }
        }
    }

    /**
     * The eyes for a head at {@code pos} of {@code entity}'s kind in state {@code snapshot}, or {@code null} when
     * the client's definitions give that head nothing to draw.
     */
    @Nullable
    static SkullEyes create(BlockPos pos, ResourceLocation entity, Snapshot snapshot) {
        HeadInfo helper = ClientEyeConfigs.resolveHead(entity, snapshot.variantRoll());
        if (!helper.hasConfig()) {
            return null;
        }
        int[] parts = SkullEyeState.headParts(entity, helper);
        return parts.length == 0 ? null : new SkullEyes(pos.immutable(), entity, snapshot, helper, parts);
    }

    /** Stamp these eyes as rendered during {@code currentClientTick}. */
    public void markRendered(int currentClientTick) {
        lastRenderTick = currentClientTick;
    }

    /** Start the expression of the slot holding {@code gameTime}, if that is when it plays, caught up to it. */
    private void startAmbient(long gameTime, AmbientSettings ambient) {
        if (!ambient.plays()) {
            return;
        }
        int slotTicks = ambient.meanTicks();
        long slot = Math.floorDiv(gameTime, slotTicks);
        RandomSource slotRand = RandomSource.create(seed ^ slot * SLOT_SEED_STEP);
        EyeBehavior behavior = ambient.pool().get(slotRand.nextInt(ambient.pool().size()));
        int duration = behavior.defaultDuration();
        long start = slot * slotTicks + slotRand.nextInt(Math.max(1, slotTicks - duration));
        long elapsed = gameTime - start;
        if (elapsed < 0 || elapsed >= duration) {
            return;
        }
        BehaviorInstance instance = new BehaviorInstance(behavior, helper, duration, slotRand.nextLong());
        behavior.onStart(instance);
        instance.age = (int) elapsed;
        active = instance;
    }

    /**
     * Advance one client tick at level {@code gameTime}: retire a finished expression, start the slot's when it is
     * due, and step every drawn eye with the expression folded in.
     */
    void tick(long gameTime, AmbientSettings ambient) {
        if (active != null) {
            active.age++;
            if (active.age >= active.duration) {
                active = null;
            }
        }
        if (active == null) {
            startAmbient(gameTime, ambient);
        }
        for (int head : parts) {
            for (int eye = 0; eye < eyes[head].length; eye++) {
                influence.reset();
                if (active != null) {
                    active.behavior.influence(active, head, eye, influence);
                }
                GooglyTracker.EyeInfo info = eyes[head][eye];
                info.applyOverlays(influence, 1F, 1F, 0F, 0F);
                info.update(rand, 0F, 0F, 0D, 0D, 0D,
                        influence.anchorX, influence.anchorY, influence.stiffness, 0F);
            }
        }
    }
}
