package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.picker.PickerFreezeService;
import com.github.crittscott.somegoogly.picker.PickerGate;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Client → server: reposition ({@code /sg mob move}) or turn ({@code /sg mob rot}) the mob being
 * edited. Creative-gated ({@link PickerGate}). The move form carries world-axis <b>offsets</b>
 * (0 = leave that axis unchanged), resolved here against the <b>authoritative</b> server entity rather
 * than the client's interpolated copy; the change syncs back through vanilla entity tracking. The two
 * forms are distinguished on the wire by which fields are present: move sets all of x/y/z, rot sets
 * azimuth. The target must be the sender's own {@link PickerFreezeService}-frozen mob, and a move's
 * offset magnitude is capped at {@value #MAX_MOVE} blocks.
 *
 * <p>{@code azimuth} uses the <b>eye</b> convention (degrees from +X; 270 = facing -Z) so its numbers
 * mean the same direction as {@code /sg rot}; Minecraft yaw is that minus 90°. Body and head turn
 * together (body rotation isn't synced on its own — the client re-derives it from yaw/head).
 */
public class PickerMobPosePacket {

    /** Cap on a single move packet's offset magnitude, matching other picker reach conventions. */
    private static final double MAX_MOVE = 20.0;
    private static final double MAX_MOVE_SQUARED = MAX_MOVE * MAX_MOVE;

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

    public static void handle(PickerMobPosePacket packet, NetworkTransport.Context context) {
        context.queue(() -> {
            ServerPlayer sender = context.player();
            if (!PickerGate.creative(sender)) {
                return;
            }
            if (!packet.isValid()) {
                return;
            }
            if (!packet.mobId.equals(PickerFreezeService.frozenMobId(sender.getUUID()))) {
                sender.sendSystemMessage(Component.translatable("somegoogly.command.picker.mob_not_chosen"));
                return;
            }
            Entity entity = sender.serverLevel().getEntity(packet.mobId);
            if (!(entity instanceof LivingEntity living)) {
                sender.sendSystemMessage(Component.translatable("somegoogly.command.picker.mob_not_found"));
                return;
            }
            if (packet.x != null && packet.y != null && packet.z != null) {
                double distanceSquared = packet.x * packet.x + packet.y * packet.y + packet.z * packet.z;
                if (distanceSquared > MAX_MOVE_SQUARED) {
                    sender.sendSystemMessage(Component.translatable("somegoogly.command.picker.mob_out_of_range", MAX_MOVE));
                    return;
                }
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
    }

    /** Whether the client supplied exactly one complete, finite operation form. */
    public boolean isValid() {
        boolean anyMove = x != null || y != null || z != null;
        boolean completeMove = x != null && y != null && z != null;
        boolean rotation = azimuth != null;
        if (anyMove != completeMove || completeMove == rotation) {
            return false;
        }
        if (completeMove) {
            return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
        }
        return Float.isFinite(azimuth);
    }

    private static void writeOptionalDouble(FriendlyByteBuf buffer, @Nullable Double value) {
        buffer.writeBoolean(value != null);
        if (value != null) {
            buffer.writeDouble(value);
        }
    }
}
