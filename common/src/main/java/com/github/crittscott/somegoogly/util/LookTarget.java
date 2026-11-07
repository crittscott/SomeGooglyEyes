package com.github.crittscott.somegoogly.util;

import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ray-picks the living entity a player is aiming at, out to {@code reach} blocks. Shared by the
 * {@code /sg admin} command (server, longer reach) and the held-eye inspect indicator (client), which
 * both need to target the looked-at mob past vanilla's ~3-block melee pick.
 */
public final class LookTarget {

    /** Aim-pick range (blocks) for the {@code /sg admin} and {@code /sg spawn} verbs. */
    public static final double DEFAULT_REACH = 20.0;

    private LookTarget() {
    }

    /** The nearest accepted entity before any blocking surface, or {@code null}. */
    @Nullable
    public static EntityHitResult hitInCrosshair(Player player, double reach, Predicate<Entity> accepts) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(reach));
        HitResult block = player.pick(reach, 1.0F, false);
        double distance = block.getType() == HitResult.Type.BLOCK
                ? eye.distanceToSqr(block.getLocation()) : reach * reach;
        AABB search = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D);
        return ProjectileUtil.getEntityHitResult(player, eye, end, search,
                e -> accepts.test(e) && e.isPickable() && !e.isSpectator(), distance);
    }

    /** The nearest living-entity intersection before any blocking surface, or {@code null}. */
    @Nullable
    public static EntityHitResult livingHitInCrosshair(Player player, double reach) {
        return hitInCrosshair(player, reach, entity -> entity instanceof LivingEntity);
    }

    /**
     * The pickable, non-spectator living entity under {@code player}'s crosshair within {@code reach} blocks, or
     * {@code null}.
     */
    @Nullable
    public static LivingEntity livingInCrosshair(Player player, double reach) {
        EntityHitResult hit = livingHitInCrosshair(player, reach);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }
}
