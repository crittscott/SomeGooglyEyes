package com.github.crittscott.somegoogly.eye.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.handler.codec.DecoderException;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.FastColor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * One RGB color, each channel 0..1. The single color representation across the mod — config,
 * item/entity override, and the renderer all use this.
 *
 * <p>Serialized as a {@code [r, g, b]} list, the datapack JSON's color layout. The codec, the NBT form
 * ({@link #fromNbt}), and the network form ({@link #read}) all reject a channel outside 0..1 on decode, so
 * every decoded color is in range.
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

    public static final EyeColor WHITE = new EyeColor(1F, 1F, 1F);

    private static int channel(float v) {
        return Math.round(v * 255F);
    }

    /** A color from a three-element {@code [r, g, b]} array. */
    public static EyeColor of(float[] rgb) {
        return new EyeColor(rgb[0], rgb[1], rgb[2]);
    }

    /** The {@link #toNbt} form, or nothing unless {@code tag} is a list of three in-range numbers of any type. */
    public static Optional<EyeColor> fromNbt(@Nullable Tag tag) {
        if (!(tag instanceof CollectionTag<?> list) || list.size() != 3) {
            return Optional.empty();
        }
        float[] rgb = new float[3];
        for (int i = 0; i < rgb.length; i++) {
            if (!(list.get(i) instanceof NumericTag channel)) {
                return Optional.empty();
            }
            rgb[i] = channel.getAsFloat();
        }
        EyeColor color = of(rgb);
        return color.isValid() ? Optional.of(color) : Optional.empty();
    }

    /** A color from a packed {@code 0xRRGGBB} integer. */
    public static EyeColor fromRgb24(int rgb) {
        return new EyeColor(
                FastColor.ARGB32.red(rgb) / 255.0F,
                FastColor.ARGB32.green(rgb) / 255.0F,
                FastColor.ARGB32.blue(rgb) / 255.0F);
    }

    /** Read the network form written by {@link #write}, rejecting a channel outside 0..1. */
    public static EyeColor read(FriendlyByteBuf buffer) {
        EyeColor color = new EyeColor(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        if (!color.isValid()) {
            throw new DecoderException("Color channel outside 0..1");
        }
        return color;
    }

    /** Write the network form: three floats. */
    public void write(FriendlyByteBuf buffer) {
        buffer.writeFloat(r);
        buffer.writeFloat(g);
        buffer.writeFloat(b);
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

    /** The NBT form: a list of three floats. */
    public ListTag toNbt() {
        ListTag list = new ListTag();
        list.add(FloatTag.valueOf(r));
        list.add(FloatTag.valueOf(g));
        list.add(FloatTag.valueOf(b));
        return list;
    }

    /** Pack to {@code 0xRRGGBB} for hex display (tooltips); alpha byte is left zero. */
    public int toRgb24() {
        return FastColor.ARGB32.color(0, channel(r), channel(g), channel(b));
    }

    /** Pack to opaque {@code 0xFFRRGGBB} for APIs that multiply the alpha channel. */
    public int toOpaqueArgb32() {
        return FastColor.ARGB32.color(255, channel(r), channel(g), channel(b));
    }

    /** {@code RRGGBB} — six uppercase hex digits, no leading {@code #}; display templates add their own. */
    public String toHex() {
        return String.format("%06X", toRgb24());
    }
}
