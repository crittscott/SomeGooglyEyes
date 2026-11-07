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
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-to-client synchronization of the complete resolved eye-definition set. The server builds it from
 * the installed set and that set's precomputed NBT ({@link com.github.crittscott.somegoogly.config.ServerEyeConfigs#encoded}), which was
 * validated when installed, so writing it to each connection only copies tags. The client decodes and
 * re-validates everything it receives.
 */
public class EyeConfigSyncPacket implements CustomPacketPayload {

    public static final int MAX_PAYLOAD_BYTES = 900 * 1024;
    public static final Identifier ID = NetworkProtocol.id("eye_config");
    public static final CustomPacketPayload.Type<EyeConfigSyncPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, EyeConfigSyncPacket> STREAM_CODEC =
            StreamCodec.ofMember(EyeConfigSyncPacket::write, EyeConfigSyncPacket::new);

    private final Map<Identifier, RuntimeConfigSet> configs;
    private final Map<Identifier, CompoundTag> encoded;

    /** {@code encoded} must hold exactly {@code configs}, each encoded through {@link RuntimeConfigSet#CODEC}. */
    public EyeConfigSyncPacket(Map<Identifier, RuntimeConfigSet> configs,
                               Map<Identifier, CompoundTag> encoded) {
        this.configs = configs;
        this.encoded = encoded;
    }

    private EyeConfigSyncPacket(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() > MAX_PAYLOAD_BYTES) {
            throw new DecoderException("Eye config sync payload exceeds network limit");
        }
        int size = buffer.readVarInt();
        if (size < 0 || size > EyeConfigLimits.MAX_CONFIGS_PER_SYNC) {
            throw new DecoderException("Eye config count exceeds network limit: " + size);
        }
        Map<Identifier, RuntimeConfigSet> configs = new HashMap<>();
        Map<Identifier, CompoundTag> encoded = new HashMap<>();
        for (int i = 0; i < size; i++) {
            Identifier id = buffer.readIdentifier();
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
        this.configs = configs;
        this.encoded = encoded;
    }

    /** The packet size of {@code encoded}. */
    public static int wireSize(Map<Identifier, CompoundTag> encoded) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writeBody(buffer, encoded);
            return buffer.writerIndex();
        } finally {
            buffer.release();
        }
    }

    private static void writeBody(FriendlyByteBuf buffer, Map<Identifier, CompoundTag> encoded) {
        buffer.writeVarInt(encoded.size());
        for (Map.Entry<Identifier, CompoundTag> entry : encoded.entrySet()) {
            buffer.writeIdentifier(entry.getKey());
            buffer.writeNbt(entry.getValue());
        }
    }

    private void write(FriendlyByteBuf buffer) {
        writeBody(buffer, encoded);
    }

    /** The resolved eye definitions, by entity type. */
    public Map<Identifier, RuntimeConfigSet> configs() {
        return configs;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
