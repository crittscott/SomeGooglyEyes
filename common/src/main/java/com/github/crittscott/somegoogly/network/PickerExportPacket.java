package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.picker.PickerExportService;
import com.github.crittscott.somegoogly.picker.PickerGate;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Client → server: "write this authored eye config for {@code entityTypeId} into the world datapack
 * and reload" — the wire half of {@code /sg export}. The config travels as codec-encoded NBT (the
 * same {@code RuntimeConfig.CODEC} the sync packet uses in the other direction); all validation,
 * path construction, the 10-second cooldown, the file write, and the {@code /reload} live in
 * {@link PickerExportService}. It requires {@link PickerGate#authorized}.
 *
 * <p>The client refuses to send a packet that fails {@link #fitsServerbound()}, so a malformed or
 * oversized packet only comes from a misbehaving client and fails decoding the way vanilla's do.
 */
public class PickerExportPacket implements CustomPacketPayload {

    /** Wire bound on the age string; comfortably above the longest age name. */
    private static final int MAX_AGE_LENGTH = 16;

    /** Vanilla's cap on a serverbound custom payload's encoded data. */
    public static final int MAX_SERVERBOUND_BYTES = 32767;

    public static final CustomPacketPayload.Type<PickerExportPacket> TYPE =
            new CustomPacketPayload.Type<>(NetworkHandler.PICKER_EXPORT);
    public static final StreamCodec<RegistryFriendlyByteBuf, PickerExportPacket> STREAM_CODEC =
            StreamCodec.ofMember(PickerExportPacket::write, PickerExportPacket::new);

    private final CompoundTag configNbt;
    private final ResourceLocation typeId;
    private final String age;

    public PickerExportPacket(ResourceLocation typeId, String age, CompoundTag configNbt) {
        this.typeId = typeId;
        this.age = age;
        this.configNbt = configNbt;
    }

    private PickerExportPacket(FriendlyByteBuf buffer) {
        this.typeId = buffer.readResourceLocation();
        this.age = buffer.readUtf(MAX_AGE_LENGTH);
        CompoundTag configNbt = buffer.readNbt();
        if (configNbt == null) {
            throw new DecoderException("Picker export packet has no config");
        }
        this.configNbt = configNbt;
    }

    /** Whether this packet's encoded data fits under {@link #MAX_SERVERBOUND_BYTES}. */
    public boolean fitsServerbound() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            write(buffer);
            return buffer.readableBytes() <= MAX_SERVERBOUND_BYTES;
        } finally {
            buffer.release();
        }
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(typeId);
        buffer.writeUtf(age, MAX_AGE_LENGTH);
        buffer.writeNbt(configNbt);
    }

    /** Export the packet's definition for an authorized sender and report the result to them. */
    public static void handle(PickerExportPacket packet, ServerPlayer sender) {
        if (!PickerGate.authorized(sender)) {
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
