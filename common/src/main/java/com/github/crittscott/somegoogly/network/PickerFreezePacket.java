package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.picker.PickerFreezeService;
import com.github.crittscott.somegoogly.picker.PickerPermissions;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Client → server: "freeze the mob I'm editing" (or release it). The freeze itself — NoAi capture,
 * restore, crash-proofing — is owned by {@link PickerFreezeService} on the server, so it works from a
 * remote client and survives that client disappearing; this packet only asks.
 *
 * <p>Freezing requires creative mode ({@link PickerPermissions}). <b>Unfreezing does not</b>: it only
 * releases the sender's own frozen mob, and gating it would strand a mob frozen if the player lost
 * creative mid-edit.
 */
public class PickerFreezePacket {

    private final boolean freeze;
    @Nullable
    private final UUID mobId; // present iff freeze

    private PickerFreezePacket(boolean freeze, @Nullable UUID mobId) {
        this.freeze = freeze;
        this.mobId = mobId;
    }

    public static PickerFreezePacket freeze(UUID mobId) {
        return new PickerFreezePacket(true, mobId);
    }

    public static PickerFreezePacket unfreeze() {
        return new PickerFreezePacket(false, null);
    }

    public static PickerFreezePacket decode(FriendlyByteBuf buffer) {
        boolean freeze = buffer.readBoolean();
        UUID mobId = freeze ? buffer.readUUID() : null;
        return new PickerFreezePacket(freeze, mobId);
    }

    public static void encode(PickerFreezePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.freeze);
        if (packet.freeze) {
            buffer.writeUUID(packet.mobId);
        }
    }

    public static void handle(PickerFreezePacket packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            ServerPlayer sender = context.getPlayer() instanceof ServerPlayer player ? player : null;
            if (packet.freeze) {
                if (!PickerPermissions.creative(sender)) {
                    return;
                }
                String error = PickerFreezeService.freeze(sender.serverLevel(), sender.getUUID(), packet.mobId);
                if (error != null) {
                    sender.sendSystemMessage(Component.translatable("somegoogly.command.picker.feedback", error));
                }
            } else if (sender != null) {
                PickerFreezeService.unfreeze(sender.serverLevel().getServer(), sender.getUUID());
            }
        });
    }
}
