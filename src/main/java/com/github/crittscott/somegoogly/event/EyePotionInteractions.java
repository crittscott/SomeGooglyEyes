package com.github.crittscott.somegoogly.event;

import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.state.EntityEyeHolder;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.potion.ModPotions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

/**
 * "Googly eyes" potion behavior (server-side), in two forms:
 *
 * <ul>
 *   <li><b>Splash</b>: when a thrown {@link ModPotions#GOOGLY_EYES} splash breaks, exactly <b>one</b>
 *   randomly chosen eligible mob within the splash area gets eyes — not the whole cloud.</li>
 *   <li><b>Drinkable</b>: when a living entity finishes drinking the potion, that drinker gets its own
 *   eyes (the form that lets a player eye themselves).</li>
 * </ul>
 *
 * <p>For the splash we do <i>not</i> cancel the impact: the potion carries no {@code MobEffect}s, so
 * vanilla's own splash application is inert, but it still breaks the bottle, plays the particles/sound,
 * and discards the projectile. We just add the single-target pick on top. The drinkable form likewise
 * carries no effects, so vanilla's drink is an inert no-op (bottle returned, sound played) and we layer
 * the self-apply on top.
 */
public class EyePotionInteractions {

    // Mirror vanilla splash reach (ThrownPotion inflates its box by 4,2,4).
    private static final double RADIUS_XZ = 4.0;
    private static final double RADIUS_Y = 2.0;

    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownPotion potion)) {
            return;
        }
        Level level = potion.level();
        if (level.isClientSide()) {
            return;
        }

        ItemStack stack = potion.getItem();
        if (!stack.is(Items.SPLASH_POTION) || PotionUtils.getPotion(stack) != ModPotions.GOOGLY_EYES.get()) {
            return;
        }

        Vec3 hit = event.getRayTraceResult().getLocation();
        AABB area = new AABB(
                hit.x - RADIUS_XZ, hit.y - RADIUS_Y, hit.z - RADIUS_XZ,
                hit.x + RADIUS_XZ, hit.y + RADIUS_Y, hit.z + RADIUS_XZ);

        // Eligible = the entity can actually wear eyes (config-gated; players have a definition and so
        // qualify) and doesn't already have them, so a potion thrown into a partly-eyed crowd still
        // does something.
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> ServerEyeConfigs.isEligible(e) && !EyeState.hasEyes(e));
        if (candidates.isEmpty()) {
            return;
        }

        LivingEntity chosen = candidates.get(level.getRandom().nextInt(candidates.size()));

        // Apply the potion's carried appearance (brewed in from the eye item), then turn eyes on —
        // the potion is the only way to give a mob (or player) eyes. Empty properties fall back to config.
        EntityEyeHolder holder = new EntityEyeHolder(chosen);
        holder.setEyeProperties(GooglyEyeItem.getProperties(stack));
        holder.setHasEyes(true);
    }

    @SubscribeEvent
    public void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        LivingEntity drinker = event.getEntity();
        if (drinker.level().isClientSide()) {
            return;
        }

        ItemStack stack = event.getItem();
        if (!stack.is(Items.POTION) || PotionUtils.getPotion(stack) != ModPotions.GOOGLY_EYES.get()) {
            return;
        }

        // Only entities that can actually show eyes at their current age (players included — they have a
        // definition). Drinking re-applies appearance even if the drinker already has eyes, so a player
        // can recolor by drinking a differently-tinted brew. Empty properties fall back to config.
        if (!ServerEyeConfigs.isEligible(drinker)) {
            return;
        }

        EntityEyeHolder holder = new EntityEyeHolder(drinker);
        holder.setEyeProperties(GooglyEyeItem.getProperties(stack));
        holder.setHasEyes(true);
    }
}
