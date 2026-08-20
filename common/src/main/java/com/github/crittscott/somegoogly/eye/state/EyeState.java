package com.github.crittscott.somegoogly.eye.state;

import com.github.crittscott.somegoogly.platform.EntityPersistentData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * Per-mob, mutable, mid-life eye state — the override layer that sits on top of the
 * shared datapack config ({@link com.github.crittscott.somegoogly.eye.HeadInfo}).
 *
 * <p>State lives in the entity's persistent data (saved by Forge under {@code ForgeData}, so it
 * survives reload / dimension change / growing up). Three pieces:
 * <ul>
 *   <li>{@code somegoogly:hasGooglyEyes} — the on/off flag, rolled at spawn and mutable mid-life
 *       (shears remove, slimy eye adds).</li>
 *   <li>{@code somegoogly:eyeVariantRoll} — the 0..1 roll that selects the mob's placement variant,
 *       assigned at first join and redrawn by a slimy-eye application ({@link #rerollVariant}).</li>
 *   <li>{@code somegoogly:eyeOverrides} — an optional compound of per-mob appearance overrides
 *       (iris/cornea tint, glow), applied to all of the mob's eyes.</li>
 * </ul>
 *
 * <p>The read helpers are side-safe (renderer + packet use them). The {@code set*} mutation API is
 * server-only: each call writes NBT and then broadcasts the full state to every tracking client via
 * {@code EyeStatePacket}, so changes appear immediately without waiting for re-tracking.
 */
public final class EyeState {

    public static final String EYE_OVERRIDES = "somegoogly:eyeOverrides";
    public static final String HAS_EYES = "somegoogly:hasGooglyEyes";
    public static final String VARIANT_ROLL = "somegoogly:eyeVariantRoll";

    private EyeState() {
    }

    /** Replace (or clear, when {@code null}) the overrides compound on the client from a synced packet. */
    public static void applyOverridesTag(LivingEntity entity, @Nullable CompoundTag overrides) {
        if (overrides == null || overrides.isEmpty()) {
            EntityPersistentData.get(entity).remove(EYE_OVERRIDES);
        } else {
            EntityPersistentData.get(entity).put(EYE_OVERRIDES, overrides);
        }
    }

    /** Write the synced placement-variant roll onto the client's copy of the entity. */
    public static void applyVariantRoll(LivingEntity entity, float roll) {
        EntityPersistentData.get(entity).putFloat(VARIANT_ROLL, roll);
    }

    public static void clearCorneaTint(LivingEntity entity) {
        setProperties(entity, readProperties(entity).withCorneaColor(null));
    }

    public static void clearIrisTint(LivingEntity entity) {
        setProperties(entity, readProperties(entity).withIrisColor(null));
    }

    /**
     * The mob's stored placement-variant roll (0..1), assigned at first join and redrawn by
     * {@link #rerollVariant} when a slimy-eye application turns eyes on. Maps onto the current age
     * config's weighted variants via {@code HeadInfo.chooseVariantIndex}. Defaults to 0 (the first
     * variant) when unset.
     */
    public static float getVariantRoll(LivingEntity entity) {
        return EntityPersistentData.get(entity).getFloat(VARIANT_ROLL);
    }

    public static boolean hasEyes(LivingEntity entity) {
        return EntityPersistentData.get(entity).getBoolean(HAS_EYES);
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

    /**
     * Draw a fresh placement-variant roll for the mob and broadcast. Called as a slimy-eye
     * application turns eyes on, so every application produces a newly rolled arrangement.
     */
    public static void rerollVariant(LivingEntity entity) {
        EntityPersistentData.get(entity).putFloat(VARIANT_ROLL, entity.getRandom().nextFloat());
        sync(entity);
    }

    public static void setCorneaTint(LivingEntity entity, EyeColor color) {
        setProperties(entity, readProperties(entity).withCorneaColor(color));
    }

    /** {@code null} clears the override (eyes fall back to per-eye config glow). */
    public static void setGlow(LivingEntity entity, @Nullable Boolean glow) {
        setProperties(entity, readProperties(entity).withGlow(glow));
    }

    public static void setHasEyes(LivingEntity entity, boolean hasEyes) {
        EntityPersistentData.get(entity).putBoolean(HAS_EYES, hasEyes);
        sync(entity);
    }

    public static void setIrisTint(LivingEntity entity, EyeColor color) {
        setProperties(entity, readProperties(entity).withIrisColor(color));
    }

    /**
     * Replace the mob's whole appearance override (used by the slimy eye and harvest). Writes the
     * {@link AppearanceOverride} as the {@code somegoogly:eyeOverrides} compound and broadcasts.
     */
    public static void setProperties(LivingEntity entity, AppearanceOverride properties) {
        CompoundTag data = EntityPersistentData.get(entity);
        if (properties.isEmpty()) {
            data.remove(EYE_OVERRIDES);
        } else {
            data.put(EYE_OVERRIDES, properties.toNbt());
        }
        sync(entity);
    }

    private static void sync(LivingEntity entity) {
        EyeStateSync.sync(entity, hasEyes(entity), getVariantRoll(entity), overridesTagOrNull(entity));
    }
}
