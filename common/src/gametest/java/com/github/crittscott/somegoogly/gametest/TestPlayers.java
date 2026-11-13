package com.github.crittscott.somegoogly.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/** Distinct test players with the template's level and connection, optionally with fixed permissions. */
final class TestPlayers {

    /** An operator's command permission (level 2), the one the picker and {@code /sg} require. */
    static final PermissionSet GAMEMASTER = Permissions.COMMANDS_GAMEMASTER::equals;

    /** No permissions at all: a player who is not an operator. */
    static final PermissionSet NO_PERMISSIONS = permission -> false;

    private TestPlayers() {
    }

    /** A test player whose permissions come from the server, as for any joined player. */
    static ServerPlayer create(ServerPlayer template, boolean creative) {
        return setUp(new ServerPlayer(template.level().getServer(), template.level(),
                new GameProfile(UUID.randomUUID(), "EyeTest"), ClientInformation.createDefault()), template, creative);
    }

    /** A test player whose own permission checks answer from {@code permissions}. */
    static ServerPlayer create(ServerPlayer template, boolean creative, PermissionSet permissions) {
        return setUp(new ServerPlayer(template.level().getServer(), template.level(),
                new GameProfile(UUID.randomUUID(), "EyeTest"), ClientInformation.createDefault()) {
            @Override
            public PermissionSet permissions() {
                return permissions;
            }
        }, template, creative);
    }

    private static ServerPlayer setUp(ServerPlayer player, ServerPlayer template, boolean creative) {
        player.connection = template.connection;
        player.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        return player;
    }
}
