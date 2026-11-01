package com.github.crittscott.somegoogly.eye.state;

import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.platform.EntityPersistentData;
import com.github.crittscott.somegoogly.platform.Networking;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * Persistent, mutable entity eye state — the override layer that sits on top of the
 * shared datapack config ({@link com.github.crittscott.somegoogly.config.EyeConfigModel}).
 *
 * <p>State lives in the mod-owned persistent entity compound and survives entity save/load,
 * dimension changes, aging, mob conversion, and a player leaving the End ({@link #copy}). The loader-specific persistence bridge maps that compound onto each
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

    /**
     * The full portable eye state of one supported entity or placed mob head, and its one NBT form: the three
     * keys above in a mod-owned compound. A head item carries it as a data component instead, through
     * {@link #CODEC} and {@link #STREAM_CODEC}.
     */
    public record Snapshot(boolean hasEyes, float variantRoll, AppearanceOverride properties) {

        public static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("has_eyes").forGetter(Snapshot::hasEyes),
                Codec.floatRange(0.0F, 1.0F).fieldOf("variant_roll").forGetter(Snapshot::variantRoll),
                AppearanceOverride.CODEC.optionalFieldOf("properties", AppearanceOverride.EMPTY)
                        .forGetter(Snapshot::properties)
        ).apply(instance, Snapshot::new));
        /** No eyes, the first variant, no appearance overrides; what an absent compound reads as. */
        public static final Snapshot NONE = new Snapshot(false, 0.0F, AppearanceOverride.EMPTY);

        /** The roll is clamped on decode, as a stored roll is. */
        public static final StreamCodec<ByteBuf, Snapshot> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Snapshot::hasEyes,
                ByteBufCodecs.FLOAT.map(EyeState::clampRoll, roll -> roll), Snapshot::variantRoll,
                AppearanceOverride.STREAM_CODEC, Snapshot::properties,
                Snapshot::new);

        /** Read the state keys of {@code data}; absent keys read as {@link #NONE}'s values. */
        public static Snapshot read(CompoundTag data) {
            return new Snapshot(data.getBoolean(HAS_EYES), EyeState.variantRoll(data), AppearanceOverride.fromNbt(
                    data.contains(EYE_OVERRIDES, Tag.TAG_COMPOUND) ? data.getCompound(EYE_OVERRIDES) : null));
        }

        /** Write every state key into {@code data}, removing the overrides compound when there are none. */
        public void write(CompoundTag data) {
            data.putBoolean(HAS_EYES, hasEyes);
            data.putFloat(VARIANT_ROLL, variantRoll);
            writeProperties(data, properties);
        }
    }

    /** Install a synchronized snapshot on the client without broadcasting it. */
    public static void applySnapshot(Entity entity, Snapshot snapshot) {
        snapshot.write(EntityPersistentData.get(entity));
    }

    /** {@code roll} clamped into 0..1, with a non-finite value read as 0. */
    private static float clampRoll(float roll) {
        return Float.isFinite(roll) ? Mth.clamp(roll, 0.0F, 1.0F) : 0.0F;
    }

    /** Drop both tint overrides so the mob's eyes fall back to their config colors; broadcasts. */
    public static void clearTints(Entity entity) {
        AppearanceOverride properties = readProperties(entity);
        setProperties(entity, properties.withIrisColor(null).withCorneaColor(null));
    }

    /**
     * Carry the whole eye state from an entity Minecraft is replacing to its replacement (a mob conversion,
     * or a player leaving the End), so the replacement keeps its eyes instead of rolling fresh. Copying
     * before or after the replacement joins its level both work: an initialized target is never re-rolled,
     * and the broadcast reaches whoever already tracks it.
     */
    public static void copy(LivingEntity source, LivingEntity target) {
        if (!isInitialized(source)) {
            return;
        }
        snapshot(source).write(EntityPersistentData.get(target));
        if (hasEyes(target)) {
            ServerBehaviorScheduler.onEyesGained(target);
        }
        sync(target);
    }

    /** Disable eyes and clear their entity-wide appearance with one synchronization. */
    public static void disableAndClearProperties(Entity entity) {
        CompoundTag data = EntityPersistentData.get(entity);
        data.putBoolean(HAS_EYES, false);
        writeProperties(data, AppearanceOverride.EMPTY);
        sync(entity);
    }

    /** Apply a portable appearance, reroll placement, and enable eyes with one synchronization. */
    public static void enableWithProperties(Entity entity, AppearanceOverride properties) {
        CompoundTag data = EntityPersistentData.get(entity);
        float roll = (entity instanceof LivingEntity living ? living.getRandom() : entity.level().random).nextFloat();
        data.putFloat(VARIANT_ROLL, roll);
        writeProperties(data, properties);
        data.putBoolean(HAS_EYES, true);
        if (entity instanceof LivingEntity living) {
            ServerBehaviorScheduler.onEyesGained(living);
        }
        sync(entity);
    }

    /**
     * The entity's stored placement-variant roll (0..1), assigned at first join and redrawn when a
     * slimy-eye application turns eyes on. Maps onto the current age config's weighted variants.
     * Defaults to 0 (the first variant) when unset. A stored value outside 0..1 (an edited save) reads
     * clamped into range, and a non-finite one as 0.
     */
    public static float getVariantRoll(Entity entity) {
        return variantRoll(EntityPersistentData.get(entity));
    }

    /** Whether the entity currently has eyes; {@code false} when the flag has never been set. */
    public static boolean hasEyes(Entity entity) {
        return EntityPersistentData.get(entity).getBoolean(HAS_EYES);
    }

    /**
     * Store the one-time natural-eye decision and placement roll. Broadcasts to current trackers only when
     * the decision turned eyes on, since an eyeless entity already matches every client's default; later
     * trackers get the state from the start-tracking sync.
     */
    public static void initialize(Entity entity, boolean hasEyes, float variantRoll) {
        CompoundTag data = EntityPersistentData.get(entity);
        data.putBoolean(HAS_EYES, hasEyes);
        data.putFloat(VARIANT_ROLL, variantRoll);
        if (hasEyes) {
            if (entity instanceof LivingEntity living) {
                ServerBehaviorScheduler.onEyesGained(living);
            }
            sync(entity);
        }
    }

    /**
     * Whether this entity's state matches what a freshly created client entity already assumes:
     * no eyes and no appearance overrides. The start-tracking and spawn-time syncs skip the packet
     * in this case; a mid-life mutation always sends, because the client may then hold a snapshot
     * that has to be transitioned back to the default.
     */
    public static boolean isDefaultState(Entity entity) {
        return !hasEyes(entity) && readProperties(entity).isEmpty();
    }

    /** Whether the server has made this entity's one-time natural-eye decision. */
    public static boolean isInitialized(Entity entity) {
        return EntityPersistentData.get(entity).contains(HAS_EYES);
    }

    /** The overrides compound, or {@code null} if absent (used by the sync packet). */
    @Nullable
    public static CompoundTag overridesTagOrNull(Entity entity) {
        CompoundTag data = EntityPersistentData.get(entity);
        return data.contains(EYE_OVERRIDES, Tag.TAG_COMPOUND) ? data.getCompound(EYE_OVERRIDES) : null;
    }

    private static EyeStatePacket packet(Entity entity) {
        return new EyeStatePacket(entity.getId(), snapshot(entity));
    }

    /**
     * Read the mob's appearance override as {@link AppearanceOverride} ({@link AppearanceOverride#EMPTY}
     * when none). The override compound is serialized via the shared {@code AppearanceOverride} codec —
     * the same schema an eye item carries — so item↔mob transfer is a straight property copy.
     */
    public static AppearanceOverride readProperties(Entity entity) {
        return AppearanceOverride.fromNbt(overridesTagOrNull(entity));
    }

    /**
     * Send one newly tracking player the entity's snapshot, unless it equals the client default
     * ({@link #isDefaultState}).
     */
    public static void sendTo(Entity entity, ServerPlayer player) {
        if (!isDefaultState(entity)) {
            Networking.sendToPlayer(player, packet(entity));
        }
    }

    /** Override the cornea color for every one of the mob's eyes; broadcasts. */
    public static void setCorneaTint(Entity entity, EyeColor color) {
        setProperties(entity, readProperties(entity).withCorneaColor(color));
    }

    /** {@code null} clears the override (eyes fall back to per-eye config glow). */
    public static void setGlow(Entity entity, @Nullable Boolean glow) {
        setProperties(entity, readProperties(entity).withGlow(glow));
    }

    /** Flip the on/off flag alone and broadcast, leaving the variant roll and appearance overrides untouched. */
    public static void setHasEyes(Entity entity, boolean hasEyes) {
        EntityPersistentData.get(entity).putBoolean(HAS_EYES, hasEyes);
        if (hasEyes) {
            if (entity instanceof LivingEntity living) {
                ServerBehaviorScheduler.onEyesGained(living);
            }
        }
        sync(entity);
    }

    /** Override the iris color for every one of the mob's eyes; broadcasts. */
    public static void setIrisTint(Entity entity, EyeColor color) {
        setProperties(entity, readProperties(entity).withIrisColor(color));
    }

    /**
     * Replace the mob's whole appearance override (used by the slimy eye and harvest). Writes the
     * {@link AppearanceOverride} as the {@code somegoogly:eyeOverrides} compound and broadcasts.
     */
    public static void setProperties(Entity entity, AppearanceOverride properties) {
        writeProperties(EntityPersistentData.get(entity), properties);
        sync(entity);
    }

    /** Read all synchronized fields as one coherent value. */
    public static Snapshot snapshot(Entity entity) {
        return Snapshot.read(EntityPersistentData.get(entity));
    }

    private static void sync(Entity entity) {
        Networking.sendTrackingAndSelf(entity, packet(entity));
    }

    /** The stored roll of {@code data}, clamped ({@link #clampRoll}). */
    private static float variantRoll(CompoundTag data) {
        return clampRoll(data.getFloat(VARIANT_ROLL));
    }

    private static void writeProperties(CompoundTag data, AppearanceOverride properties) {
        if (properties.isEmpty()) {
            data.remove(EYE_OVERRIDES);
        } else {
            data.put(EYE_OVERRIDES, properties.toNbt());
        }
    }
}
