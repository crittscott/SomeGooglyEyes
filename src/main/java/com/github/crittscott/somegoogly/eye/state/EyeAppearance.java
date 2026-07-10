package com.github.crittscott.somegoogly.eye.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The concrete, placement-independent look of an eye: cornea color, iris color, and whether it glows.
 * Always has values (a datapack eye, or {@link #DEFAULT}); the sparse override that layers on top is
 * {@link AppearanceOverride}.
 *
 * <p>Serialized with the datapack JSON's flat field names ({@code corneaColors},
 * {@code irisColors}, {@code glows}), so it embeds directly into {@link com.github.crittscott.somegoogly.eye.EyeDefinition}.
 */
public record EyeAppearance(EyeColor cornea, EyeColor iris, boolean glow) {

    public static final EyeAppearance DEFAULT = new EyeAppearance(EyeColor.WHITE, EyeColor.BLACK, false);

    /** A {@link MapCodec} so {@link com.github.crittscott.somegoogly.eye.EyeDefinition} can flatten these
     *  fields next to placement at the same JSON level. Required, like every other config field. */
    public static final MapCodec<EyeAppearance> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            EyeColor.CODEC.fieldOf("corneaColors").forGetter(EyeAppearance::cornea),
            EyeColor.CODEC.fieldOf("irisColors").forGetter(EyeAppearance::iris),
            Codec.BOOL.fieldOf("glows").forGetter(EyeAppearance::glow)
    ).apply(inst, EyeAppearance::new));

    public static final Codec<EyeAppearance> CODEC = MAP_CODEC.codec();

    /** This appearance with the override's present fields layered on top — the single merge op. */
    public EyeAppearance overlay(AppearanceOverride o) {
        return new EyeAppearance(
                o.cornea().orElse(cornea),
                o.iris().orElse(iris),
                o.glow().orElse(glow));
    }

    /** A fully-populated override carrying this whole appearance (used when harvesting onto an item). */
    public AppearanceOverride toOverride() {
        return AppearanceOverride.EMPTY.withCorneaColor(cornea).withIrisColor(iris).withGlow(glow);
    }
}
