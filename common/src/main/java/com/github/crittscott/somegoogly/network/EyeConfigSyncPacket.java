package com.github.crittscott.somegoogly.network;

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
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Server-to-client synchronization of the complete resolved eye-definition set, with the set's NBT encoding. The
 * server builds one per candidate set ({@link com.github.crittscott.somegoogly.config.ServerEyeConfigs#packetFor}),
 * checks it with {@link #refusal}, and installs it, so construction serializes once and every joining or
 * reloading connection copies those bytes. The client decodes and re-validates everything it receives.
 */
public class EyeConfigSyncPacket implements CustomPacketPayload {

    public static final int MAX_PAYLOAD_BYTES = 900 * 1024;
    public static final ResourceLocation ID = NetworkProtocol.id("eye_config");
    public static final CustomPacketPayload.Type<EyeConfigSyncPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, EyeConfigSyncPacket> STREAM_CODEC =
            StreamCodec.ofMember(EyeConfigSyncPacket::write, EyeConfigSyncPacket::read);

    private final byte[] body;
    private final Map<ResourceLocation, RuntimeConfigSet> configs;
    private final Map<ResourceLocation, CompoundTag> encoded;

    /** {@code encoded} must hold exactly {@code configs}, each encoded through {@link RuntimeConfigSet#CODEC}. */
    public EyeConfigSyncPacket(Map<ResourceLocation, RuntimeConfigSet> configs,
                               Map<ResourceLocation, CompoundTag> encoded) {
        this(configs, encoded, body(encoded));
    }

    private EyeConfigSyncPacket(Map<ResourceLocation, RuntimeConfigSet> configs,
                                Map<ResourceLocation, CompoundTag> encoded, byte[] body) {
        this.configs = Map.copyOf(configs);
        this.encoded = Map.copyOf(encoded);
        this.body = body;
    }

    private static byte[] body(Map<ResourceLocation, CompoundTag> encoded) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeVarInt(encoded.size());
            for (Map.Entry<ResourceLocation, CompoundTag> entry : encoded.entrySet()) {
                buffer.writeResourceLocation(entry.getKey());
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
    public Map<ResourceLocation, RuntimeConfigSet> configs() {
        return configs;
    }

    /** {@link #configs} encoded through {@link RuntimeConfigSet#CODEC}; read-only. */
    public Map<ResourceLocation, CompoundTag> encoded() {
        return encoded;
    }

    /** Decode and validate a received payload. */
    private static EyeConfigSyncPacket read(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() > MAX_PAYLOAD_BYTES) {
            throw new DecoderException("Eye config sync payload exceeds network limit");
        }
        byte[] body = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), body);
        int size = buffer.readVarInt();
        if (size < 0 || size > EyeConfigLimits.MAX_CONFIGS_PER_SYNC) {
            throw new DecoderException("Eye config count exceeds network limit: " + size);
        }
        Map<ResourceLocation, RuntimeConfigSet> configs = new HashMap<>();
        Map<ResourceLocation, CompoundTag> encoded = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            if (configs.containsKey(id)) {
                throw new DecoderException("Duplicate synced eye config for " + id);
            }
            CompoundTag tag = buffer.readNbt();
            if (tag == null) {
                throw new DecoderException("Missing synced eye config for " + id);
            }
            RuntimeConfigSet decoded = RuntimeConfigSet.CODEC.parse(NbtOps.INSTANCE, tag).result()
                    .orElseThrow(() -> new DecoderException("Malformed synced eye config for " + id));
            configs.put(id, decoded);
            encoded.put(id, tag);
        }
        EyeConfigLimits.Issue error = EyeConfigLimits.validateSync(configs);
        if (error != null) {
            throw new DecoderException("Unsafe synced eye config: " + error.diagnostic());
        }
        return new EyeConfigSyncPacket(configs, encoded, body);
    }

    /**
     * Why clients would refuse this packet, or {@code null} if they accept it: the work its definitions ask of a
     * client, then its size. Shared by reload and picker export, so an export is checked exactly as the reload it
     * triggers will be.
     */
    @Nullable
    public EyeConfigLimits.Issue refusal() {
        EyeConfigLimits.Issue limits = EyeConfigLimits.validateSync(configs);
        if (limits != null) {
            return limits;
        }
        return body.length > MAX_PAYLOAD_BYTES ? new EyeConfigLimits.Issue("somegoogly.validation.sync_size",
                List.of(body.length, MAX_PAYLOAD_BYTES),
                "Eye config sync size " + body.length + " exceeds " + MAX_PAYLOAD_BYTES + " bytes") : null;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeBytes(body);
    }
}
