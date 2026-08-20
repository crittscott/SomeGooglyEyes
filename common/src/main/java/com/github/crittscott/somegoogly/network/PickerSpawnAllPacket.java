package com.github.crittscott.somegoogly.network;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.picker.PickerPermissions;
import com.github.crittscott.somegoogly.picker.PickerSpawnService;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

/**
 * Client → server: "spawn the {@code /sg spawnall} grid around me", optionally narrowed to one mod
 * namespace. Creative-gated ({@link PickerPermissions}) <b>and</b> refused unless the server config's
 * {@link ServerConfig#ALLOW_SPAWN_ALL} is enabled (default off — the grid terraforms and mass-spawns
 * with no undo, so an admin must consciously opt in). The layout/terraform work and all feedback
 * are {@link PickerSpawnService#spawn} on the server thread. The filter is validated against the
 * namespace charset so an arbitrary client string never reaches the command.
 */
public class PickerSpawnAllPacket {

    @Nullable
    private final String modFilter;

    public PickerSpawnAllPacket(@Nullable String modFilter) {
        this.modFilter = modFilter;
    }

    public static PickerSpawnAllPacket decode(FriendlyByteBuf buffer) {
        String modFilter = buffer.readBoolean() ? buffer.readUtf() : null;
        return new PickerSpawnAllPacket(modFilter);
    }

    public static void encode(PickerSpawnAllPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.modFilter != null);
        if (packet.modFilter != null) {
            buffer.writeUtf(packet.modFilter);
        }
    }

    public static void handle(PickerSpawnAllPacket packet, NetworkManager.PacketContext context) {
        context.queue(() -> {
            ServerPlayer sender = context.getPlayer() instanceof ServerPlayer player ? player : null;
            if (!PickerPermissions.creative(sender)) {
                return;
            }
            if (!ServerConfig.ALLOW_SPAWN_ALL.get()) {
                sender.sendSystemMessage(Component.translatable("somegoogly.command.picker.spawnall_disabled"));
                return;
            }
            // Only the chars legal in a mod namespace; anything else is dropped (spawn(null) = all mods).
            if (packet.modFilter != null && !packet.modFilter.matches("[a-z0-9_.-]+")) {
                return;
            }
            PickerSpawnService.spawn(sender, packet.modFilter);
        });
    }
}
