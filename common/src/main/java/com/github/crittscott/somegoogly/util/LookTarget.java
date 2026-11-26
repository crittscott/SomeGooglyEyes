package com.github.crittscott.somegoogly.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

import javax.annotation.Nullable;

/**
 * Ray-picks what a player is aiming at: the living entity the {@code /sg admin} and picker spawn
 * verbs target past vanilla's interaction reach, and whether the eye items' sneak self-use is aimed
 * at nothing.
 */
public final class LookTarget {

    /** Aim-pick range (blocks) for the {@code /sg admin} and {@code /sg spawn} verbs. */
    public static final double DEFAULT_REACH = 20.0;

    /** The block reach vanilla's crosshair uses outside creative. */
    private static final double SURVIVAL_BLOCK_REACH = 4.5;

    /** The block reach vanilla's crosshair uses in creative. */
    private static final double CREATIVE_BLOCK_REACH = 5.0;

    /** The entity reach vanilla's crosshair uses outside creative. */
    private static final double SURVIVAL_ENTITY_REACH = 3.0;

    /** The entity reach vanilla's crosshair uses in creative. */
    private static final double CREATIVE_ENTITY_REACH = 6.0;

    private LookTarget() {}

    /**
     * Whether {@code player} aims at nothing it could click: no block within its block reach and no
     * pickable entity within its entity reach, the reaches vanilla's own crosshair target uses.
     * This version has no reach attributes in common code, so the reaches are vanilla's fixed ones
     * by game mode. Both sides answer the same way, so a use the client predicts on air is the use
     * the server performs.
     */
    public static boolean aimsAtNothing(Player player) {
        boolean creative = player.getAbilities().instabuild;
        double blockReach = creative ? CREATIVE_BLOCK_REACH : SURVIVAL_BLOCK_REACH;
        double entityReach = creative ? CREATIVE_ENTITY_REACH : SURVIVAL_ENTITY_REACH;
        return player.pick(blockReach, 1.0F, false).getType() == HitResult.Type.MISS
                && entityInCrosshair(player, entityReach, Entity::isPickable) == null;
    }

    /**
     * The non-spectator entity matching {@code filter} under {@code player}'s crosshair within
     * {@code reach} blocks and in front of the nearest block there, or {@code null}.
     */
    @Nullable
    private static Entity entityInCrosshair(Player player, double reach, Predicate<Entity> filter) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(reach));
        HitResult block = player.pick(reach, 1.0F, false);
        double distance =
                block.getType() == HitResult.Type.BLOCK
                        ? eye.distanceToSqr(block.getLocation())
                        : reach * reach;
        AABB search = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D);
        EntityHitResult hit =
                ProjectileUtil.getEntityHitResult(
                        player,
                        eye,
                        end,
                        search,
                        e -> !e.isSpectator() && filter.test(e),
                        distance);
        return hit != null ? hit.getEntity() : null;
    }

    /**
     * The pickable, non-spectator living entity under {@code player}'s crosshair within {@code
     * reach} blocks, or {@code null}.
     */
    @Nullable
    public static LivingEntity livingInCrosshair(Player player, double reach) {
        return entityInCrosshair(player, reach, e -> e instanceof LivingEntity && e.isPickable())
                        instanceof LivingEntity living
                ? living
                : null;
    }
}
