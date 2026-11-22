package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server → client sync of a single entity's full eye state: the {@code hasGooglyEyes} flag, the chosen
 * placement-variant roll, plus the optional per-mob appearance overrides (see {@link EyeState}). Sent on
 * start-tracking (so a newly watching player gets current state) and whenever the state is mutated
 * mid-life (so changes from shears, a Slimy Eye, or {@code /sg admin} appear immediately on every
 * tracking client). Both paths follow the entity's own spawn on the same connection, so the client always knows the entity id.
 * The snapshot travels in {@link EyeState.Snapshot#STREAM_CODEC}, the head-item component's wire form.
 */
public record EyeStatePacket(int entityId, EyeState.Snapshot snapshot) implements CustomPacketPayload {

    public static final ResourceLocation ID = SomeGooglyCommon.id("eye_state");
    public static final CustomPacketPayload.Type<EyeStatePacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, EyeStatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EyeStatePacket::entityId,
            EyeState.Snapshot.STREAM_CODEC, EyeStatePacket::snapshot,
            EyeStatePacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
