package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo.RuntimeConfigSet;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Server → client sync of the version/age-selected eye geometry configs. Sent on player login and
 * on {@code /reload} (see {@code ServerEventHandler#onDatapackSync}). Custom datapack data isn't
 * auto-synced, so we carry it ourselves; each entity's selected config set travels as JSON.
 */
public class EyeConfigSyncPacket {

    private static final Gson GSON = new Gson();

    private final Map<ResourceLocation, RuntimeConfigSet> configs;

    public EyeConfigSyncPacket(Map<ResourceLocation, RuntimeConfigSet> configs) {
        this.configs = configs;
    }

    public static void encode(EyeConfigSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.configs.size());
        for (Map.Entry<ResourceLocation, RuntimeConfigSet> entry : packet.configs.entrySet()) {
            buffer.writeResourceLocation(entry.getKey());
            JsonElement json = RuntimeConfigSet.CODEC.encodeStart(JsonOps.INSTANCE, entry.getValue())
                    .result().orElseGet(JsonObject::new);
            buffer.writeUtf(GSON.toJson(json));
        }
    }

    public static EyeConfigSyncPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        // Don't pre-size from the wire count: an oversized value would force a large table allocation
        // before any real data is read. Let the map grow as entries actually arrive.
        Map<ResourceLocation, RuntimeConfigSet> configs = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            String json = buffer.readUtf();
            // Skip a single malformed entry (e.g. schema drift between mod versions) rather than
            // letting it abort the whole sync — which would surface as a disconnect. The id and JSON
            // are always read first so the buffer stays aligned for the next entry.
            try {
                JsonElement element = GSON.fromJson(json, JsonElement.class);
                configs.put(id, RuntimeConfigSet.CODEC.parse(JsonOps.INSTANCE, element).result().orElseThrow());
            } catch (Exception e) {
                SomeGoogly.LOGGER.error("Skipping malformed synced eye config for {}", id, e);
            }
        }
        return new EyeConfigSyncPacket(configs);
    }

    public static void handle(EyeConfigSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                // Client-only: apply into the client store (and invalidate caches). Guarded so the
                // dedicated server never class-loads ClientEyeConfigs via this path.
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    ClientEyeConfigs.replaceAll(packet.configs);
                    // Drop trackers too: a config change can resize per-head eye arrays.
                    if (SomeGoogly.clientEventHandler != null) {
                        SomeGoogly.clientEventHandler.clearTrackers();
                    }
                })
        );
        context.setPacketHandled(true);
    }
}
