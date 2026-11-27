package com.github.crittscott.somegoogly.gametest;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/** Distinct test players with actual player permission checks and the template's connection. */
final class TestPlayers {

    /**
     * An interaction reach shorter than the gap between a player's eyes and the top of the
     * 4-block-high test structure, so aiming up within it touches nothing, whatever encloses the
     * test area.
     */
    private static final double NOTHING_REACH = 0.25;

    private TestPlayers() {}

    /**
     * Aim {@code player} straight up with a reach of {@link #NOTHING_REACH}, so a use is aimed at
     * nothing; the returned action restores its aim and reaches.
     */
    static Runnable aimAtNothing(Player player) {
        AttributeInstance block = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        AttributeInstance entity = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        double blockReach = block.getBaseValue();
        double entityReach = entity.getBaseValue();
        float xRot = player.getXRot();
        player.setXRot(-90.0F);
        block.setBaseValue(NOTHING_REACH);
        entity.setBaseValue(NOTHING_REACH);
        return () -> {
            block.setBaseValue(blockReach);
            entity.setBaseValue(entityReach);
            player.setXRot(xRot);
        };
    }

    static ServerPlayer create(ServerPlayer template, boolean creative, int permission) {
        ServerPlayer player =
                new ServerPlayer(
                        template.level().getServer(),
                        template.level(),
                        new GameProfile(UUID.randomUUID(), "EyeTest"),
                        ClientInformation.createDefault()) {
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
