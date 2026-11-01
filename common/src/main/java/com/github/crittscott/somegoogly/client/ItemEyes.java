package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.EyeState.Snapshot;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorInstance;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeInfluence;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * One item presentation's loose eye or configured head-eye layout and ambient expressions. Its identity and
 * supplied clock determine expression age; physics is local and disposable. The renderer supplies gravity.
 */
public final class ItemEyes {

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
    public final int[] parts;
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

    public ItemEyes(UUID uuid) {
        this(uuid, null, null, null);
    }

    private ItemEyes(UUID uuid, @Nullable ResourceLocation entity, @Nullable Snapshot snapshot,
                       @Nullable HeadInfo helper) {
        seed = uuid.getMostSignificantBits() ^ Long.rotateLeft(uuid.getLeastSignificantBits(), 1);
        rand = RandomSource.create(seed);
        this.entity = entity;
        this.snapshot = snapshot;
        this.helper = helper;
        eye.prevDeltaY = eye.deltaY = 0F;
        parts = helper == null ? new int[]{0} : SkullEyeState.headParts(helper);
        if (helper == null) {
            eyes = new GooglyTracker.EyeInfo[][]{{eye}};
        } else {
            eyes = new GooglyTracker.EyeInfo[helper.getHeadCount()][];
            for (int head = 0; head < eyes.length; head++) {
                eyes[head] = new GooglyTracker.EyeInfo[helper.getEyeCount(head)];
                for (int index = 0; index < eyes[head].length; index++) {
                    GooglyTracker.EyeInfo pupil = new GooglyTracker.EyeInfo();
                    pupil.prevDeltaY = pupil.deltaY = 0F;
                    pupil.itemGravity = true;
                    eyes[head][index] = pupil;
                }
            }
        }
    }

    /** Animation selected during frame-state extraction, carried to its item draw. */
    public record FrameData(@Nullable ItemEyes eye, float partialTick) {
    }

    /** Implemented by the frame render-state mixin. */
    public interface Holder {
        @Nullable
        FrameData somegoogly$frameEye();

        void somegoogly$setFrameEye(@Nullable FrameData data);
    }

    /** Build the item's current layout; plain heads and unsupported definitions have no simulation. */
    @Nullable
    public static ItemEyes create(UUID uuid, ItemStack stack) {
        if (stack.is(ModContent.GOOGLY_EYE.get())) {
            return new ItemEyes(uuid);
        }
        ResourceLocation entity = SkullEyeState.entityFor(stack);
        Snapshot snapshot = SkullEyeState.itemSnapshot(stack);
        if (entity == null || snapshot == null) {
            return null;
        }
        HeadInfo helper = ClientEyeConfigs.resolveHead(entity, snapshot.variantRoll());
        if (!helper.hasConfig() || SkullEyeState.headParts(helper).length == 0) {
            return null;
        }
        return new ItemEyes(uuid, entity, snapshot, helper);
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
        if (settings != behaviors) {
            settings = behaviors;
            int count = 0;
            for (int part : parts) {
                count += eyes[part].length;
            }
            boolean paired = count > 1;
            pool = behaviors.pool().stream().filter(b -> paired || b != EyeBehavior.CROSS_EYE).toList();
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
                pupil.update(rand, 0F, 0F, 0D, 0D, 0D,
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
