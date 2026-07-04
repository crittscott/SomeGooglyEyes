package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.picker.PickerExportService;
import com.github.crittscott.somegoogly.picker.PickerPermissions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client → server: "write this authored eye config for {@code entityTypeId} into the world datapack
 * and reload" — the wire half of {@code /sg export}. The config travels as codec-encoded NBT (the
 * same {@code RuntimeConfig.CODEC} the sync packet uses in the other direction); all validation,
 * path construction, the 10-second cooldown, the file write, and the {@code /reload} live in
 * {@link PickerExportService}, creative-gated ({@link PickerPermissions}).
 *
 * <p>The NBT is read under a {@link PickerExportService#MAX_CONFIG_BYTES} quota — a legitimate config
 * is a few KiB — so an oversized payload decodes to {@code null} (the service rejects it with
 * feedback) instead of allocating unbounded memory.
 */
public class PickerExportPacket {

    @Nullable
    private final CompoundTag configNbt;
    private final ResourceLocation typeId;

    public PickerExportPacket(ResourceLocation typeId, @Nullable CompoundTag configNbt) {
        this.typeId = typeId;
        this.configNbt = configNbt;
    }

    public static PickerExportPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation typeId = buffer.readResourceLocation();
        CompoundTag configNbt;
        try {
            configNbt = buffer.readNbt(new NbtAccounter(PickerExportService.MAX_CONFIG_BYTES));
        } catch (RuntimeException oversized) {
            // Quota exceeded mid-read: consume the rest (nothing follows the tag) and let the
            // service reject the null payload with feedback instead of the decode killing the connection.
            buffer.readerIndex(buffer.writerIndex());
            configNbt = null;
        }
        return new PickerExportPacket(typeId, configNbt);
    }

    public static void encode(PickerExportPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.typeId);
        buffer.writeNbt(packet.configNbt);
    }

    public static void handle(PickerExportPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (!PickerPermissions.creative(sender)) {
                return;
            }
            UUID playerId = sender.getUUID();
            String result = PickerExportService.export(
                    sender.serverLevel().getServer(), playerId, packet.typeId, packet.configNbt);
            sender.sendSystemMessage(Component.literal("[Googly] " + result));
        });
        context.setPacketHandled(true);
    }
}
