package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.picker.PickerExportService;
import com.github.crittscott.somegoogly.picker.PickerGate;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_ADULT;
import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_BABY;

/**
 * Client → server: "write this authored eye config for {@code entityTypeId} into the world datapack
 * and reload" — the wire half of {@code /sg export}. The config travels as codec-encoded NBT (the
 * same {@code RuntimeConfig.CODEC} the sync packet uses in the other direction); semantic config validation,
 * path construction, the export cooldown, the file write, and the {@code /reload} live in
 * {@link PickerExportService}. It requires {@link PickerGate#authorized}.
 *
 * <p>The decoder requires an adult or baby age and a config compound. Server-dependent validation stays in
 * the export service and reports refusals to the author. The client checks {@link #fitsServerbound()} before sending.
 */
public class PickerExportPacket implements Payload {

    /** What a loader adds to the encoded data on the wire: Forge's channel writes a one-byte message index first. */
    private static final int LOADER_FRAMING_BYTES = 1;

    /** Wire bound on the age string; comfortably above the longest age name. */
    private static final int MAX_AGE_LENGTH = 16;

    /** Vanilla's cap on a serverbound custom payload's encoded data. */
    public static final int MAX_SERVERBOUND_BYTES = 32767;

    public static final ResourceLocation ID = SomeGooglyCommon.id("picker_export");

    private final CompoundTag configNbt;
    private final ResourceLocation typeId;
    private final String age;

    public PickerExportPacket(ResourceLocation typeId, String age, CompoundTag configNbt) {
        this.typeId = typeId;
        this.age = age;
        this.configNbt = configNbt;
    }

    /** Decode and validate a received payload. */
    public static PickerExportPacket read(FriendlyByteBuf buffer) {
        ResourceLocation typeId = buffer.readResourceLocation();
        String age = buffer.readUtf(MAX_AGE_LENGTH);
        if (!AGE_ADULT.equals(age) && !AGE_BABY.equals(age)) {
            throw new DecoderException("Invalid picker export age");
        }
        CompoundTag configNbt = buffer.readNbt();
        if (configNbt == null) {
            throw new DecoderException("Picker export packet has no config");
        }
        return new PickerExportPacket(typeId, age, configNbt);
    }

    /** Whether this packet's encoded data and loader framing fit under {@link #MAX_SERVERBOUND_BYTES}. */
    public boolean fitsServerbound() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            write(buffer);
            return buffer.readableBytes() + LOADER_FRAMING_BYTES <= MAX_SERVERBOUND_BYTES;
        } finally {
            buffer.release();
        }
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
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
        PickerGate.tell(sender, result);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
