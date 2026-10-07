package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.config.EyeConfigLimits;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Server-to-client synchronization of the complete resolved eye-definition set, plus the current
 * {@code googlyEyesEnabled} master switch. The server builds it from the installed set and that set's
 * precomputed NBT ({@link com.github.crittscott.somegoogly.config.ServerEyeConfigs#encoded}), which was
 * validated when installed, so writing it to each connection only copies tags. The client decodes and
 * re-validates everything it receives.
 */
public class EyeConfigSyncPacket implements CustomPacketPayload {

    public static final int MAX_PAYLOAD_BYTES = 900 * 1024;
    public static final CustomPacketPayload.Type<EyeConfigSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(NetworkHandler.EYE_CONFIG);
    public static final StreamCodec<RegistryFriendlyByteBuf, EyeConfigSyncPacket> STREAM_CODEC =
            StreamCodec.ofMember(EyeConfigSyncPacket::write, EyeConfigSyncPacket::new);

    private final Map<ResourceLocation, RuntimeConfigSet> configs;
    private final Map<ResourceLocation, CompoundTag> encoded;
    private final boolean googlyEyesEnabled;

    /** {@code encoded} must hold exactly {@code configs}, each encoded through {@link RuntimeConfigSet#CODEC}. */
    public EyeConfigSyncPacket(Map<ResourceLocation, RuntimeConfigSet> configs,
                               Map<ResourceLocation, CompoundTag> encoded, boolean googlyEyesEnabled) {
        this.configs = configs;
        this.encoded = encoded;
        this.googlyEyesEnabled = googlyEyesEnabled;
    }

    private EyeConfigSyncPacket(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() > MAX_PAYLOAD_BYTES) {
            throw new DecoderException("Eye config sync payload exceeds network limit");
        }
        int start = buffer.readerIndex();
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
            RuntimeConfigSet decoded;
            try {
                decoded = RuntimeConfigSet.CODEC.parse(NbtOps.INSTANCE, tag).result().orElseThrow();
            } catch (Exception e) {
                throw new DecoderException("Malformed synced eye config for " + id, e);
            }
            configs.put(id, decoded);
            encoded.put(id, tag);
        }
        if (buffer.readerIndex() - start > MAX_PAYLOAD_BYTES) {
            throw new DecoderException("Eye config sync payload exceeds network limit");
        }
        String error = EyeConfigLimits.validateSync(configs);
        if (error != null) {
            throw new DecoderException("Unsafe synced eye config: " + error);
        }
        this.configs = configs;
        this.encoded = encoded;
        this.googlyEyesEnabled = buffer.readBoolean();
    }

    private void write(FriendlyByteBuf buffer) {
        int start = buffer.writerIndex();
        buffer.writeVarInt(encoded.size());
        for (Map.Entry<ResourceLocation, CompoundTag> entry : encoded.entrySet()) {
            buffer.writeResourceLocation(entry.getKey());
            buffer.writeNbt(entry.getValue());
        }
        buffer.writeBoolean(googlyEyesEnabled);
        int written = buffer.writerIndex() - start;
        if (written > MAX_PAYLOAD_BYTES) {
            throw new EncoderException("Eye config sync payload is " + written
                    + " bytes, exceeding the safe " + MAX_PAYLOAD_BYTES + "-byte limit");
        }
    }

    public Map<ResourceLocation, RuntimeConfigSet> configs() {
        return configs;
    }

    public boolean googlyEyesEnabled() {
        return googlyEyesEnabled;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
