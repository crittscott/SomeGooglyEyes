package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Server → client sync of a single entity's full eye state: the {@code hasGooglyEyes} flag, the chosen
 * placement-variant roll, plus the optional per-mob appearance overrides (see {@link EyeState}). Sent on
 * start-tracking (so a newly watching player gets current state) and whenever the state is mutated
 * mid-life (so changes from shears / dye / redstone appear immediately on every tracking client).
 */
public class EyeStatePacket {

    private final int entityId;
    private final boolean hasGooglyEyes;
    private final AppearanceOverride overrides;
    private final float variantRoll;

    public EyeStatePacket(int entityId, boolean hasGooglyEyes, float variantRoll, AppearanceOverride overrides) {
        this.entityId = entityId;
        this.hasGooglyEyes = hasGooglyEyes;
        this.variantRoll = variantRoll;
        this.overrides = overrides;
    }

    public static EyeStatePacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        boolean hasGooglyEyes = buffer.readBoolean();
        float variantRoll = buffer.readFloat();
        if (!Float.isFinite(variantRoll) || variantRoll < 0.0F || variantRoll > 1.0F) {
            throw new DecoderException("Invalid eye placement variant roll");
        }
        int flags = buffer.readUnsignedByte();
        if ((flags & ~0x7) != 0) {
            throw new DecoderException("Unknown eye appearance flags");
        }
        AppearanceOverride overrides = AppearanceOverride.EMPTY;
        if ((flags & 0x1) != 0) {
            overrides = overrides.withCorneaColor(readColor(buffer));
        }
        if ((flags & 0x2) != 0) {
            overrides = overrides.withIrisColor(readColor(buffer));
        }
        if ((flags & 0x4) != 0) {
            overrides = overrides.withGlow(buffer.readBoolean());
        }
        return new EyeStatePacket(entityId, hasGooglyEyes, variantRoll, overrides);
    }

    public static void encode(EyeStatePacket packet, FriendlyByteBuf buffer) {
        if (!packet.valid()) {
            throw new EncoderException("Invalid eye state packet");
        }
        buffer.writeInt(packet.entityId);
        buffer.writeBoolean(packet.hasGooglyEyes);
        buffer.writeFloat(packet.variantRoll);
        int flags = (packet.overrides.cornea().isPresent() ? 0x1 : 0)
                | (packet.overrides.iris().isPresent() ? 0x2 : 0)
                | (packet.overrides.glow().isPresent() ? 0x4 : 0);
        buffer.writeByte(flags);
        if (packet.overrides.cornea().isPresent()) {
            writeColor(buffer, packet.overrides.cornea().orElseThrow());
        }
        if (packet.overrides.iris().isPresent()) {
            writeColor(buffer, packet.overrides.iris().orElseThrow());
        }
        if (packet.overrides.glow().isPresent()) {
            buffer.writeBoolean(packet.overrides.glow().orElseThrow());
        }
    }

    public int entityId() {
        return entityId;
    }

    public boolean hasGooglyEyes() {
        return hasGooglyEyes;
    }

    public float variantRoll() {
        return variantRoll;
    }

    public AppearanceOverride overrides() {
        return overrides;
    }

    private boolean valid() {
        return Float.isFinite(variantRoll) && variantRoll >= 0.0F && variantRoll <= 1.0F
                && overrides != null && overrides.isValid();
    }

    private static EyeColor readColor(FriendlyByteBuf buffer) {
        EyeColor color = new EyeColor(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        if (!validColor(color)) {
            throw new DecoderException("Invalid eye appearance color");
        }
        return color;
    }

    private static void writeColor(FriendlyByteBuf buffer, EyeColor color) {
        buffer.writeFloat(color.r());
        buffer.writeFloat(color.g());
        buffer.writeFloat(color.b());
    }

    private static boolean validColor(EyeColor color) {
        return color == null || color.isValid();
    }
}
