package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.picker.PickerPermissions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client → server: reposition ({@code /sg mob move}) or turn ({@code /sg mob rot}) the mob being
 * edited. Creative-gated ({@link PickerPermissions}). The move form carries world-axis <b>offsets</b>
 * (0 = leave that axis unchanged), resolved here against the <b>authoritative</b> server entity rather
 * than the client's interpolated copy; the change syncs back through vanilla entity tracking. The two
 * forms are distinguished on the wire by which fields are present: move sets all of x/y/z, rot sets
 * azimuth.
 *
 * <p>{@code azimuth} uses the <b>eye</b> convention (degrees from +X; 270 = facing -Z) so its numbers
 * mean the same direction as {@code /sg rot}; Minecraft yaw is that minus 90°. Body and head turn
 * together (body rotation isn't synced on its own — the client re-derives it from yaw/head).
 */
public class PickerMobPosePacket {

    @Nullable
    private final Float azimuth;
    private final UUID mobId;
    @Nullable
    private final Double x;
    @Nullable
    private final Double y;
    @Nullable
    private final Double z;

    private PickerMobPosePacket(UUID mobId, @Nullable Double x, @Nullable Double y, @Nullable Double z,
                                @Nullable Float azimuth) {
        this.mobId = mobId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.azimuth = azimuth;
    }

    public static PickerMobPosePacket move(UUID mobId, double dx, double dy, double dz) {
        return new PickerMobPosePacket(mobId, dx, dy, dz, null);
    }

    public static PickerMobPosePacket rot(UUID mobId, float azimuth) {
        return new PickerMobPosePacket(mobId, null, null, null, azimuth);
    }

    public static PickerMobPosePacket decode(FriendlyByteBuf buffer) {
        UUID mobId = buffer.readUUID();
        Double x = buffer.readBoolean() ? buffer.readDouble() : null;
        Double y = buffer.readBoolean() ? buffer.readDouble() : null;
        Double z = buffer.readBoolean() ? buffer.readDouble() : null;
        Float azimuth = buffer.readBoolean() ? buffer.readFloat() : null;
        return new PickerMobPosePacket(mobId, x, y, z, azimuth);
    }

    public static void encode(PickerMobPosePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.mobId);
        writeOptionalDouble(buffer, packet.x);
        writeOptionalDouble(buffer, packet.y);
        writeOptionalDouble(buffer, packet.z);
        buffer.writeBoolean(packet.azimuth != null);
        if (packet.azimuth != null) {
            buffer.writeFloat(packet.azimuth);
        }
    }

    public static void handle(PickerMobPosePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (!PickerPermissions.creative(sender)) {
                return;
            }
            Entity entity = sender.serverLevel().getEntity(packet.mobId);
            if (!(entity instanceof LivingEntity living)) {
                sender.sendSystemMessage(Component.translatable("somegoogly.command.picker.mob_not_found"));
                return;
            }
            if (packet.x != null && packet.y != null && packet.z != null) {
                living.teleportTo(living.getX() + packet.x, living.getY() + packet.y, living.getZ() + packet.z);
                sender.sendSystemMessage(Component.translatable("somegoogly.command.picker.mob_moved",
                        String.format("%.2f", living.getX()), String.format("%.2f", living.getY()), String.format("%.2f", living.getZ())));
            }
            if (packet.azimuth != null) {
                float yaw = Mth.wrapDegrees(packet.azimuth - 90.0F);
                living.setYRot(yaw);
                living.setYHeadRot(yaw);
                living.setYBodyRot(yaw);
                sender.sendSystemMessage(Component.translatable("somegoogly.command.picker.mob_rotated",
                        String.format("%.0f", packet.azimuth)));
            }
        });
        context.setPacketHandled(true);
    }

    private static void writeOptionalDouble(FriendlyByteBuf buffer, @Nullable Double value) {
        buffer.writeBoolean(value != null);
        if (value != null) {
            buffer.writeDouble(value);
        }
    }
}
