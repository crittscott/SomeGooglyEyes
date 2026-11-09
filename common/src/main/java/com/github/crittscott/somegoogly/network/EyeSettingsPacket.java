package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Server → client: the {@code googlyEyesEnabled} master switch and the behavior settings clients run behaviors
 * from. Sent to a joining player and to everyone when either changes. Clients read both afresh every tick, so
 * applying them leaves running eye motion alone.
 */
public record EyeSettingsPacket(boolean googlyEyesEnabled, BehaviorSettings behaviors) implements Payload {

    public static final ResourceLocation ID = SomeGooglyCommon.id("eye_settings");

    /** Decode and validate a received payload. */
    public static EyeSettingsPacket read(FriendlyByteBuf buffer) {
        return new EyeSettingsPacket(buffer.readBoolean(), BehaviorSettings.read(buffer));
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(googlyEyesEnabled);
        behaviors.write(buffer);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
