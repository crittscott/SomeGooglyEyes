package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfigSet;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Server → client sync of the version/age-selected eye geometry configs. Sent on player login and
 * on {@code /reload} (see {@code ServerEventHandler#onDatapackSync}). Custom datapack data isn't
 * auto-synced, so we carry it ourselves; each entity's selected config set travels as binary NBT
 * (the codec output via {@link NbtOps}), roughly half the size of a JSON-string encoding — this
 * packet goes to every player at login, so wire size matters.
 *
 * <p>The whole packet must fit vanilla's 1 MiB clientbound custom-payload cap or every client is
 * disconnected at login; {@link #encode} logs the encoded size and warns when an authored config set
 * approaches that ceiling, so the failure is diagnosable rather than a mystery kick.
 */
public class EyeConfigSyncPacket {

    // Warn while there is still headroom under the 1 MiB (1,048,576-byte) clientbound payload cap.
    private static final int PAYLOAD_WARN_BYTES = 900 * 1024;

    private final Map<ResourceLocation, RuntimeConfigSet> configs;

    public EyeConfigSyncPacket(Map<ResourceLocation, RuntimeConfigSet> configs) {
        this.configs = configs;
    }

    public static EyeConfigSyncPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        // Don't pre-size from the wire count: an oversized value would force a large table allocation
        // before any real data is read. Let the map grow as entries actually arrive.
        Map<ResourceLocation, RuntimeConfigSet> configs = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            CompoundTag tag = buffer.readNbt();
            // Skip a single entry that fails to decode rather than letting it abort the whole sync —
            // which would surface as a disconnect. (The protocol version gates cross-build wire
            // compatibility, so this guards a bug, not version skew.) The id and NBT are always read
            // first so the buffer stays aligned for the next entry.
            try {
                configs.put(id, RuntimeConfigSet.CODEC.parse(NbtOps.INSTANCE, tag).result().orElseThrow());
            } catch (Exception e) {
                SomeGooglyCommon.LOGGER.error("Skipping malformed synced eye config for {}", id, e);
            }
        }
        return new EyeConfigSyncPacket(configs);
    }

    public static void encode(EyeConfigSyncPacket packet, FriendlyByteBuf buffer) {
        int start = buffer.writerIndex();
        buffer.writeVarInt(packet.configs.size());
        for (Map.Entry<ResourceLocation, RuntimeConfigSet> entry : packet.configs.entrySet()) {
            buffer.writeResourceLocation(entry.getKey());
            DataResult<Tag> encoded = RuntimeConfigSet.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue());
            Tag tag = encoded.result().orElseGet(CompoundTag::new);
            buffer.writeNbt(tag instanceof CompoundTag compound ? compound : new CompoundTag());
        }
        int written = buffer.writerIndex() - start;
        if (written > PAYLOAD_WARN_BYTES) {
            SomeGooglyCommon.LOGGER.warn(
                    "Eye config sync payload is {} bytes for {} entities — nearing the 1 MiB packet cap; "
                            + "exceeding it will disconnect every client at login. Trim the authored eye configs.",
                    written, packet.configs.size());
        }
    }

    public Map<ResourceLocation, RuntimeConfigSet> configs() {
        return configs;
    }
}
