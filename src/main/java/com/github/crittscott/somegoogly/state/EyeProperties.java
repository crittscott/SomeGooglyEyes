package com.github.crittscott.somegoogly.state;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Canonical, portable description of an eye's <b>appearance</b> (and, in future, behaviours).
 *
 * <p>This is the single data model shared by three storage locations, all driven off the one
 * {@link #CODEC} so they cannot drift:
 * <ul>
 *   <li>an eye <b>item</b>'s stack NBT (what survives crafting),</li>
 *   <li>a mob's per-entity <b>override</b> (see {@link EyeState}),</li>
 *   <li>the network <b>sync</b> payload ({@code EyeStatePacket}).</li>
 * </ul>
 *
 * <p><b>Property vs. placement.</b> These are intentionally the <i>placement-independent</i>
 * attributes only. Geometry — position, scale, angles, attach part — is NOT here; it always comes
 * from the mob's datapack config ({@link com.github.crittscott.somegoogly.head.HeadInfo}). An eye
 * item therefore carries "what the eye looks like", and attaching it to a mob reuses that mob's
 * configured placement. (This is why "placement is automatic" in the design notes.)
 *
 * <p>All fields are {@link Optional}: a sparse instance acts as an <i>override</i> over config
 * (absent = fall back to config), while a "complete" instance (e.g. a harvested eye item) simply
 * fills them all in.
 *
 * <p>FUTURE — unify everything: the datapack geometry ({@code HeadInfo.EyeConfig}, currently Gson)
 * and the picker exporter could migrate onto this same {@code Codec} via {@link JsonOps}, so config,
 * item, and entity share one schema. Deliberately not done yet (keeps the working config-load and
 * picker-export paths untouched).
 *
 * <p>NOTE — behaviours live elsewhere. The eye expressions (stare, blink, swirl, …) are <i>universal,
 * transient effects</i> a mob plays on trigger, not per-eye properties: they're scheduled server-side
 * and animated client-side (see {@link com.github.crittscott.somegoogly.behavior.EyeBehaviors}), with
 * no presence in this appearance record. So nothing behaviour-related is stored here.
 */
public record EyeProperties(OptionalInt corneaColor, OptionalInt irisColor, Optional<Boolean> glow) {

    public static final EyeProperties EMPTY =
            new EyeProperties(OptionalInt.empty(), OptionalInt.empty(), Optional.empty());

    public static final Codec<EyeProperties> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.optionalFieldOf("corneaColor").forGetter(p -> boxed(p.corneaColor)),
            Codec.INT.optionalFieldOf("irisColor").forGetter(p -> boxed(p.irisColor)),
            Codec.BOOL.optionalFieldOf("glow").forGetter(EyeProperties::glow)
    ).apply(inst, (cornea, iris, glow) -> new EyeProperties(unboxed(cornea), unboxed(iris), glow)));

    public boolean isEmpty() {
        return corneaColor.isEmpty() && irisColor.isEmpty() && glow.isEmpty();
    }

    /** Return a copy with {@code over}'s present fields layered on top of this one's. */
    public EyeProperties merge(EyeProperties over) {
        return new EyeProperties(
                over.corneaColor.isPresent() ? over.corneaColor : corneaColor,
                over.irisColor.isPresent() ? over.irisColor : irisColor,
                over.glow.isPresent() ? over.glow : glow);
    }

    /** {@code null} clears the field. Colours are masked to {@code 0xRRGGBB}. */
    public EyeProperties withCorneaColor(@Nullable Integer rgb) {
        return new EyeProperties(opt(rgb), irisColor, glow);
    }

    public EyeProperties withIrisColor(@Nullable Integer rgb) {
        return new EyeProperties(corneaColor, opt(rgb), glow);
    }

    public EyeProperties withGlow(@Nullable Boolean value) {
        return new EyeProperties(corneaColor, irisColor, Optional.ofNullable(value));
    }

    // --- Serialization (one Codec → NBT for item/entity, JSON for the future config bridge) ---

    public CompoundTag toNbt() {
        Tag tag = CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        return tag instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    public static EyeProperties fromNbt(@Nullable Tag tag) {
        return tag == null ? EMPTY : CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(EMPTY);
    }

    public JsonElement toJson() {
        return CODEC.encodeStart(JsonOps.INSTANCE, this).result().orElseGet(JsonObject::new);
    }

    public static EyeProperties fromJson(JsonElement json) {
        return CODEC.parse(JsonOps.INSTANCE, json).result().orElse(EMPTY);
    }

    private static Optional<Integer> boxed(OptionalInt value) {
        return value.isPresent() ? Optional.of(value.getAsInt()) : Optional.empty();
    }

    private static OptionalInt unboxed(Optional<Integer> value) {
        return value.map(OptionalInt::of).orElseGet(OptionalInt::empty);
    }

    private static OptionalInt opt(@Nullable Integer rgb) {
        return rgb == null ? OptionalInt.empty() : OptionalInt.of(rgb & 0xFFFFFF);
    }
}
