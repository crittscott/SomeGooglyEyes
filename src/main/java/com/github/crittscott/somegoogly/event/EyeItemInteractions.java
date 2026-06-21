package com.github.crittscott.somegoogly.event;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.enchant.ModEnchantments;
import com.github.crittscott.somegoogly.head.HeadInfo;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.state.EntityEyeHolder;
import com.github.crittscott.somegoogly.state.EyeHolder;
import com.github.crittscott.somegoogly.state.AppearanceOverride;
import com.github.crittscott.somegoogly.state.EyeState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * The thin-slice eye-item verbs (server-side). Both directions go through the {@link EyeHolder} seam
 * rather than {@link EyeState} directly, so future holders (item frames, item models) reuse them.
 *
 * <ul>
 *   <li><b>Harvest (non-lethal)</b> — right-click an eyed mob with shears enchanted with
 *       {@link ModEnchantments#OPTOMETRIST}: capture its effective appearance into a {@code googly_eye}
 *       item, drop it, and turn the mob's eyes off, leaving the mob unharmed. Plain (unenchanted) shears
 *       do nothing here, so they no longer hijack vanilla shearing (sheep wool, mooshroom).</li>
 *   <li><b>Harvest (on kill)</b> — killing an eyed mob with a direct shears melee blow has a configurable
 *       chance ({@link ServerConfig#HARVEST_ON_KILL_PERCENT}) to drop the same {@code googly_eye}; see
 *       {@link #onLivingDrops}.</li>
 *   <li><b>Reattach</b> — right-click an eyeless but eye-<i>configured</i> mob with a {@code googly_eye}:
 *       apply the item's appearance and turn the mob's eyes on (placement comes from the mob's config).</li>
 * </ul>
 *
 * <p>Both harvest paths use vanilla {@link Items#SHEARS} and capture the mob's appearance from head 0 /
 * eye 0 (per-eye appearance would need the override model to go per-eye, which it isn't yet).
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
        } else if (stack.is(Items.SHEARS) && holder.hasEyes() && hasOptometrist(stack)) {
            harvest(event, player, mob, stack, holder);
        }
    }

    private static boolean hasOptometrist(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.OPTOMETRIST.get(), stack) > 0;
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

        ItemStack drop = buildEyeDrop(helper, holder.getEyeProperties());
        mob.spawnAtLocation(drop);

        holder.setEyeProperties(AppearanceOverride.EMPTY); // appearance now lives in the item
        holder.setHasEyes(false);

        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(event.getHand()));
        consume(event, InteractionResult.SUCCESS);
    }

    /**
     * Killing an eyed mob with a direct shears melee blow has a {@link ServerConfig#HARVEST_ON_KILL_PERCENT}
     * chance to drop its eyes — the only way to collect eyes with plain (un-enchanted) shears. The drop is
     * added to the mob's natural death loot; the shears take durability on a successful harvest.
     */
    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        LivingEntity mob = event.getEntity();
        if (mob.level().isClientSide() || !EyeState.hasEyes(mob)) {
            return;
        }

        DamageSource source = event.getSource();
        // Require a direct melee kill by a player holding shears: the shears must do the deed.
        if (!(source.getEntity() instanceof Player player) || source.getDirectEntity() != player) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (!weapon.is(Items.SHEARS)) {
            return;
        }

        if (mob.getRandom().nextInt(100) >= ServerConfig.HARVEST_ON_KILL_PERCENT.get()) {
            return;
        }

        HeadInfo helper = helperFor(mob);
        if (helper == null || !helper.hasConfig()) {
            return; // no geometry to sample an appearance from
        }

        ItemStack drop = buildEyeDrop(helper, EyeState.readProperties(mob));
        ItemEntity entity = new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), drop);
        entity.setDefaultPickUpDelay();
        event.getDrops().add(entity);

        weapon.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
    }

    /**
     * Build the {@code googly_eye} item a harvest yields: the mob's *effective* appearance — config
     * colors/glow (sampled from head 0 / eye 0) with any per-mob override layered on top — at a count
     * equal to the mob's total eye count.
     */
    private static ItemStack buildEyeDrop(HeadInfo helper, AppearanceOverride override) {
        // Effective appearance = the mob's config appearance (head 0 / eye 0) with its override on top,
        // captured as a fully-populated override so the item carries the exact look.
        AppearanceOverride harvested = helper.appearanceAt(0, 0).overlay(override).toOverride();
        return GooglyEyeItem.create(harvested, totalEyes(helper));
    }

    private static HeadInfo helperFor(LivingEntity mob) {
        // Server-side: resolve geometry (and the mob's chosen variant) from the authoritative server
        // config store, not the client one — getHelper reads ClientEyeConfigs, which is empty on a
        // dedicated server.
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return HeadInfo.serverHelper(type, mob);
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
