package com.github.crittscott.somegoogly.eye.state;

import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * A <b>sparse</b> override of an eye's {@link EyeAppearance}: each field is optional and, when absent,
 * falls back to the underlying value (a mob's datapack appearance, via {@link EyeAppearance#overlay}).
 *
 * <p>This is the one portable appearance payload. Persistent forms use {@link #toNbt} and {@link #fromNbt},
 * which runs while drawing; the bounded network packet ({@code EyeStatePacket}) uses {@link #write} and
 * {@link #read}. It serves three uses:
 * <ul>
 *   <li>an eye <b>item</b>'s stack NBT (what survives crafting / harvest),</li>
 *   <li>a mob's per-entity override (see {@link EyeState}),</li>
 *   <li>the network sync payload ({@code EyeStatePacket}).</li>
 * </ul>
 *
 * <p><b>Appearance only.</b> Placement — position, scale, angles, attach part — is never here; it always
 * comes from the mob's datapack config ({@link com.github.crittscott.somegoogly.eye.EyePlacement}). An
 * eye item carries "what the eye looks like", and attaching it reuses that mob's configured placement.
 *
 * <p>Behaviors (stare, blink, swirl, …) are transient triggered effects, not appearance; they have no
 * presence here (see {@link EyeBehavior}).
 */
public record AppearanceOverride(Optional<EyeColor> cornea, Optional<EyeColor> iris, Optional<Boolean> glow) {

    private static final String CORNEA_KEY = "corneaColor";
    public static final AppearanceOverride EMPTY =
            new AppearanceOverride(Optional.empty(), Optional.empty(), Optional.empty());
    private static final String GLOW_KEY = "glow";
    private static final String IRIS_KEY = "irisColor";

    /**
     * Decode an override, returning {@link #EMPTY} when {@code tag} is not a compound; a field that is
     * missing or malformed, or a color out of range, reads as unset.
     */
    public static AppearanceOverride fromNbt(@Nullable Tag tag) {
        if (!(tag instanceof CompoundTag data)) {
            return EMPTY;
        }
        return new AppearanceOverride(EyeColor.fromNbt(data.get(CORNEA_KEY)), EyeColor.fromNbt(data.get(IRIS_KEY)),
                data.contains(GLOW_KEY, Tag.TAG_ANY_NUMERIC) ? Optional.of(data.getBoolean(GLOW_KEY)) : Optional.empty());
    }

    /** Read the network form written by {@link #write}; an out-of-range color fails the decode. */
    public static AppearanceOverride read(FriendlyByteBuf buffer) {
        return new AppearanceOverride(
                buffer.readOptional(EyeColor::read),
                buffer.readOptional(EyeColor::read),
                buffer.readOptional(FriendlyByteBuf::readBoolean));
    }

    /** Write the network form: each field prefixed with a presence boolean. */
    public void write(FriendlyByteBuf buffer) {
        buffer.writeOptional(cornea, (out, color) -> color.write(out));
        buffer.writeOptional(iris, (out, color) -> color.write(out));
        buffer.writeOptional(glow, FriendlyByteBuf::writeBoolean);
    }

    /** Whether no field is set, so this override contributes nothing on {@link EyeAppearance#overlay}. */
    public boolean isEmpty() {
        return cornea.isEmpty() && iris.isEmpty() && glow.isEmpty();
    }

    /** Whether every set color channel is in range; an unset channel is always valid. */
    public boolean isValid() {
        return cornea.map(EyeColor::isValid).orElse(true) && iris.map(EyeColor::isValid).orElse(true);
    }

    /** Encode this override as a compound. */
    public CompoundTag toNbt() {
        CompoundTag data = new CompoundTag();
        cornea.ifPresent(color -> data.put(CORNEA_KEY, color.toNbt()));
        iris.ifPresent(color -> data.put(IRIS_KEY, color.toNbt()));
        glow.ifPresent(value -> data.putBoolean(GLOW_KEY, value));
        return data;
    }

    /** {@code null} clears the field. */
    public AppearanceOverride withCorneaColor(@Nullable EyeColor color) {
        return new AppearanceOverride(Optional.ofNullable(color), iris, glow);
    }

    /** {@code null} clears the field. */
    public AppearanceOverride withGlow(@Nullable Boolean value) {
        return new AppearanceOverride(cornea, iris, Optional.ofNullable(value));
    }

    /** {@code null} clears the field. */
    public AppearanceOverride withIrisColor(@Nullable EyeColor color) {
        return new AppearanceOverride(cornea, Optional.ofNullable(color), glow);
    }
}
