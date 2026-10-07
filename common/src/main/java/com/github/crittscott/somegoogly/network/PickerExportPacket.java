package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.picker.PickerExportService;
import com.github.crittscott.somegoogly.picker.PickerGate;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Client → server: "write this authored eye config for {@code entityTypeId} into the world datapack
 * and reload" — the wire half of {@code /sg export}. The config travels as codec-encoded NBT (the
 * same {@code RuntimeConfig.CODEC} the sync packet uses in the other direction); all validation,
 * path construction, the 10-second cooldown, the file write, and the {@code /reload} live in
 * {@link PickerExportService}. Unlike the other picker verbs this one also requires permission
 * level 2: it forces a server-wide datapack reload, which vanilla reserves for operators.
 *
 * <p>The NBT is read under a {@link PickerExportService#MAX_CONFIG_BYTES} quota — a legitimate config
 * is a few KiB — so an oversized payload decodes to {@code null} (the service rejects it with
 * feedback) instead of allocating unbounded memory.
 */
public class PickerExportPacket implements CustomPacketPayload {

    /** Wire bound on the age string; comfortably above the longest age name. */
    private static final int MAX_AGE_LENGTH = 16;

    public static final CustomPacketPayload.Type<PickerExportPacket> TYPE =
            new CustomPacketPayload.Type<>(NetworkHandler.PICKER_EXPORT);
    public static final StreamCodec<RegistryFriendlyByteBuf, PickerExportPacket> STREAM_CODEC =
            StreamCodec.ofMember(PickerExportPacket::write, PickerExportPacket::new);

    @Nullable
    private final CompoundTag configNbt;
    private final ResourceLocation typeId;
    private final String age;

    public PickerExportPacket(ResourceLocation typeId, String age, @Nullable CompoundTag configNbt) {
        this.typeId = typeId;
        this.age = age;
        this.configNbt = configNbt;
    }

    private PickerExportPacket(FriendlyByteBuf buffer) {
        this.typeId = buffer.readResourceLocation();
        this.age = buffer.readUtf(MAX_AGE_LENGTH);
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
        this.configNbt = configNbt;
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(typeId);
        buffer.writeUtf(age, MAX_AGE_LENGTH);
        buffer.writeNbt(configNbt);
    }

    public static void handle(PickerExportPacket packet, ServerPlayer sender) {
        if (!PickerGate.creative(sender)) {
            return;
        }
        if (!sender.hasPermissions(Commands.LEVEL_GAMEMASTERS)) {
            sender.sendSystemMessage(Component.translatable("somegoogly.command.picker.feedback",
                    Component.translatable("somegoogly.command.picker.export_rejected_not_operator")));
            return;
        }
        UUID playerId = sender.getUUID();
        Component result = PickerExportService.export(
                sender.serverLevel().getServer(), playerId, packet.typeId, packet.age, packet.configNbt);
        sender.sendSystemMessage(Component.translatable("somegoogly.command.picker.feedback", result));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
