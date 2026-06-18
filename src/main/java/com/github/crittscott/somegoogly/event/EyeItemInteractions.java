package com.github.crittscott.somegoogly.event;

import com.github.crittscott.somegoogly.head.HeadInfo;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.state.EntityEyeHolder;
import com.github.crittscott.somegoogly.state.EyeHolder;
import com.github.crittscott.somegoogly.state.EyeProperties;
import com.github.crittscott.somegoogly.state.EyeState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * The thin-slice eye-item verbs (server-side). Both directions go through the {@link EyeHolder} seam
 * rather than {@link EyeState} directly, so future holders (item frames, item models) reuse them.
 *
 * <ul>
 *   <li><b>Harvest</b> — right-click an eyed mob with shears: capture its effective appearance into a
 *       {@code googly_eye} item, drop it, and turn the mob's eyes off.</li>
 *   <li><b>Reattach</b> — right-click an eyeless but eye-<i>configured</i> mob with a {@code googly_eye}:
 *       apply the item's appearance and turn the mob's eyes on (placement comes from the mob's config).</li>
 * </ul>
 *
 * <p>Harvest uses vanilla {@link Items#SHEARS} — settled; there is no dedicated harvest tool. The one
 * remaining simplification: the mob's appearance is sampled from head 0 / eye 0 (per-eye appearance
 * would need the override model to go per-eye, which it isn't yet).
 */
public class EyeItemInteractions {

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getTarget() instanceof LivingEntity mob)) {
            return;
        }

        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        EntityEyeHolder holder = new EntityEyeHolder(mob);

        if (stack.getItem() instanceof GooglyEyeItem) {
            reattach(event, player, mob, stack, holder);
        } else if (stack.is(Items.SHEARS) && holder.hasEyes()) {
            harvest(event, player, mob, stack, holder);
        }
    }

    private static void reattach(PlayerInteractEvent.EntityInteract event, Player player, LivingEntity mob,
                                 ItemStack stack, EyeHolder holder) {
        HeadInfo helper = helperFor(mob);
        if (helper == null || !helper.hasConfig()) {
            player.displayClientMessage(Component.literal("This mob can't wear googly eyes."), true);
            consume(event, InteractionResult.FAIL);
            return;
        }
        if (holder.hasEyes()) {
            return; // already has eyes — leave other interactions alone
        }

        holder.setEyeProperties(GooglyEyeItem.getProperties(stack));
        holder.setHasEyes(true);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        consume(event, InteractionResult.SUCCESS);
    }

    private static void harvest(PlayerInteractEvent.EntityInteract event, Player player, LivingEntity mob,
                                ItemStack stack, EyeHolder holder) {
        HeadInfo helper = helperFor(mob);
        if (helper == null || !helper.hasConfig()) {
            return; // shouldn't happen while hasEyes is true, but guard anyway
        }

        // The eye item captures the mob's *effective* appearance: config colours/glow (sampled from
        // head 0 / eye 0) with any per-mob override layered on top.
        EyeProperties configProps = EyeProperties.EMPTY
                .withCorneaColor(EyeState.packColor(helper.getCorneaColours(0, 0)))
                .withIrisColor(EyeState.packColor(helper.getIrisColours(0, 0)))
                .withGlow(helper.doesEyeGlow(0, 0));
        EyeProperties harvested = configProps.merge(holder.getEyeProperties());

        ItemStack drop = GooglyEyeItem.create(harvested, totalEyes(helper));
        mob.spawnAtLocation(drop);

        holder.setEyeProperties(EyeProperties.EMPTY); // appearance now lives in the item
        holder.setHasEyes(false);

        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(event.getHand()));
        consume(event, InteractionResult.SUCCESS);
    }

    private static HeadInfo helperFor(LivingEntity mob) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return HeadInfo.getHelper(type, mob);
    }

    private static int totalEyes(HeadInfo helper) {
        int total = 0;
        for (int head = 0; head < helper.getHeadCount(); head++) {
            total += helper.getEyeCount(head);
        }
        return Math.max(1, total);
    }

    private static void consume(PlayerInteractEvent.EntityInteract event, InteractionResult result) {
        event.setCanceled(true);
        event.setCancellationResult(result);
    }
}
