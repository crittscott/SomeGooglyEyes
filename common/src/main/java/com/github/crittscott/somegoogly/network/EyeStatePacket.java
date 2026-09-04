package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
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

    public EyeStatePacket(int entityId, EyeState.Snapshot snapshot) {
        this(entityId, snapshot.hasEyes(), snapshot.variantRoll(), snapshot.properties());
    }

    public static EyeStatePacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        boolean hasGooglyEyes = buffer.readBoolean();
        float variantRoll = buffer.readFloat();
        if (!Float.isFinite(variantRoll) || variantRoll < 0.0F || variantRoll > 1.0F) {
            throw new DecoderException("Invalid eye placement variant roll");
        }
        AppearanceOverride overrides = AppearanceOverride.STREAM_CODEC.decode(buffer);
        if (!overrides.isValid()) {
            throw new DecoderException("Invalid eye appearance color");
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
        AppearanceOverride.STREAM_CODEC.encode(buffer, packet.overrides);
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

    public EyeState.Snapshot snapshot() {
        return new EyeState.Snapshot(hasGooglyEyes, variantRoll, overrides);
    }

    private boolean valid() {
        return Float.isFinite(variantRoll) && variantRoll >= 0.0F && variantRoll <= 1.0F
                && overrides != null && overrides.isValid();
    }
}
