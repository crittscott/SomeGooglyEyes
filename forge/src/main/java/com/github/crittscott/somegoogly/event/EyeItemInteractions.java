package com.github.crittscott.somegoogly.event;

import com.github.crittscott.somegoogly.server.EyeItemService;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Forge adapters for the shared eye-item interaction and death-harvest service. */
public final class EyeItemInteractions {

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof LivingEntity living)) {
            return;
        }
        InteractionResult result = EyeItemService.interact(
                event.getEntity(), event.getLevel(), event.getHand(), living);
        if (result != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        EyeItemService.onDeath(event.getEntity(), event.getSource());
    }
}
