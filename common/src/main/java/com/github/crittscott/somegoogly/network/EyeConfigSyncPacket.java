package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.EyeConfigLimits;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Nullable;

/**
 * Server-to-client synchronization of the complete resolved eye-definition set, with the set's NBT
 * encoding. The server builds one per candidate set ({@link
 * com.github.crittscott.somegoogly.config.ServerEyeConfigs#packetFor}), checks it with {@link
 * #refusal}, and installs it, so construction serializes and compresses once and every joining or
 * reloading connection copies those bytes. The client decodes and re-validates everything it
 * receives and keeps only the decoded definitions: it never re-sends the packet, so {@link
 * #encoded}, {@link #refusal}, and {@link #write} serve server-built packets only.
 */
public class EyeConfigSyncPacket implements Payload {

    public static final ResourceLocation ID = SomeGooglyCommon.id("eye_config");

    /** Compressed wire body; retains the existing transport budget on both loaders. */
    public static final int MAX_COMPRESSED_BYTES = 900 * 1024;

    /** Maximum inflated body, bounded before parsing NBT. */
    public static final int MAX_PAYLOAD_BYTES = 2 * 1024 * 1024;

    private final int bodySize;
    @Nullable private final byte[] compressed;
    private final Map<ResourceLocation, RuntimeConfigSet> configs;
    @Nullable private final Map<ResourceLocation, CompoundTag> encoded;

    /** A received packet: the decoded definitions alone. */
    private EyeConfigSyncPacket(Map<ResourceLocation, RuntimeConfigSet> configs) {
        this.configs = Map.copyOf(configs);
        this.encoded = null;
        this.bodySize = 0;
        this.compressed = null;
    }

    /**
     * {@code encoded} must hold exactly {@code configs}, each encoded through {@link
     * RuntimeConfigSet#CODEC}.
     */
    public EyeConfigSyncPacket(
            Map<ResourceLocation, RuntimeConfigSet> configs,
            Map<ResourceLocation, CompoundTag> encoded) {
        this(configs, encoded, body(encoded));
    }

    private EyeConfigSyncPacket(
            Map<ResourceLocation, RuntimeConfigSet> configs,
            Map<ResourceLocation, CompoundTag> encoded,
            byte[] body) {
        this.configs = Map.copyOf(configs);
        this.encoded = Map.copyOf(encoded);
        this.bodySize = body.length;
        this.compressed = compress(body);
    }

    private static byte[] body(Map<ResourceLocation, CompoundTag> encoded) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writeBody(buffer, encoded);
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }

    private static byte[] compress(byte[] bytes) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Could not compress eye config sync", e);
        }
        return output.toByteArray();
    }

    /** The resolved eye definitions, by entity type; read-only, values included. */
    public Map<ResourceLocation, RuntimeConfigSet> configs() {
        return configs;
    }

    /**
     * {@link #configs} encoded through {@link RuntimeConfigSet#CODEC}; read-only. Server-built
     * packets only.
     */
    // Only server-built packets expose encoded data; the receive-only constructor deliberately
    // omits it.
    @SuppressWarnings("NullAway")
    public Map<ResourceLocation, CompoundTag> encoded() {
        return encoded;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    /** Decode and validate a received payload. */
    public static EyeConfigSyncPacket read(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() > MAX_COMPRESSED_BYTES) {
            throw new DecoderException("Eye config sync payload exceeds network limit");
        }
        byte[] decoded;
        try (GZIPInputStream gzip =
                new GZIPInputStream(
                        new ByteBufInputStream(buffer.readSlice(buffer.readableBytes())))) {
            decoded = gzip.readNBytes(MAX_PAYLOAD_BYTES + 1);
        } catch (IOException e) {
            throw new DecoderException("Malformed compressed eye config sync", e);
        }
        if (decoded.length > MAX_PAYLOAD_BYTES) {
            throw new DecoderException("Decoded eye config sync exceeds network limit");
        }
        FriendlyByteBuf body = new FriendlyByteBuf(Unpooled.wrappedBuffer(decoded));
        try {
            return readBody(body);
        } finally {
            body.release();
        }
    }

    private static EyeConfigSyncPacket readBody(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > EyeConfigLimits.MAX_CONFIGS_PER_SYNC) {
            throw new DecoderException("Eye config count exceeds network limit: " + size);
        }
        Map<ResourceLocation, RuntimeConfigSet> configs = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            if (configs.containsKey(id)) {
                throw new DecoderException("Duplicate synced eye config for " + id);
            }
            CompoundTag tag = buffer.readNbt();
            if (tag == null) {
                throw new DecoderException("Missing synced eye config for " + id);
            }
            RuntimeConfigSet decoded =
                    RuntimeConfigSet.CODEC
                            .parse(NbtOps.INSTANCE, tag)
                            .result()
                            .orElseThrow(() ->
                                    new DecoderException("Malformed synced eye config for " + id));
            configs.put(id, decoded);
        }
        EyeConfigLimits.Issue error = EyeConfigLimits.validateSync(configs);
        if (error != null) {
            throw new DecoderException("Unsafe synced eye config: " + error.diagnostic());
        }
        if (buffer.isReadable()) {
            throw new DecoderException("Trailing eye config sync data");
        }
        return new EyeConfigSyncPacket(configs);
    }

    /**
     * Why clients would refuse this packet, or {@code null} if they accept it: the work its
     * definitions ask of a client, then its decoded and transmitted sizes. Shared by reload and
     * picker export, so an export is checked exactly as the reload it triggers will be.
     * Server-built packets only.
     */
    // Reload and export validate server-built packets, whose constructor computes compressed bytes.
    @SuppressWarnings("NullAway")
    @Nullable
    public EyeConfigLimits.Issue refusal() {
        EyeConfigLimits.Issue limits = EyeConfigLimits.validateSync(configs);
        if (limits != null) {
            return limits;
        }
        int size = bodySize;
        int limit = MAX_PAYLOAD_BYTES;
        if (size <= limit) {
            size = compressed.length;
            limit = MAX_COMPRESSED_BYTES;
        }
        return size > limit
                ? new EyeConfigLimits.Issue(
                        "somegoogly.validation.sync_size",
                        List.of(size, limit),
                        "Eye config sync size " + size + " exceeds " + limit + " bytes")
                : null;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBytes(compressed);
    }

    private static void writeBody(
            FriendlyByteBuf buffer, Map<ResourceLocation, CompoundTag> encoded) {
        buffer.writeVarInt(encoded.size());
        for (Map.Entry<ResourceLocation, CompoundTag> entry : encoded.entrySet()) {
            buffer.writeResourceLocation(entry.getKey());
            buffer.writeNbt(entry.getValue());
        }
    }
}
