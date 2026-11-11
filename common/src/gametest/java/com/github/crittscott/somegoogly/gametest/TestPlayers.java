package com.github.crittscott.somegoogly.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/** Distinct test players with actual player permission checks and the template's connection. */
final class TestPlayers {

    private TestPlayers() {
    }

    static ServerPlayer create(ServerPlayer template, boolean creative, int permission) {
        ServerPlayer player = new ServerPlayer(template.serverLevel().getServer(), template.serverLevel(),
                new GameProfile(UUID.randomUUID(), "EyeTest"), ClientInformation.createDefault()) {
            @Override
            public boolean hasPermissions(int required) {
                return permission >= required;
            }
        };
        player.connection = template.connection;
        player.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        return player;
    }
}
