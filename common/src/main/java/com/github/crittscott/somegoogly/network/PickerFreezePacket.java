package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.picker.PickerFreezeService;
import com.github.crittscott.somegoogly.picker.PickerGate;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import org.jspecify.annotations.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Client → server: "freeze the mob I'm editing" (or release it). The freeze itself — NoAi capture,
 * restore, crash-proofing — is owned by {@link PickerFreezeService} on the server, so it works from a
 * remote client and survives that client disappearing; this packet only asks.
 *
 * <p>Freezing requires picker authorization ({@link PickerGate#authorized}). <b>Unfreezing does
 * not</b>: it only releases the sender's own frozen mob, and gating it would strand a mob frozen if the
 * player lost creative or operator status mid-edit.
 */
public class PickerFreezePacket implements CustomPacketPayload {

    public static final Identifier ID = SomeGooglyCommon.id("picker_freeze");

    /**
     * Server-side sanity bound on the freeze target's distance from the requester. Well past the crosshair
     * reach the picker chooses mobs at, because the client hits the mob's box and the server measures to its
     * position, which are far apart on a very large mob. Keeps an authorized client from freezing an
     * arbitrary mob in any loaded chunk by UUID.
     */
    private static final double MAX_FREEZE_DISTANCE = 24.0;
    private static final double MAX_FREEZE_DISTANCE_SQ = MAX_FREEZE_DISTANCE * MAX_FREEZE_DISTANCE;

    /** The mob to freeze, or absent to release; a presence boolean, then the UUID. */
    public static final StreamCodec<RegistryFriendlyByteBuf, PickerFreezePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), packet -> Optional.ofNullable(packet.mobId),
            mobId -> new PickerFreezePacket(mobId.orElse(null)));
    public static final CustomPacketPayload.Type<PickerFreezePacket> TYPE = new CustomPacketPayload.Type<>(ID);

    @Nullable
    private final UUID mobId; // present iff freezing

    private PickerFreezePacket(@Nullable UUID mobId) {
        this.mobId = mobId;
    }

    /** A request to freeze the mob with {@code mobId} for the sender's picker. */
    public static PickerFreezePacket freeze(UUID mobId) {
        return new PickerFreezePacket(mobId);
    }

    /** Freeze the requested mob for an authorized sender in range, or release the sender's frozen mob. */
    public static void handle(PickerFreezePacket packet, ServerPlayer sender) {
        UUID mobId = packet.mobId;
        if (mobId != null) {
            if (!PickerGate.authorized(sender)) {
                return;
            }
            Entity target = sender.level().getEntity(mobId);
            if (target != null && sender.distanceToSqr(target) > MAX_FREEZE_DISTANCE_SQ) {
                PickerGate.tell(sender, Component.translatable("somegoogly.command.picker.freeze_too_far"));
                return;
            }
            Component error = PickerFreezeService.freeze(sender.level(), sender.getUUID(), mobId);
            if (error != null) {
                PickerGate.tell(sender, error);
            }
        } else {
            PickerFreezeService.unfreeze(sender.level().getServer(), sender.getUUID());
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** A request to release the sender's frozen mob. */
    public static PickerFreezePacket unfreeze() {
        return new PickerFreezePacket(null);
    }
}
