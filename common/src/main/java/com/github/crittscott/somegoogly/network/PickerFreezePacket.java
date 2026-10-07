package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.picker.PickerFreezeService;
import com.github.crittscott.somegoogly.picker.PickerGate;
import com.github.crittscott.somegoogly.util.LookTarget;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Client → server: "freeze the mob I'm editing" (or release it). The freeze itself — NoAi capture,
 * restore, crash-proofing — is owned by {@link PickerFreezeService} on the server, so it works from a
 * remote client and survives that client disappearing; this packet only asks.
 *
 * <p>Freezing requires creative mode ({@link PickerGate}). <b>Unfreezing does not</b>: it only
 * releases the sender's own frozen mob, and gating it would strand a mob frozen if the player lost
 * creative mid-edit.
 */
public class PickerFreezePacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PickerFreezePacket> TYPE =
            new CustomPacketPayload.Type<>(NetworkHandler.PICKER_FREEZE);
    public static final StreamCodec<RegistryFriendlyByteBuf, PickerFreezePacket> STREAM_CODEC =
            StreamCodec.ofMember(PickerFreezePacket::write, PickerFreezePacket::new);

    /**
     * Server-side sanity bound on the freeze target's distance from the requester: the picker reach
     * ({@link LookTarget#DEFAULT_REACH}) plus slack for the gap between the client's raytrace hit on
     * the mob's box and the mob's position the server measures to. Keeps a creative client from
     * freezing an arbitrary mob in any loaded chunk by UUID.
     */
    private static final double FREEZE_DISTANCE_SLACK = 4.0;
    private static final double MAX_FREEZE_DISTANCE_SQ = (LookTarget.DEFAULT_REACH + FREEZE_DISTANCE_SLACK)
            * (LookTarget.DEFAULT_REACH + FREEZE_DISTANCE_SLACK);

    private final boolean freeze;
    @Nullable
    private final UUID mobId; // present iff freeze

    private PickerFreezePacket(boolean freeze, @Nullable UUID mobId) {
        this.freeze = freeze;
        this.mobId = mobId;
    }

    public static PickerFreezePacket freeze(UUID mobId) {
        return new PickerFreezePacket(true, mobId);
    }

    public static PickerFreezePacket unfreeze() {
        return new PickerFreezePacket(false, null);
    }

    private PickerFreezePacket(FriendlyByteBuf buffer) {
        this.freeze = buffer.readBoolean();
        this.mobId = freeze ? buffer.readUUID() : null;
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(freeze);
        if (freeze) {
            buffer.writeUUID(mobId);
        }
    }

    public static void handle(PickerFreezePacket packet, ServerPlayer sender) {
        if (packet.freeze) {
            if (!PickerGate.creative(sender)) {
                return;
            }
            Entity target = sender.serverLevel().getEntity(packet.mobId);
            if (target != null && sender.distanceToSqr(target) > MAX_FREEZE_DISTANCE_SQ) {
                sender.sendSystemMessage(Component.translatable("somegoogly.command.picker.feedback",
                        Component.translatable("somegoogly.command.picker.freeze_too_far")));
                return;
            }
            Component error = PickerFreezeService.freeze(sender.serverLevel(), sender.getUUID(), packet.mobId);
            if (error != null) {
                sender.sendSystemMessage(Component.translatable("somegoogly.command.picker.feedback", error));
            }
        } else {
            PickerFreezeService.unfreeze(sender.serverLevel().getServer(), sender.getUUID());
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
