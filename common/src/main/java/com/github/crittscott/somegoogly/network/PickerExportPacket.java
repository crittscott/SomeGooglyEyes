package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.picker.PickerExportService;
import com.github.crittscott.somegoogly.picker.PickerPermissions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.UUID;

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
    private final String age;

    public PickerExportPacket(ResourceLocation typeId, String age, @Nullable CompoundTag configNbt) {
        this.typeId = typeId;
        this.age = age;
        this.configNbt = configNbt;
    }

    public static PickerExportPacket decode(FriendlyByteBuf buffer) {
        ResourceLocation typeId = buffer.readResourceLocation();
        String age = buffer.readUtf(16);
        CompoundTag configNbt;
        try {
            Tag tag = buffer.readNbt(NbtAccounter.create(PickerExportService.MAX_CONFIG_BYTES));
            configNbt = tag instanceof CompoundTag compound ? compound : null;
        } catch (RuntimeException oversized) {
            // Quota exceeded mid-read: consume the rest (nothing follows the tag) and let the
            // service reject the null payload with feedback instead of the decode killing the connection.
            buffer.readerIndex(buffer.writerIndex());
            configNbt = null;
        }
        return new PickerExportPacket(typeId, age, configNbt);
    }

    public static void encode(PickerExportPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.typeId);
        buffer.writeUtf(packet.age, 16);
        buffer.writeNbt(packet.configNbt);
    }

    public static void handle(PickerExportPacket packet, NetworkTransport.Context context) {
        context.queue(() -> {
            ServerPlayer sender = context.player();
            if (!PickerPermissions.creative(sender)) {
                return;
            }
            UUID playerId = sender.getUUID();
            Component result = PickerExportService.export(
                    sender.serverLevel().getServer(), playerId, packet.typeId, packet.age, packet.configNbt);
            sender.sendSystemMessage(Component.translatable("somegoogly.command.picker.feedback", result));
        });
    }
}
