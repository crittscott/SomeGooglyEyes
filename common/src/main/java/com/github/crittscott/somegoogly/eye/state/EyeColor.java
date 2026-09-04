package com.github.crittscott.somegoogly.eye.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.FastColor;

import java.util.List;

/**
 * One RGB color, each channel 0..1. The single color representation across the mod — config,
 * item/entity override, and the renderer all use this.
 *
 * <p>Serialized as a {@code [r, g, b]} list, the datapack JSON's color layout.
 */
public record EyeColor(float r, float g, float b) {

    public static final EyeColor BLACK = new EyeColor(0F, 0F, 0F);

    public static final Codec<EyeColor> CODEC = Codec.FLOAT.listOf().comapFlatMap(
            list -> list.size() == 3
                    ? DataResult.success(new EyeColor(list.get(0), list.get(1), list.get(2)))
                    : DataResult.error(() -> "Expected 3 color channels, got " + list.size()),
            color -> List.of(color.r, color.g, color.b));

    /** Three raw floats; validation is the caller's ({@link #isValid()}). */
    public static final StreamCodec<ByteBuf, EyeColor> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, EyeColor::r,
            ByteBufCodecs.FLOAT, EyeColor::g,
            ByteBufCodecs.FLOAT, EyeColor::b,
            EyeColor::new);

    public static final EyeColor WHITE = new EyeColor(1F, 1F, 1F);

    private static int channel(float v) {
        int i = Math.round(v * 255F);
        return i < 0 ? 0 : Math.min(i, 255);
    }

    public static EyeColor of(float[] rgb) {
        return new EyeColor(rgb[0], rgb[1], rgb[2]);
    }

    public static EyeColor fromRgb24(int rgb) {
        return new EyeColor(
                FastColor.ARGB32.red(rgb) / 255.0F,
                FastColor.ARGB32.green(rgb) / 255.0F,
                FastColor.ARGB32.blue(rgb) / 255.0F);
    }

    public boolean isValid() {
        return validChannel(r) && validChannel(g) && validChannel(b);
    }

    private static boolean validChannel(float value) {
        return Float.isFinite(value) && value >= 0.0F && value <= 1.0F;
    }

    /** The renderer/model APIs take a {@code float[3]}. */
    public float[] toArray() {
        return new float[]{r, g, b};
    }

    /** Pack to {@code 0xRRGGBB} for hex display (tooltips); alpha byte is left zero. */
    public int toRgb24() {
        return FastColor.ARGB32.color(0, channel(r), channel(g), channel(b));
    }

    /** {@code RRGGBB} — six uppercase hex digits, no leading {@code #}; display templates add their own. */
    public String toHex() {
        return String.format("%06X", toRgb24());
    }
}
