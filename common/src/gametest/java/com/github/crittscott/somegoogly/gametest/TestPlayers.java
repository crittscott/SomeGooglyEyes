package com.github.crittscott.somegoogly.gametest;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

/** Distinct test players with actual player permission checks and the loader's fake connection. */
final class TestPlayers {
    private TestPlayers() {
    }

    /**
     * Aim {@code player} straight up, so a use is aimed at nothing; the returned action restores its aim. This
     * version's reaches are fixed by game mode and cannot be shortened, so this relies on nothing standing within
     * five blocks above the player.
     */
    static Runnable aimAtNothing(Player player) {
        float xRot = player.getXRot();
        player.setXRot(-90.0F);
        return () -> player.setXRot(xRot);
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
