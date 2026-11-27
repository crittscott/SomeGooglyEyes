package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.EyeConfigLimits;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-to-client synchronization of the complete resolved eye-definition set, with the set's NBT
 * encoding. The server builds one per candidate set ({@link
 * com.github.crittscott.somegoogly.config.ServerEyeConfigs#packetFor}), checks it with {@link
 * #refusal}, and installs it, so construction serializes once and every joining or reloading
 * connection copies those bytes. The client decodes and re-validates everything it receives, and a
 * received packet keeps only the decoded definitions.
 */
public class EyeConfigSyncPacket implements CustomPacketPayload {

    public static final int MAX_PAYLOAD_BYTES = 900 * 1024;
    public static final Identifier ID = SomeGooglyCommon.id("eye_config");
    public static final CustomPacketPayload.Type<EyeConfigSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, EyeConfigSyncPacket> STREAM_CODEC =
            StreamCodec.ofMember(EyeConfigSyncPacket::write, EyeConfigSyncPacket::read);

    private final byte @Nullable [] body;
    private final Map<Identifier, RuntimeConfigSet> configs;
    private final @Nullable Map<Identifier, CompoundTag> encoded;

    /**
     * {@code encoded} must hold exactly {@code configs}, each encoded through {@link
     * RuntimeConfigSet#CODEC}.
     */
    public EyeConfigSyncPacket(
            Map<Identifier, RuntimeConfigSet> configs, Map<Identifier, CompoundTag> encoded) {
        this.configs = Map.copyOf(configs);
        this.encoded = Map.copyOf(encoded);
        this.body = body(encoded);
    }

    /** A received packet: the decoded definitions alone. */
    private EyeConfigSyncPacket(Map<Identifier, RuntimeConfigSet> configs) {
        this.configs = Map.copyOf(configs);
        this.encoded = null;
        this.body = null;
    }

    private static byte[] body(Map<Identifier, CompoundTag> encoded) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeVarInt(encoded.size());
            for (Map.Entry<Identifier, CompoundTag> entry : encoded.entrySet()) {
                buffer.writeIdentifier(entry.getKey());
                buffer.writeNbt(entry.getValue());
            }
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }

    /** The resolved eye definitions, by entity type; read-only, values included. */
    public Map<Identifier, RuntimeConfigSet> configs() {
        return configs;
    }

    /**
     * {@link #configs} encoded through {@link RuntimeConfigSet#CODEC}; read-only. Server-built
     * packets only.
     */
    public Map<Identifier, CompoundTag> encoded() {
        return encoded;
    }

    /** Decode and validate a received payload. */
    private static EyeConfigSyncPacket read(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() > MAX_PAYLOAD_BYTES) {
            throw new DecoderException("Eye config sync payload exceeds network limit");
        }
        int size = buffer.readVarInt();
        if (size < 0 || size > EyeConfigLimits.MAX_CONFIGS_PER_SYNC) {
            throw new DecoderException("Eye config count exceeds network limit: " + size);
        }
        Map<Identifier, RuntimeConfigSet> configs = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Identifier id = buffer.readIdentifier();
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
        return new EyeConfigSyncPacket(configs);
    }

    /**
     * Why clients would refuse this packet, or {@code null} if they accept it: the work its
     * definitions ask of a client, then its size. Shared by reload and picker export, so an export
     * is checked exactly as the reload it triggers will be. Server-built packets only.
     */
    public EyeConfigLimits.@Nullable Issue refusal() {
        EyeConfigLimits.Issue limits = EyeConfigLimits.validateSync(configs);
        if (limits != null) {
            return limits;
        }
        return body.length > MAX_PAYLOAD_BYTES
                ? new EyeConfigLimits.Issue(
                        "somegoogly.validation.sync_size",
                        List.of(body.length, MAX_PAYLOAD_BYTES),
                        "Eye config sync size "
                                + body.length
                                + " exceeds "
                                + MAX_PAYLOAD_BYTES
                                + " bytes")
                : null;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Server-built packets only; a received packet is never sent on. */
    private void write(FriendlyByteBuf buffer) {
        buffer.writeBytes(body);
    }
}
