package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.config.EyeConfigLimits;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.mojang.serialization.DataResult;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Server-to-client synchronization of the complete resolved eye-definition set. */
public class EyeConfigSyncPacket implements CustomPacketPayload {

    public static final int MAX_PAYLOAD_BYTES = 900 * 1024;
    public static final CustomPacketPayload.Type<EyeConfigSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(NetworkHandler.EYE_CONFIG);
    public static final StreamCodec<RegistryFriendlyByteBuf, EyeConfigSyncPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public EyeConfigSyncPacket decode(RegistryFriendlyByteBuf buffer) {
                    return EyeConfigSyncPacket.decode(buffer);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, EyeConfigSyncPacket packet) {
                    EyeConfigSyncPacket.encode(packet, buffer);
                }
            };

    private final Map<ResourceLocation, RuntimeConfigSet> configs;

    public EyeConfigSyncPacket(Map<ResourceLocation, RuntimeConfigSet> configs) {
        this.configs = configs;
    }

    public static EyeConfigSyncPacket decode(FriendlyByteBuf buffer) {
        if (buffer.readableBytes() > MAX_PAYLOAD_BYTES) {
            throw new DecoderException("Eye config sync payload exceeds network limit");
        }
        int start = buffer.readerIndex();
        int size = buffer.readVarInt();
        if (size < 0 || size > EyeConfigLimits.MAX_CONFIGS_PER_SYNC) {
            throw new DecoderException("Eye config count exceeds network limit: " + size);
        }
        Map<ResourceLocation, RuntimeConfigSet> configs = new HashMap<>();
        int wireEyes = 0;
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            if (configs.containsKey(id)) {
                throw new DecoderException("Duplicate synced eye config for " + id);
            }
            CompoundTag tag = buffer.readNbt();
            if (tag == null) {
                throw new DecoderException("Missing synced eye config for " + id);
            }
            EyeConfigLimits.WireValidation validation = EyeConfigLimits.validateWireConfigSet(tag);
            if (validation.error() != null) {
                throw new DecoderException("Unsafe synced eye config for " + id + ": " + validation.error());
            }
            wireEyes += validation.eyes();
            if (wireEyes > EyeConfigLimits.MAX_TOTAL_EYES_PER_SYNC) {
                throw new DecoderException("Synced eye config total exceeds network limit");
            }
            RuntimeConfigSet decoded;
            try {
                decoded = RuntimeConfigSet.CODEC.parse(NbtOps.INSTANCE, tag).result().orElseThrow();
            } catch (Exception e) {
                throw new DecoderException("Malformed synced eye config for " + id, e);
            }
            configs.put(id, decoded);
        }
        if (buffer.readerIndex() - start > MAX_PAYLOAD_BYTES) {
            throw new DecoderException("Eye config sync payload exceeds network limit");
        }
        String error = EyeConfigLimits.validateSync(configs);
        if (error != null) {
            throw new DecoderException("Unsafe synced eye config: " + error);
        }
        return new EyeConfigSyncPacket(configs);
    }

    public static void encode(EyeConfigSyncPacket packet, FriendlyByteBuf buffer) {
        String error = EyeConfigLimits.validateSync(packet.configs);
        if (error != null) {
            throw new EncoderException("Unsafe eye config sync: " + error);
        }
        int start = buffer.writerIndex();
        buffer.writeVarInt(packet.configs.size());
        for (Map.Entry<ResourceLocation, RuntimeConfigSet> entry : packet.configs.entrySet()) {
            buffer.writeResourceLocation(entry.getKey());
            DataResult<Tag> encoded = RuntimeConfigSet.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue());
            Tag tag = encoded.result().orElseThrow(() -> new EncoderException(
                    "Could not encode synced eye config for " + entry.getKey()));
            if (!(tag instanceof CompoundTag compound)) {
                throw new EncoderException("Synced eye config did not encode as a compound for " + entry.getKey());
            }
            buffer.writeNbt(compound);
        }
        int written = buffer.writerIndex() - start;
        if (written > MAX_PAYLOAD_BYTES) {
            throw new EncoderException("Eye config sync payload is " + written
                    + " bytes, exceeding the safe " + MAX_PAYLOAD_BYTES + "-byte limit");
        }
    }

    public Map<ResourceLocation, RuntimeConfigSet> configs() {
        return configs;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
