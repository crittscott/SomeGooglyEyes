package com.github.crittscott.somegoogly.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/** Distinct test players with actual player permission checks and the loader's fake connection. */
final class TestPlayers {
    private TestPlayers() {
    }

    static ServerPlayer create(ServerPlayer template, boolean creative, int permission) {
        ServerPlayer player = new ServerPlayer(template.serverLevel().getServer(), template.serverLevel(),
            new GameProfile(UUID.randomUUID(), "EyeTest")) {
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
