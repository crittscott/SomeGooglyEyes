package com.github.crittscott.somegoogly.eye.state;

import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.platform.EntityPersistentData;
import com.github.crittscott.somegoogly.platform.Networking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * Per-mob, mutable, mid-life eye state — the override layer that sits on top of the
 * shared datapack config ({@link com.github.crittscott.somegoogly.config.EyeConfigModel}).
 *
 * <p>State lives in the mod-owned persistent entity compound and survives entity save/load,
 * dimension changes, and aging. The loader-specific persistence bridge maps that compound onto each
 * platform's entity storage. Three pieces:
 * <ul>
 *   <li>{@code somegoogly:hasGooglyEyes} — the on/off flag, rolled at spawn and mutable mid-life
 *       (shears remove, slimy eye adds).</li>
 *   <li>{@code somegoogly:eyeVariantRoll} — the 0..1 roll that selects the mob's placement variant,
 *       assigned at first join and redrawn by a slimy-eye application.</li>
 *   <li>{@code somegoogly:eyeOverrides} — an optional compound of per-mob appearance overrides
 *       (iris/cornea tint, glow), applied to all of the mob's eyes.</li>
 * </ul>
 *
 * <p>The read helpers and {@link Snapshot} are side-safe. Server mutation methods write a complete
 * coherent transition and then broadcast one snapshot to the entity's trackers (and the entity itself
 * when it is a player); {@link #sendTo} brings one new tracker up to date. Packet handling installs a
 * snapshot without sending it back to the server.
 */
public final class EyeState {

    // The namespace is written out rather than composed from SomeGooglyCommon.MOD_ID: these are
    // persisted NBT keys, and a future mod-id change must not silently orphan saved eye state.
    public static final String EYE_OVERRIDES = "somegoogly:eyeOverrides";
    public static final String HAS_EYES = "somegoogly:hasGooglyEyes";
    public static final String VARIANT_ROLL = "somegoogly:eyeVariantRoll";

    private EyeState() {
    }

    /** The full portable state synchronized for one living entity. */
    public record Snapshot(boolean hasEyes, float variantRoll, AppearanceOverride properties) {
    }

    /** Install a synchronized snapshot on the client without broadcasting it. */
    public static void applySnapshot(LivingEntity entity, Snapshot snapshot) {
        CompoundTag data = EntityPersistentData.get(entity);
        data.putBoolean(HAS_EYES, snapshot.hasEyes());
        data.putFloat(VARIANT_ROLL, snapshot.variantRoll());
        writeProperties(data, snapshot.properties());
    }

    /** Drop both tint overrides so the mob's eyes fall back to their config colors; broadcasts. */
    public static void clearTints(LivingEntity entity) {
        AppearanceOverride properties = readProperties(entity);
        setProperties(entity, properties.withIrisColor(null).withCorneaColor(null));
    }

    /**
     * The mob's stored placement-variant roll (0..1), assigned at first join and redrawn when a
     * slimy-eye application turns eyes on. Maps onto the current age config's weighted variants.
     * Defaults to 0 (the first variant) when unset. A stored value outside 0..1 (an edited save) reads
     * clamped into range, and a non-finite one as 0.
     */
    public static float getVariantRoll(LivingEntity entity) {
        float roll = EntityPersistentData.get(entity).getFloat(VARIANT_ROLL);
        return Float.isFinite(roll) ? Mth.clamp(roll, 0.0F, 1.0F) : 0.0F;
    }

    /** Whether the entity currently has eyes; {@code false} when the flag has never been set. */
    public static boolean hasEyes(LivingEntity entity) {
        return EntityPersistentData.get(entity).getBoolean(HAS_EYES);
    }

    /**
     * Whether this entity's state matches what a freshly created client entity already assumes:
     * no eyes and no appearance overrides. The start-tracking and spawn-time syncs skip the packet
     * in this case; a mid-life mutation always sends, because the client may then hold a snapshot
     * that has to be transitioned back to the default.
     */
    public static boolean isDefaultState(LivingEntity entity) {
        return !hasEyes(entity) && readProperties(entity).isEmpty();
    }

    /** Whether the server has made this entity's one-time natural-eye decision. */
    public static boolean isInitialized(LivingEntity entity) {
        return EntityPersistentData.get(entity).contains(HAS_EYES);
    }

    /**
     * Store the one-time natural-eye decision and placement roll. Broadcasts only when the decision
     * turned eyes on: an eyeless outcome already equals every client's implicit default, so the
     * eyeless spawn — the overwhelming majority — costs no packet. The eyed broadcast is kept so a
     * player who is already tracking when this runs is corrected; later trackers get it from the
     * start-tracking sync.
     */
    public static void initialize(LivingEntity entity, boolean hasEyes, float variantRoll) {
        CompoundTag data = EntityPersistentData.get(entity);
        data.putBoolean(HAS_EYES, hasEyes);
        data.putFloat(VARIANT_ROLL, variantRoll);
        if (hasEyes) {
            ServerBehaviorScheduler.onEyesGained(entity);
            sync(entity);
        }
    }

    /** The overrides compound, or {@code null} if absent (used by the sync packet). */
    @Nullable
    public static CompoundTag overridesTagOrNull(LivingEntity entity) {
        CompoundTag data = EntityPersistentData.get(entity);
        return data.contains(EYE_OVERRIDES, Tag.TAG_COMPOUND) ? data.getCompound(EYE_OVERRIDES) : null;
    }

    /**
     * Read the mob's appearance override as {@link AppearanceOverride} ({@link AppearanceOverride#EMPTY}
     * when none). The override compound is serialized via the shared {@code AppearanceOverride} codec —
     * the same schema an eye item carries — so item↔mob transfer is a straight property copy.
     */
    public static AppearanceOverride readProperties(LivingEntity entity) {
        return AppearanceOverride.fromNbt(overridesTagOrNull(entity));
    }

    /** Read all synchronized fields as one coherent value. */
    public static Snapshot snapshot(LivingEntity entity) {
        return new Snapshot(hasEyes(entity), getVariantRoll(entity), readProperties(entity));
    }

    /** Apply a portable appearance, reroll placement, and enable eyes with one synchronization. */
    public static void enableWithProperties(LivingEntity entity, AppearanceOverride properties) {
        CompoundTag data = EntityPersistentData.get(entity);
        data.putFloat(VARIANT_ROLL, entity.getRandom().nextFloat());
        writeProperties(data, properties);
        data.putBoolean(HAS_EYES, true);
        ServerBehaviorScheduler.onEyesGained(entity);
        sync(entity);
    }

    /** Disable eyes and clear their entity-wide appearance with one synchronization. */
    public static void disableAndClearProperties(LivingEntity entity) {
        CompoundTag data = EntityPersistentData.get(entity);
        data.putBoolean(HAS_EYES, false);
        writeProperties(data, AppearanceOverride.EMPTY);
        sync(entity);
    }

    /** Override the cornea color for every one of the mob's eyes; broadcasts. */
    public static void setCorneaTint(LivingEntity entity, EyeColor color) {
        setProperties(entity, readProperties(entity).withCorneaColor(color));
    }

    /** {@code null} clears the override (eyes fall back to per-eye config glow). */
    public static void setGlow(LivingEntity entity, @Nullable Boolean glow) {
        setProperties(entity, readProperties(entity).withGlow(glow));
    }

    /** Flip the on/off flag alone and broadcast, leaving the variant roll and appearance overrides untouched. */
    public static void setHasEyes(LivingEntity entity, boolean hasEyes) {
        EntityPersistentData.get(entity).putBoolean(HAS_EYES, hasEyes);
        if (hasEyes) {
            ServerBehaviorScheduler.onEyesGained(entity);
        }
        sync(entity);
    }

    /** Override the iris color for every one of the mob's eyes; broadcasts. */
    public static void setIrisTint(LivingEntity entity, EyeColor color) {
        setProperties(entity, readProperties(entity).withIrisColor(color));
    }

    /**
     * Replace the mob's whole appearance override (used by the slimy eye and harvest). Writes the
     * {@link AppearanceOverride} as the {@code somegoogly:eyeOverrides} compound and broadcasts.
     */
    public static void setProperties(LivingEntity entity, AppearanceOverride properties) {
        writeProperties(EntityPersistentData.get(entity), properties);
        sync(entity);
    }

    private static void writeProperties(CompoundTag data, AppearanceOverride properties) {
        if (properties.isEmpty()) {
            data.remove(EYE_OVERRIDES);
        } else {
            data.put(EYE_OVERRIDES, properties.toNbt());
        }
    }

    /**
     * Send one newly tracking player the entity's snapshot, unless it equals the client default
     * ({@link #isDefaultState}).
     */
    public static void sendTo(LivingEntity entity, ServerPlayer player) {
        if (!isDefaultState(entity)) {
            Networking.sendToPlayer(player, packet(entity));
        }
    }

    private static void sync(LivingEntity entity) {
        Networking.sendTracking(entity, true, packet(entity));
    }

    private static EyeStatePacket packet(LivingEntity entity) {
        return new EyeStatePacket(entity.getId(), entity.getUUID(), snapshot(entity));
    }
}
