package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorInstance;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeInfluence;
import com.github.crittscott.somegoogly.eye.state.EyeState.Snapshot;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.registry.ModContent;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * One presentation's loose eye or configured eye layout and ambient expressions. Its identity and
 * supplied clock determine expression age; physics is local and disposable. The renderer supplies gravity.
 */
public final class PresentationEyes {

    /** Spreads consecutive ambient slots' seeds apart. */
    private static final long SLOT_SEED_STEP = 0x9E3779B97F4A7C15L;

    @Nullable
    public BehaviorInstance active;
    private long behaviorSeed;
    @Nullable
    public final ResourceLocation entity;
    /** Loose-item pupil; configured heads use the shaped arrays in eyes. */
    public final GooglyTracker.EyeInfo eye = new GooglyTracker.EyeInfo();
    public final GooglyTracker.EyeInfo[][] eyes;
    @Nullable
    public final HeadInfo helper;
    private final EyeInfluence influence = new EyeInfluence();
    int lastRenderTick;
    private long motionTick = Long.MIN_VALUE;
    @Nullable
    private Entity owner;
    public final int[] parts;
    private double prevX, prevY, prevZ;
    private List<EyeBehavior> pool = List.of();
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

    public PresentationEyes(UUID uuid) {
        this(uuidSeed(uuid), null, null, null, new int[]{0}, false);
    }

    private PresentationEyes(long seed, @Nullable ResourceLocation entity, @Nullable Snapshot snapshot,
                             @Nullable HeadInfo helper, int[] parts, boolean stationaryHead) {
        this.seed = seed;
        this.stationaryHead = stationaryHead;
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

    /** Animation selected during frame-state extraction, carried to its item draw. */
    public record FrameData(@Nullable PresentationEyes eye, float partialTick) {
    }

    /** Implemented by the frame render-state mixin. */
    public interface Holder {
        @Nullable
        FrameData somegoogly$frameEye();

        void somegoogly$setFrameEye(@Nullable FrameData data);
    }

    /** Build the item's current layout; plain heads and unsupported definitions have no simulation. */
    @Nullable
    public static PresentationEyes create(UUID uuid, ItemStack stack) {
        if (stack.is(ModContent.GOOGLY_EYE.get())) {
            return new PresentationEyes(uuid);
        }
        ResourceLocation entity = SkullEyeState.entityFor(stack);
        Snapshot snapshot = SkullEyeState.itemSnapshot(stack);
        if (entity == null || snapshot == null) {
            return null;
        }
        HeadInfo helper = ClientEyeConfigs.resolveHead(entity, snapshot.variantRoll());
        int[] parts = SkullEyeState.headParts(helper);
        if (parts.length == 0) {
            return null;
        }
        return new PresentationEyes(uuidSeed(uuid), entity, snapshot, helper, parts, false);
    }

    /** Build a validated, enabled layout without item or skull eligibility assumptions. */
    public static PresentationEyes create(UUID uuid, ResourceLocation entity, Snapshot snapshot, HeadInfo helper) {
        if (!helper.hasConfig()) {
            throw new IllegalArgumentException("An ambient layout must contain eyes");
        }
        return new PresentationEyes(uuidSeed(uuid), entity, snapshot, helper,
                IntStream.range(0, helper.getHeadCount()).toArray(), false);
    }

    /** Use the block position's seed and the attached-eye gravity convention for a placed head. */
    public static PresentationEyes placedHead(long seed, ResourceLocation entity, Snapshot snapshot,
                                              HeadInfo helper, int[] parts) {
        return new PresentationEyes(seed, entity, snapshot, helper, parts, true);
    }

    private static long uuidSeed(UUID uuid) {
        return uuid.getMostSignificantBits() ^ Long.rotateLeft(uuid.getLeastSignificantBits(), 1);
    }

    /** Stamp this presentation as rendered during the current client tick. */
    public void markRendered(int currentClientTick) {
        lastRenderTick = currentClientTick;
    }

    /** Item changes that affect layout or appearance replace a head simulation. */
    public boolean matches(ItemStack stack) {
        return helper == null ? stack.is(ModContent.GOOGLY_EYE.get())
                : entity.equals(SkullEyeState.entityFor(stack)) && snapshot.equals(SkullEyeState.itemSnapshot(stack));
    }

    /** Whether this item carries a loose eye or an eligible head-eye snapshot. */
    public static boolean supports(ItemStack stack) {
        return stack.is(ModContent.GOOGLY_EYE.get())
                || SkullEyeState.entityFor(stack) != null && SkullEyeState.itemSnapshot(stack) != null;
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
            pool = behaviors.pool().stream().filter(b -> hasPartner || b != EyeBehavior.CROSS_EYE).toList();
            slotTicks = Math.max(behaviors.meanTicks(), pool.stream()
                    .mapToInt(EyeBehavior::defaultDuration).max().orElse(1));
            scheduledSlot = Long.MIN_VALUE;
            active = null;
        }
        if (!behaviors.ambient() || pool.isEmpty()) {
            active = null;
        } else {
            long slot = Math.floorDiv(gameTime, slotTicks);
            if (slot != scheduledSlot) {
                scheduledSlot = slot;
                RandomSource slotRand = RandomSource.create(seed ^ slot * SLOT_SEED_STEP);
                slotBehavior = pool.get(slotRand.nextInt(pool.size()));
                startsAt = slot * slotTicks + slotRand.nextInt(slotTicks - slotBehavior.defaultDuration() + 1);
                behaviorSeed = slotRand.nextLong();
                active = null;
            }
            long elapsed = gameTime - startsAt;
            if (elapsed >= 0 && elapsed < slotBehavior.defaultDuration()) {
                if (active == null) {
                    active = new BehaviorInstance(slotBehavior, helper, slotBehavior.defaultDuration(), behaviorSeed);
                    slotBehavior.onStart(active);
                }
                active.age = (int) elapsed;
            } else {
                active = null;
            }
        }

        for (int part : parts) {
            for (int index = 0; index < eyes[part].length; index++) {
                influence.reset();
                if (active != null) {
                    active.behavior.influence(active, part, index, influence);
                }
                GooglyTracker.EyeInfo pupil = eyes[part][index];
                pupil.applyOverlays(influence, 1F, 1F, 0F, 0F);
                if (resetMotion && !stationaryHead) {
                    pupil.resetMotion();
                }
                pupil.update(rand, yaw, pitch, mx, my, mz,
                        influence.anchorX, influence.anchorY, influence.stiffness, 0F);
            }
        }
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
