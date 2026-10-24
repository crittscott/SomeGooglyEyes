package com.github.crittscott.somegoogly.eye.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ARGB;

import java.util.List;

/**
 * One RGB color, each channel 0..1. The single color representation across the mod — config,
 * item/entity override, and the renderer all use this.
 *
 * <p>Serialized as a {@code [r, g, b]} list, the datapack JSON's color layout. Both codecs reject a
 * channel outside 0..1 on decode, so every decoded color is in range.
 */
public record EyeColor(float r, float g, float b) {

    public static final EyeColor BLACK = new EyeColor(0F, 0F, 0F);

    public static final Codec<EyeColor> CODEC = Codec.FLOAT.listOf().comapFlatMap(
            list -> {
                if (list.size() != 3) {
                    return DataResult.error(() -> "Expected 3 color channels, got " + list.size());
                }
                EyeColor color = new EyeColor(list.get(0), list.get(1), list.get(2));
                return color.isValid()
                        ? DataResult.success(color)
                        : DataResult.error(() -> "Color channel outside 0..1: " + list);
            },
            color -> List.of(color.r, color.g, color.b));

    public static final StreamCodec<ByteBuf, EyeColor> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, EyeColor::r,
            ByteBufCodecs.FLOAT, EyeColor::g,
            ByteBufCodecs.FLOAT, EyeColor::b,
            EyeColor::new).map(color -> {
                if (!color.isValid()) {
                    throw new DecoderException("Color channel outside 0..1");
                }
                return color;
            }, color -> color);

    public static final EyeColor WHITE = new EyeColor(1F, 1F, 1F);

    private static int channel(float v) {
        int i = Math.round(v * 255F);
        return i < 0 ? 0 : Math.min(i, 255);
    }

    /** A color from a three-element {@code [r, g, b]} array. */
    public static EyeColor of(float[] rgb) {
        return new EyeColor(rgb[0], rgb[1], rgb[2]);
    }

    /** A color from a packed {@code 0xRRGGBB} integer. */
    public static EyeColor fromRgb24(int rgb) {
        return new EyeColor(
                ARGB.red(rgb) / 255.0F,
                ARGB.green(rgb) / 255.0F,
                ARGB.blue(rgb) / 255.0F);
    }

    /** Whether every channel is within {@code [0, 1]}. */
    public boolean isValid() {
        return channelInRange(r) && channelInRange(g) && channelInRange(b);
    }

    private static boolean channelInRange(float value) {
        return value >= 0.0F && value <= 1.0F;
    }

    /** The renderer/model APIs take a {@code float[3]}. */
    public float[] toArray() {
        return new float[]{r, g, b};
    }

    /** Pack to {@code 0xRRGGBB} for hex display (tooltips); alpha byte is left zero. */
    public int toRgb24() {
        return ARGB.color(0, channel(r), channel(g), channel(b));
    }

    /** Pack to opaque {@code 0xFFRRGGBB} for APIs that multiply the alpha channel. */
    public int toOpaqueArgb32() {
        return ARGB.color(255, channel(r), channel(g), channel(b));
    }

    /** {@code RRGGBB} — six uppercase hex digits, no leading {@code #}; display templates add their own. */
    public String toHex() {
        return String.format("%06X", toRgb24());
    }
}
