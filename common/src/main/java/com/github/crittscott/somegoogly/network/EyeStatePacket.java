package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.eye.state.EyeState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;

/**
 * Server → client sync of a single entity's full eye state: the {@code hasGooglyEyes} flag, the chosen
 * placement-variant roll, plus the optional per-mob appearance overrides (see {@link EyeState}). Sent on
 * start-tracking (so a newly watching player gets current state) and whenever the state is mutated
 * mid-life (so changes from shears / dye / redstone appear immediately on every tracking client).
 */
public class EyeStatePacket {

    private final int entityId;
    private final boolean hasGooglyEyes;
    @Nullable
    private final CompoundTag overrides;
    private final float variantRoll;

    public EyeStatePacket(int entityId, boolean hasGooglyEyes, float variantRoll, @Nullable CompoundTag overrides) {
        this.entityId = entityId;
        this.hasGooglyEyes = hasGooglyEyes;
        this.variantRoll = variantRoll;
        this.overrides = overrides;
    }

    public static EyeStatePacket decode(FriendlyByteBuf buffer) {
        int entityId = buffer.readInt();
        boolean hasGooglyEyes = buffer.readBoolean();
        float variantRoll = buffer.readFloat();
        CompoundTag overrides = buffer.readBoolean() ? buffer.readNbt() : null;
        return new EyeStatePacket(entityId, hasGooglyEyes, variantRoll, overrides);
    }

    public static void encode(EyeStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.entityId);
        buffer.writeBoolean(packet.hasGooglyEyes);
        buffer.writeFloat(packet.variantRoll);
        buffer.writeBoolean(packet.overrides != null);
        if (packet.overrides != null) {
            buffer.writeNbt(packet.overrides);
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

    @Nullable
    public CompoundTag overrides() {
        return overrides;
    }
}
