package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server → client: the {@code googlyEyesEnabled} master switch and the behavior settings clients run behaviors
 * from. Sent to a joining player and to everyone when either changes. Clients read both afresh every tick, so
 * applying them leaves running eye motion alone.
 */
public record EyeSettingsPacket(boolean googlyEyesEnabled, BehaviorSettings behaviors) implements CustomPacketPayload {

    public static final ResourceLocation ID = NetworkProtocol.id("eye_settings");
    public static final CustomPacketPayload.Type<EyeSettingsPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, EyeSettingsPacket> STREAM_CODEC =
            StreamCodec.ofMember(EyeSettingsPacket::write, EyeSettingsPacket::read);

    private static EyeSettingsPacket read(FriendlyByteBuf buffer) {
        return new EyeSettingsPacket(buffer.readBoolean(), BehaviorSettings.read(buffer));
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(googlyEyesEnabled);
        behaviors.write(buffer);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
