package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.head.HeadInfo.RuntimeConfigSet;
import com.google.gson.Gson;
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
            buffer.writeUtf(GSON.toJson(entry.getValue()));
        }
    }

    public static EyeConfigSyncPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Map<ResourceLocation, RuntimeConfigSet> configs = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            RuntimeConfigSet config = GSON.fromJson(buffer.readUtf(), RuntimeConfigSet.class);
            configs.put(id, config);
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
