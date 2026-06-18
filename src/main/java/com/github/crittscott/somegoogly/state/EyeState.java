package com.github.crittscott.somegoogly.state;

import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * Per-mob, mutable, mid-life eye state — the "Keystone A" override layer that sits on top of the
 * shared datapack config ({@link com.github.crittscott.somegoogly.head.HeadInfo}).
 *
 * <p>State lives in the entity's persistent data (saved by Forge under {@code ForgeData}, so it
 * survives reload / dimension change / growing up). Two pieces:
 * <ul>
 *   <li>{@code somegoogly:hasGooglyEyes} — the on/off flag, originally a once-at-spawn decision,
 *       now mutable (shears remove, potion adds).</li>
 *   <li>{@code somegoogly:eyeOverrides} — an optional compound of per-mob appearance overrides
 *       (iris/cornea tint, glow), applied to all of the mob's eyes.</li>
 * </ul>
 *
 * <p>The read helpers are side-safe (renderer + packet use them). The {@code set*} mutation API is
 * server-only: each call writes NBT and then broadcasts the full state to every tracking client via
 * {@link EyeStatePacket}, so changes appear immediately without waiting for re-tracking.
 */
public final class EyeState {

    public static final String HAS_EYES = "somegoogly:hasGooglyEyes";
    public static final String EYE_OVERRIDES = "somegoogly:eyeOverrides";

    private EyeState() {
    }

    // --- Reads (side-safe) ---------------------------------------------------

    public static boolean hasEyes(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(HAS_EYES);
    }

    /**
     * Read the mob's appearance override as {@link EyeProperties} ({@link EyeProperties#EMPTY} when
     * none). The override compound is serialized via the shared {@code EyeProperties} codec — the same
     * schema an eye item carries — so item↔mob transfer is a straight property copy.
     *
     * <p>FUTURE — unify everything: the datapack geometry ({@code HeadInfo.EyeConfig}) could also move
     * onto this codec so config, override, and item share one schema (see {@link EyeProperties}).
     */
    public static EyeProperties readProperties(LivingEntity entity) {
        return EyeProperties.fromNbt(overridesTagOrNull(entity));
    }

    /** The overrides compound, or {@code null} if absent (used by the sync packet). */
    @Nullable
    public static CompoundTag overridesTagOrNull(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.contains(EYE_OVERRIDES, Tag.TAG_COMPOUND) ? data.getCompound(EYE_OVERRIDES) : null;
    }

    /** Replace (or clear, when {@code null}) the overrides compound on the client from a synced packet. */
    public static void applyOverridesTag(LivingEntity entity, @Nullable CompoundTag overrides) {
        if (overrides == null || overrides.isEmpty()) {
            entity.getPersistentData().remove(EYE_OVERRIDES);
        } else {
            entity.getPersistentData().put(EYE_OVERRIDES, overrides);
        }
    }

    /** Unpack a stored {@code 0xRRGGBB} colour into the {@code float[3]} (0..1) the renderer expects. */
    public static float[] unpackColor(int rgb) {
        return new float[]{
                ((rgb >> 16) & 0xFF) / 255F,
                ((rgb >> 8) & 0xFF) / 255F,
                (rgb & 0xFF) / 255F
        };
    }

    /** Pack a {@code float[3]} (0..1) colour into {@code 0xRRGGBB} (used when capturing config colours). */
    public static int packColor(float[] rgb) {
        return (channel(rgb[0]) << 16) | (channel(rgb[1]) << 8) | channel(rgb[2]);
    }

    private static int channel(float v) {
        int i = Math.round(v * 255F);
        return i < 0 ? 0 : Math.min(i, 255);
    }

    // --- Mutations (server-only; write NBT then broadcast) -------------------

    public static void setHasEyes(LivingEntity entity, boolean hasEyes) {
        entity.getPersistentData().putBoolean(HAS_EYES, hasEyes);
        sync(entity);
    }

    public static void setIrisTint(LivingEntity entity, int rgb) {
        setProperties(entity, readProperties(entity).withIrisColor(rgb));
    }

    public static void clearIrisTint(LivingEntity entity) {
        setProperties(entity, readProperties(entity).withIrisColor(null));
    }

    public static void setCorneaTint(LivingEntity entity, int rgb) {
        setProperties(entity, readProperties(entity).withCorneaColor(rgb));
    }

    public static void clearCorneaTint(LivingEntity entity) {
        setProperties(entity, readProperties(entity).withCorneaColor(null));
    }

    /** {@code null} clears the override (eyes fall back to per-eye config glow). */
    public static void setGlow(LivingEntity entity, @Nullable Boolean glow) {
        setProperties(entity, readProperties(entity).withGlow(glow));
    }

    /**
     * Replace the mob's whole appearance override (used by {@link EyeHolder} / item reattach). Writes
     * the {@link EyeProperties} as the {@code somegoogly:eyeOverrides} compound and broadcasts.
     */
    public static void setProperties(LivingEntity entity, EyeProperties properties) {
        CompoundTag data = entity.getPersistentData();
        if (properties.isEmpty()) {
            data.remove(EYE_OVERRIDES);
        } else {
            data.put(EYE_OVERRIDES, properties.toNbt());
        }
        sync(entity);
    }

    private static void sync(LivingEntity entity) {
        EyeStatePacket packet = new EyeStatePacket(entity.getId(), hasEyes(entity), overridesTagOrNull(entity));
        NetworkHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
    }
}
