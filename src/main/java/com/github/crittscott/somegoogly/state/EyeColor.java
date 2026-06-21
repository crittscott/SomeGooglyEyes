package com.github.crittscott.somegoogly.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.List;

/**
 * One RGB color, each channel 0..1. The single color representation across the mod — config,
 * item/entity override, and the renderer all use this, replacing the old triple of {@code double[3]}
 * (config JSON), packed {@code 0xRRGGBB} int (override NBT), and {@code float[3]} (renderer).
 *
 * <p>Serialized as the {@code [r, g, b]} list the datapack JSON already uses, so the existing eye
 * config files are unchanged.
 */
public record EyeColor(float r, float g, float b) {

    public static final EyeColor WHITE = new EyeColor(1F, 1F, 1F);
    public static final EyeColor BLACK = new EyeColor(0F, 0F, 0F);

    public static final Codec<EyeColor> CODEC = Codec.FLOAT.listOf().comapFlatMap(
            list -> list.size() == 3
                    ? DataResult.success(new EyeColor(list.get(0), list.get(1), list.get(2)))
                    : DataResult.error(() -> "Expected 3 color channels, got " + list.size()),
            color -> List.of(color.r, color.g, color.b));

    public static EyeColor of(float[] rgb) {
        return new EyeColor(rgb[0], rgb[1], rgb[2]);
    }

    /** The renderer/model APIs take a {@code float[3]}. */
    public float[] toArray() {
        return new float[]{r, g, b};
    }

    /** Pack to {@code 0xRRGGBB} for hex display (tooltips). */
    public int toRgb24() {
        return (channel(r) << 16) | (channel(g) << 8) | channel(b);
    }

    public EyeColor lerp(EyeColor to, float t) {
        return new EyeColor(r + (to.r - r) * t, g + (to.g - g) * t, b + (to.b - b) * t);
    }

    private static int channel(float v) {
        int i = Math.round(v * 255F);
        return i < 0 ? 0 : Math.min(i, 255);
    }
}
