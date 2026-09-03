package com.github.crittscott.somegoogly.event;

import com.github.crittscott.somegoogly.eye.behavior.ServerBehaviorScheduler;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Translates server-side game events into eye-behavior reactions, handing each off to
 * {@link ServerBehaviorScheduler} — the sole authority that decides whether the mob actually plays
 * (eyed, tracked, not already busy). This class stays a thin adapter: no eligibility logic lives here.
 *
 * <ul>
 *   <li><b>grow</b> — a player damaging an eyed mob has a configurable chance to bulge its eyes.</li>
 *   <li><b>swirl</b> — completing a trade with an eyed villager (or wandering trader), or an eyed mob
 *       being healed (rate-limited), spins its pupils.</li>
 * </ul>
 *
 * Ambient behaviors (blink, side_eye, stare, cross_eye) and color_change are scheduled through the
 * config-driven pool and admin command, not driven from here.
 */
public class EyeReactionHandler {

    @SubscribeEvent
    public void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        ServerBehaviorScheduler.onHealed(event.getEntity());
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getSource().getEntity() instanceof Player) {
            ServerBehaviorScheduler.onPlayerHurt(event.getEntity());
        }
    }

    @SubscribeEvent
    public void onTradeWithVillager(TradeWithVillagerEvent event) {
        if (event.getAbstractVillager().level().isClientSide()) {
            return;
        }
        ServerBehaviorScheduler.onTrade(event.getAbstractVillager());
    }
}
