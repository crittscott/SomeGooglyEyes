package com.github.crittscott.somegoogly.util;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ray-picks the living entity a player is aiming at, out to {@code reach} blocks, for the {@code /sg admin} and
 * picker spawn verbs, which target the looked-at mob past vanilla's interaction reach.
 */
public final class LookTarget {

    /** Aim-pick range (blocks) for the {@code /sg admin} and {@code /sg spawn} verbs. */
    public static final double DEFAULT_REACH = 20.0;

    private LookTarget() {
    }

    /**
     * The pickable, non-spectator living entity under {@code player}'s crosshair within {@code reach} blocks, or
     * {@code null}.
     */
    @Nullable
    public static LivingEntity livingInCrosshair(Player player, double reach) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(reach));
        HitResult block = player.pick(reach, 1.0F, false);
        double distance = block.getType() == HitResult.Type.BLOCK
                ? eye.distanceToSqr(block.getLocation()) : reach * reach;
        AABB search = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, search,
                e -> e instanceof LivingEntity && e.isPickable() && !e.isSpectator(), distance);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }
}
