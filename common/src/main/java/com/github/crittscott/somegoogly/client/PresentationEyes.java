package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorInstance;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeInfluence;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState.Snapshot;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.registry.ModContent;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * One presentation's loose eye or configured eye layout, its ambient expressions, and, for an eye in a hand, the
 * server's reactions. Its identity and supplied clock determine expression age; physics is local and disposable.
 * The renderer supplies gravity.
 */
public final class PresentationEyes {

    @Nullable
    public BehaviorInstance active;
    @Nullable
    private BehaviorInstance ambientExpression;
    private long behaviorSeed;
    /** A server-owned hand reaction, which plays over the ambient expression until it ends. */
    @Nullable
    private BehaviorInstance reaction;
    private long reactionSeed;
    @Nullable
    public final Identifier entity;
    /** Whether the placed head uses fitted Heads-mod geometry. */
    public final boolean headsMod;
    /** Loose-item pupil; configured heads use the shaped arrays in eyes. */
    public final GooglyTracker.EyeInfo eye = new GooglyTracker.EyeInfo();
    public final GooglyTracker.EyeInfo[][] eyes;
    /**
     * The placed head's eye-state compound this was built from. A load replaces that compound, so another
     * reference means the head's state may have changed.
     */
    @Nullable
    CompoundTag headData;
    @Nullable
    public final HeadInfo helper;
    private final EyeInfluence influence = new EyeInfluence();
    /** A private copy of the presented item; empty for placed heads and boats. */
    private final ItemStack item;
    int lastRenderTick;
    private long motionTick = Long.MIN_VALUE;
    @Nullable
    private Entity owner;
    public final int[] parts;
    private double prevX, prevY, prevZ;
    private List<EyeBehavior> pool = List.of();
    /** The appearance this presentation draws, decoded once. */
    public final AppearanceOverride properties;
    private final RandomSource rand;
    private long scheduledSlot = Long.MIN_VALUE;
    private final long seed;
    @Nullable
    private BehaviorSettings settings;
    @Nullable
    private EyeBehavior slotBehavior;
    private int slotTicks;
    @Nullable
    public final Snapshot snapshot;
    private long startsAt;
    private final boolean stationaryHead;

    private PresentationEyes(UUID uuid, ItemStack looseEye) {
        this(BehaviorSettings.presentationSeed(uuid), looseEye.copyWithCount(1), null, null, null, new int[]{0}, false, false);
    }

    private PresentationEyes(long seed, ItemStack item, @Nullable Identifier entity, @Nullable Snapshot snapshot,
                             @Nullable HeadInfo helper, int[] parts, boolean stationaryHead, boolean headsMod) {
        this.seed = seed;
        this.item = item;
        this.properties = snapshot != null ? snapshot.properties() : EyeItemProperties.get(item);
        this.stationaryHead = stationaryHead;
        this.headsMod = headsMod;
        rand = RandomSource.create(seed);
        this.entity = entity;
        this.snapshot = snapshot;
        this.helper = helper;
        eye.prevDeltaY = eye.deltaY = 0F;
        eye.itemGravity = true;
        this.parts = parts.clone();
        if (helper == null) {
            eyes = new GooglyTracker.EyeInfo[][]{{eye}};
        } else {
            eyes = new GooglyTracker.EyeInfo[helper.getHeadCount()][];
            for (int head = 0; head < eyes.length; head++) {
                eyes[head] = new GooglyTracker.EyeInfo[helper.getEyeCount(head)];
                for (int index = 0; index < eyes[head].length; index++) {
                    GooglyTracker.EyeInfo pupil = new GooglyTracker.EyeInfo();
                    if (!stationaryHead) {
                        pupil.prevDeltaY = pupil.deltaY = 0F;
                        pupil.itemGravity = true;
                    }
                    eyes[head][index] = pupil;
                }
            }
        }
    }

    /** The animation a draw uses and that draw's partial tick; {@link #NONE} outside animated draws. */
    public record Draw(@Nullable PresentationEyes eyes, float partialTick) {
        public static final Draw NONE = new Draw(null, 0F);

        /** {@code eyes} at {@code partialTick}, or {@link #NONE} when there are none. */
        public static Draw of(@Nullable PresentationEyes eyes, float partialTick) {
            return eyes == null ? NONE : new Draw(eyes, partialTick);
        }
    }

    /** Implemented on render states, so an animation chosen at extraction reaches the later draw. */
    public interface Holder {
        Draw somegoogly$draw();

        void somegoogly$setDraw(Draw draw);
    }

    /** Build the item's current layout; plain heads and unsupported definitions have no simulation. */
    @Nullable
    public static PresentationEyes create(UUID uuid, ItemStack stack) {
        if (stack.is(ModContent.GOOGLY_EYE.get())) {
            return new PresentationEyes(uuid, stack);
        }
        Identifier entity = SkullEyeState.entityFor(stack);
        Snapshot snapshot = SkullEyeState.itemSnapshot(stack);
        if (entity == null || snapshot == null) {
            return null;
        }
        HeadInfo helper = ClientEyeConfigs.resolveHead(entity, snapshot.variantRoll());
        int[] parts = SkullEyeState.headParts(entity, helper);
        if (parts.length == 0) {
            return null;
        }
        return new PresentationEyes(BehaviorSettings.presentationSeed(uuid), stack.copyWithCount(1), entity, snapshot, helper, parts, false, false);
    }

    /** Build a validated, enabled layout without item or skull eligibility assumptions. */
    public static PresentationEyes create(UUID uuid, Identifier entity, Snapshot snapshot, HeadInfo helper) {
        return new PresentationEyes(BehaviorSettings.presentationSeed(uuid), ItemStack.EMPTY, entity, snapshot, helper,
                IntStream.range(0, helper.getHeadCount()).toArray(), false, false);
    }

    /** Use the block position's seed and the attached-eye gravity convention for a placed head. */
    public static PresentationEyes placedHead(long seed, Identifier entity, Snapshot snapshot,
                                              HeadInfo helper, int[] parts, boolean headsMod) {
        return new PresentationEyes(seed, ItemStack.EMPTY, entity, snapshot, helper, parts, true, headsMod);
    }

    /** Stamp this presentation as rendered during the current client tick. */
    public void markRendered(int currentClientTick) {
        lastRenderTick = currentClientTick;
    }

    /** Whether {@code stack} is still the presented item; a different count or stack copy still is. */
    public boolean matches(ItemStack stack) {
        return ItemStack.isSameItemSameComponents(item, stack);
    }

    /** Whether this item carries a loose eye or an eligible head-eye snapshot. */
    public static boolean supports(ItemStack stack) {
        return stack.is(ModContent.GOOGLY_EYE.get())
                || SkullEyeState.itemHasEyes(stack) && SkullEyeState.entityFor(stack) != null;
    }

    /**
     * Step once on the presentation clock. Absolute expression age catches up after time offscreen;
     * slots are at least the longest expression, so even very short configured intervals cannot overlap.
     */
    public void tick(long gameTime, BehaviorSettings behaviors) {
        tick(gameTime, behaviors, null);
    }

    /** Step with the world's actual owner motion; render poses supply gravity and the eye-plane basis. */
    public void tick(long gameTime, BehaviorSettings behaviors, @Nullable Entity owner) {
        boolean resetMotion = this.owner != owner || motionTick == Long.MIN_VALUE || gameTime != motionTick + 1;
        this.owner = owner;
        motionTick = gameTime;
        double mx = owner == null || resetMotion ? 0D : owner.getX() - prevX;
        double my = owner == null || resetMotion ? 0D : owner.getY() - prevY;
        double mz = owner == null || resetMotion ? 0D : owner.getZ() - prevZ;
        float yaw = owner instanceof LivingEntity living ? living.getYHeadRot() : owner == null ? 0F : owner.getYRot();
        float pitch = owner == null ? 0F : owner.getXRot();
        if (owner != null) {
            prevX = owner.getX();
            prevY = owner.getY();
            prevZ = owner.getZ();
        }
        active = ambientExpression;
        if (settings != behaviors) {
            settings = behaviors;
            boolean paired = false;
            if (helper != null) {
                for (int part : parts) {
                    for (int index = 0; index < eyes[part].length; index++) {
                        int target = helper.placementAt(part, index).crossTarget();
                        paired |= target >= 0 && target < eyes[part].length && target != index;
                    }
                }
            }
            boolean hasPartner = paired;
            pool = behaviors.ambientPool(hasPartner);
            slotTicks = behaviors.slotTicks(pool);
            scheduledSlot = Long.MIN_VALUE;
            active = null;
        }
        if (!behaviors.ambient() || pool.isEmpty()) {
            active = null;
        } else {
            long slot = Math.floorDiv(gameTime, slotTicks);
            if (slot != scheduledSlot) {
                scheduledSlot = slot;
                BehaviorSettings.AmbientSlot selected = behaviors.ambientSlot(seed, slot, slotTicks, pool);
                slotBehavior = selected.behavior();
                startsAt = selected.startsAt();
                behaviorSeed = selected.seed();
                active = null;
            }
            long elapsed = gameTime - startsAt;
            if (elapsed >= 0 && elapsed < slotBehavior.defaultDuration()) {
                if (active == null) {
                    active = BehaviorInstance.start(slotBehavior, helper, slotBehavior.defaultDuration(), behaviorSeed);
                }
                active.age = (int) elapsed;
            } else {
                active = null;
            }
        }

        ambientExpression = active;
        if (reaction != null) {
            active = reaction;
            if (reaction.advance()) {
                reaction = null;
            }
        }

        for (int part : parts) {
            for (int index = 0; index < eyes[part].length; index++) {
                GooglyTracker.EyeInfo pupil = eyes[part][index];
                if (resetMotion && !stationaryHead) {
                    pupil.resetMotion();
                }
                pupil.step(influence, active, part, index, rand, yaw, pitch, mx, my, mz);
            }
        }
    }

    /** Install or catch up a server-owned hand reaction before its physics tick. */
    public void react(EyeBehavior behavior, long seed, int elapsed) {
        if (reaction == null || reactionSeed != seed || reaction.behavior != behavior) {
            reaction = BehaviorInstance.start(behavior, helper, behavior.defaultDuration(), seed);
            reactionSeed = seed;
        }
        reaction.age = elapsed;
    }

    /** Use screen-down rather than world-down when the GUI renderer captures pupil orientation. */
    public void useGuiGravity() {
        for (int part : parts) {
            for (GooglyTracker.EyeInfo pupil : eyes[part]) {
                pupil.guiGravity = true;
                pupil.itemGravity = false;
            }
        }
    }
}
