package com.github.crittscott.somegoogly.network;

import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_ADULT;
import static com.github.crittscott.somegoogly.config.EyeConfigModel.AGE_BABY;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.picker.PickerExportService;
import com.github.crittscott.somegoogly.picker.PickerGate;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Client → server: "write this authored eye config for {@code entityTypeId} into the world datapack
 * and reload" — the wire half of {@code /sg export}. The config travels as codec-encoded NBT (the
 * same {@code RuntimeConfig.CODEC} the sync packet uses in the other direction); semantic config
 * validation, path construction, the export cooldown, the file write, and the {@code /reload} live
 * in {@link PickerExportService}. It requires {@link PickerGate#authorized}.
 *
 * <p>The decoder requires an adult or baby age and a config compound. Server-dependent validation
 * stays in the export service and reports refusals to the author. The client checks {@link
 * #fitsServerbound()} before sending.
 */
public class PickerExportPacket implements CustomPacketPayload {

    public static final ResourceLocation ID = SomeGooglyCommon.id("picker_export");

    /** Wire bound on the age string; comfortably above the longest age name. */
    private static final int MAX_AGE_LENGTH = 16;

    /** Vanilla's cap on a serverbound custom payload's encoded data. */
    public static final int MAX_SERVERBOUND_BYTES = 32767;

    public static final CustomPacketPayload.Type<PickerExportPacket> TYPE =
            new CustomPacketPayload.Type<>(ID);

    /**
     * The payload over a plain buffer, which {@link #fitsServerbound()} can measure without
     * registries.
     */
    private static final StreamCodec<ByteBuf, PickerExportPacket> WIRE_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    packet -> packet.typeId,
                    ByteBufCodecs.stringUtf8(MAX_AGE_LENGTH)
                            .map(PickerExportPacket::checkedAge, age -> age),
                    packet -> packet.age,
                    ByteBufCodecs.COMPOUND_TAG,
                    packet -> packet.configNbt,
                    PickerExportPacket::new);

    /** Declared after {@link #WIRE_CODEC}, which it reads. */
    public static final StreamCodec<RegistryFriendlyByteBuf, PickerExportPacket> STREAM_CODEC =
            WIRE_CODEC.cast();

    private final String age;
    private final CompoundTag configNbt;
    private final ResourceLocation typeId;

    public PickerExportPacket(ResourceLocation typeId, String age, CompoundTag configNbt) {
        this.typeId = typeId;
        this.age = age;
        this.configNbt = configNbt;
    }

    /** {@code age}, if it is an adult or baby age. */
    private static String checkedAge(String age) {
        if (!AGE_ADULT.equals(age) && !AGE_BABY.equals(age)) {
            throw new DecoderException("Invalid picker export age");
        }
        return age;
    }

    /** Whether this packet's encoded data fits under {@link #MAX_SERVERBOUND_BYTES}. */
    public boolean fitsServerbound() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            WIRE_CODEC.encode(buffer, this);
            return buffer.readableBytes() <= MAX_SERVERBOUND_BYTES;
        } finally {
            buffer.release();
        }
    }

    /** Export the packet's definition for an authorized sender and report the result to them. */
    public static void handle(PickerExportPacket packet, ServerPlayer sender) {
        if (!PickerGate.authorized(sender)) {
            return;
        }
        UUID playerId = sender.getUUID();
        Component result =
                PickerExportService.export(
                        sender.serverLevel().getServer(),
                        playerId,
                        packet.typeId,
                        packet.age,
                        packet.configNbt);
        PickerGate.tell(sender, result);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
