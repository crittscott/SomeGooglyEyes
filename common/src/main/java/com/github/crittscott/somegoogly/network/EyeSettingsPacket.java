package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server → client: the {@code googlyEyesEnabled} master switch and the behavior settings clients run behaviors
 * from. Sent to a joining player and to everyone when either changes. Clients read both afresh every tick, so
 * applying them leaves running eye motion alone.
 */
public record EyeSettingsPacket(boolean googlyEyesEnabled, BehaviorSettings behaviors) implements CustomPacketPayload {

    public static final ResourceLocation ID = SomeGooglyCommon.id("eye_settings");
    public static final StreamCodec<RegistryFriendlyByteBuf, EyeSettingsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, EyeSettingsPacket::googlyEyesEnabled,
            BehaviorSettings.STREAM_CODEC, EyeSettingsPacket::behaviors,
            EyeSettingsPacket::new);
    public static final CustomPacketPayload.Type<EyeSettingsPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
