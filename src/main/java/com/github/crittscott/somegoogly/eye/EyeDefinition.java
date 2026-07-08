package com.github.crittscott.somegoogly.eye;

import com.github.crittscott.somegoogly.eye.state.EyeAppearance;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The complete datapack description of one eye on a mob: its {@link EyePlacement} (where/how big/aimed)
 * plus its {@link EyeAppearance} (color/glow).
 *
 * <p>The two halves are kept as separate value types because the rest of the mod treats them
 * differently — appearance is portable/overridable (an eye item carries it), placement is fixed to the
 * mob. But they share one <b>flat</b> JSON object: {@link #CODEC} merges both {@link com.mojang.serialization.MapCodec}s
 * at the same level, so the datapack files keep their existing flat field layout
 * ({@code position}, {@code eyeScale}, …, {@code corneaColors}, {@code glows}).
 */
public record EyeDefinition(EyePlacement placement, EyeAppearance appearance) {

    public static final Codec<EyeDefinition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            EyePlacement.MAP_CODEC.forGetter(EyeDefinition::placement),
            EyeAppearance.MAP_CODEC.forGetter(EyeDefinition::appearance)
    ).apply(inst, EyeDefinition::new));

    public static final EyeDefinition DEFAULT = new EyeDefinition(EyePlacement.DEFAULT, EyeAppearance.DEFAULT);
}
