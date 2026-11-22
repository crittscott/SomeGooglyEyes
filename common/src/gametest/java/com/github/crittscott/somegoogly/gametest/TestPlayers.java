package com.github.crittscott.somegoogly.gametest;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/** Distinct test players with the template's level and connection, optionally with fixed permissions. */
final class TestPlayers {

    /**
     * An interaction reach shorter than the gap between a player's eyes and the top of the 4-block-high test
     * structure, so aiming up within it touches nothing, whatever encloses the test area.
     */
    private static final double NOTHING_REACH = 0.25;

    /** An operator's command permission (level 2), the one the picker and {@code /sg} require. */
    static final PermissionSet GAMEMASTER = Permissions.COMMANDS_GAMEMASTER::equals;

    /** No permissions at all: a player who is not an operator. */
    static final PermissionSet NO_PERMISSIONS = permission -> false;

    private TestPlayers() {
    }

    /**
     * Aim {@code player} straight up with a reach of {@link #NOTHING_REACH}, so a use is aimed at nothing; the
     * returned action restores its aim and reaches.
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
