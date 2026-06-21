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

/**
 * A <b>sparse</b> override of an eye's {@link EyeAppearance}: each field is optional and, when absent,
 * falls back to the underlying value (a mob's datapack appearance, via {@link EyeAppearance#overlay}).
 *
 * <p>This is the one portable appearance payload, driven off a single {@link #CODEC} so its three
 * storage locations cannot drift:
 * <ul>
 *   <li>an eye <b>item</b>'s stack NBT (what survives crafting / harvest),</li>
 *   <li>a mob's per-entity override (see {@link EyeState}),</li>
 *   <li>the network sync payload ({@code EyeStatePacket}).</li>
 * </ul>
 *
 * <p><b>Appearance only.</b> Placement — position, scale, angles, attach part — is never here; it always
 * comes from the mob's datapack config ({@link com.github.crittscott.somegoogly.head.EyePlacement}). An
 * eye item carries "what the eye looks like", and attaching it reuses that mob's configured placement.
 *
 * <p>Behaviors (stare, blink, swirl, …) are transient triggered effects, not appearance; they have no
 * presence here (see {@link com.github.crittscott.somegoogly.behavior.EyeBehaviors}).
 */
public record AppearanceOverride(Optional<EyeColor> cornea, Optional<EyeColor> iris, Optional<Boolean> glow) {

    public static final AppearanceOverride EMPTY =
            new AppearanceOverride(Optional.empty(), Optional.empty(), Optional.empty());

    public static final Codec<AppearanceOverride> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            EyeColor.CODEC.optionalFieldOf("corneaColor").forGetter(AppearanceOverride::cornea),
            EyeColor.CODEC.optionalFieldOf("irisColor").forGetter(AppearanceOverride::iris),
            Codec.BOOL.optionalFieldOf("glow").forGetter(AppearanceOverride::glow)
    ).apply(inst, AppearanceOverride::new));

    public boolean isEmpty() {
        return cornea.isEmpty() && iris.isEmpty() && glow.isEmpty();
    }

    /** Return a copy with {@code over}'s present fields layered on top of this one's. */
    public AppearanceOverride merge(AppearanceOverride over) {
        return new AppearanceOverride(
                over.cornea.isPresent() ? over.cornea : cornea,
                over.iris.isPresent() ? over.iris : iris,
                over.glow.isPresent() ? over.glow : glow);
    }

    /** {@code null} clears the field. */
    public AppearanceOverride withCorneaColor(@Nullable EyeColor color) {
        return new AppearanceOverride(Optional.ofNullable(color), iris, glow);
    }

    public AppearanceOverride withIrisColor(@Nullable EyeColor color) {
        return new AppearanceOverride(cornea, Optional.ofNullable(color), glow);
    }

    public AppearanceOverride withGlow(@Nullable Boolean value) {
        return new AppearanceOverride(cornea, iris, Optional.ofNullable(value));
    }

    // --- Serialization (one Codec → NBT for item/entity, JSON for the config bridge) ---

    public CompoundTag toNbt() {
        Tag tag = CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        return tag instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    public static AppearanceOverride fromNbt(@Nullable Tag tag) {
        return tag == null ? EMPTY : CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(EMPTY);
    }

    public JsonElement toJson() {
        return CODEC.encodeStart(JsonOps.INSTANCE, this).result().orElseGet(JsonObject::new);
    }

    public static AppearanceOverride fromJson(JsonElement json) {
        return CODEC.parse(JsonOps.INSTANCE, json).result().orElse(EMPTY);
    }
}
