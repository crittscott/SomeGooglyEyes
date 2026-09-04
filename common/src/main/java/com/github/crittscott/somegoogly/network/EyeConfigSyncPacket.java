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
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Server → client sync of the version/age-selected eye geometry configs. Sent on player login and
 * on {@code /reload} (see {@code ServerServices#syncEyeConfigs}). Custom datapack data isn't
 * auto-synced, so we carry it ourselves; each entity's selected config set travels as binary NBT
 * (the codec output via {@link NbtOps}), roughly half the size of a JSON-string encoding — this
 * packet goes to every player at login, so wire size matters.
 *
 * <p>The whole packet must fit vanilla's 1 MiB clientbound custom-payload cap or every client is
 * disconnected at login; {@link #encode} refuses an authored config set before it reaches that ceiling.
 */
public class EyeConfigSyncPacket {

    // Refuse the payload while there is still headroom under the 1 MiB clientbound payload cap.
    static final int MAX_PAYLOAD_BYTES = 900 * 1024;

    private final Map<ResourceLocation, RuntimeConfigSet> configs;
    private final long generation;

    public EyeConfigSyncPacket(long generation, Map<ResourceLocation, RuntimeConfigSet> configs) {
        this.generation = generation;
        this.configs = configs;
    }

    public static EyeConfigSyncPacket decode(FriendlyByteBuf buffer) {
        long generation = buffer.readLong();
        if (generation < 0) {
            throw new DecoderException("Negative eye config generation");
        }
        int size = buffer.readVarInt();
        if (size < 0 || size > EyeConfigLimits.MAX_CONFIGS_PER_SYNC) {
            throw new DecoderException("Eye config count exceeds protocol limit: " + size);
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
            EyeConfigLimits.WireValidation wireValidation = EyeConfigLimits.validateWireConfigSet(tag);
            if (wireValidation.error() != null) {
                throw new DecoderException("Unsafe synced eye config for " + id + ": "
                        + wireValidation.error());
            }
            wireEyes += wireValidation.eyes();
            if (wireEyes > EyeConfigLimits.MAX_TOTAL_EYES_PER_SYNC) {
                throw new DecoderException("Synced eye config total exceeds protocol limit");
            }
            RuntimeConfigSet decoded;
            try {
                decoded = RuntimeConfigSet.CODEC.parse(NbtOps.INSTANCE, tag).result().orElseThrow();
            } catch (Exception e) {
                throw new DecoderException("Malformed synced eye config for " + id, e);
            }
            configs.put(id, decoded);
        }
        String error = EyeConfigLimits.validateSync(configs);
        if (error != null) {
            throw new DecoderException("Unsafe synced eye config: " + error);
        }
        return new EyeConfigSyncPacket(generation, configs);
    }

    public static void encode(EyeConfigSyncPacket packet, FriendlyByteBuf buffer) {
        String error = EyeConfigLimits.validateSync(packet.configs);
        if (error != null) {
            throw new EncoderException("Unsafe eye config sync: " + error);
        }
        int start = buffer.writerIndex();
        buffer.writeLong(packet.generation);
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

    public long generation() {
        return generation;
    }
}
