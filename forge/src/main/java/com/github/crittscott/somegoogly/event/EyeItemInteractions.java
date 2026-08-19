package com.github.crittscott.somegoogly.event;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.enchant.ModEnchantments;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.item.SlimyEyeItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * The eye-item verbs that ride entity right-clicks: the slimy eye's <i>apply</i> and the shears
 * <i>harvest</i>s.
 *
 * <ul>
 *   <li><b>Apply</b> — right-click an eyeless living entity with a slimy eye ({@link SlimyEyeItem}):
 *       give it eyes carrying the stack's appearance on a freshly rolled placement variant, consuming
 *       one; an already-eyed or unconfigured target refuses and consumes nothing
 *       ({@link SlimyEyeItem#applyToTarget}).
 *       The verb is dispatched here rather than through {@code Item#interactLivingEntity} because the
 *       item verb runs only after the target's own interaction declines the click, and mobs that
 *       consume generic right-clicks (an untamed horse rears, a villager opens trade, a tamed pet
 *       sits) would swallow it. This event fires before the target sees the click, so while a slimy
 *       eye is held the entity right-click is always the applicator's — cancelled on both sides so
 *       the client doesn't play the target's own reaction either.</li>
 *   <li><b>Harvest (non-lethal)</b> — right-click an eyed mob with shears enchanted with
 *       {@link ModEnchantments#OPTOMETRIST}: capture its effective appearance into a {@code googly_eye}
 *       item, drop it, and turn the mob's eyes off, leaving the mob unharmed. Plain (unenchanted) shears
 *       do nothing here, leaving vanilla shearing (sheep wool, mooshroom) untouched.</li>
 *   <li><b>Harvest (on kill)</b> — killing an eyed mob with a direct shears melee blow has a configurable
 *       chance ({@link ServerConfig#HARVEST_ON_KILL_PERCENT}) to drop the same {@code googly_eye}; see
 *       {@link #onLivingDrops}.</li>
 * </ul>
 *
 * <p>The plain eye item is a crafting ingredient, not a direct right-click apply, and has no verb
 * here or anywhere.
 *
 * <p>Both harvest paths accept any {@link ShearsItem} (vanilla or modded) and capture the mob's appearance from head 0 /
 * eye 0 (the override model is per-mob, not per-eye). Shears are a vanilla item we don't own, which is
 * why the harvest verbs live in this event handler rather than on an item. The harvest verbs are
 * server-side only; the apply dispatch runs on both sides.
 */
public class EyeItemInteractions {

    /**
     * Build the {@code googly_eye} item a harvest yields: the mob's *effective* appearance — config
     * colors/glow (sampled from head 0 / eye 0) with any per-mob override layered on top. Always a
     * single eye: the slimy eye that gives eyes consumes one eye, so dropping the mob's whole eye count
     * would let one seed eye multiply through a craft-apply-harvest loop.
     */
    private static ItemStack buildEyeDrop(HeadInfo helper, AppearanceOverride override) {
        // Effective appearance = the mob's config appearance (head 0 / eye 0) with its override on top,
        // captured as a fully-populated override so the item carries the exact look.
        AppearanceOverride harvested = helper.appearanceAt(0, 0).overlay(override).toOverride();
        return GooglyEyeItem.create(harvested, 1);
    }

    private static void consume(PlayerInteractEvent.EntityInteract event, InteractionResult result) {
        event.setCanceled(true);
        event.setCancellationResult(result);
    }

    private static void harvest(PlayerInteractEvent.EntityInteract event, Player player, LivingEntity mob,
                                ItemStack stack) {
        HeadInfo helper = helperFor(mob);
        if (!helper.hasConfig()) {
            return; // shouldn't happen while hasEyes is true, but guard anyway
        }

        ItemStack drop = buildEyeDrop(helper, EyeState.readProperties(mob));
        mob.spawnAtLocation(drop);

        EyeState.setProperties(mob, AppearanceOverride.EMPTY); // appearance now lives in the item
        EyeState.setHasEyes(mob, false);

        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(event.getHand()));
        consume(event, InteractionResult.SUCCESS);
    }

    private static boolean hasOptometrist(ItemStack stack) {
        return EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.OPTOMETRIST.get(), stack) > 0;
    }

    private static HeadInfo helperFor(LivingEntity mob) {
        // Server-side: resolve geometry (and the mob's chosen variant) from the authoritative server
        // config store, not the client one — getHelper reads ClientEyeConfigs, which is empty on a
        // dedicated server.
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return HeadInfo.serverHelper(type, mob, EyeState.getVariantRoll(mob));
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof LivingEntity mob)) {
            return;
        }

        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        // The slimy eye owns the click outright (see the class javadoc for why it's dispatched here).
        if (stack.getItem() instanceof SlimyEyeItem) {
            if (event.getLevel().isClientSide()) {
                // Eligibility is server-authoritative config; the client can't decide it here. It
                // already previews the verdict through EyeInspectIndicator, so swing and let the
                // server rule — cancelling locally also keeps the client from playing the target's
                // own reaction (a horse rearing) that the server never runs.
                consume(event, InteractionResult.SUCCESS);
            } else {
                consume(event, SlimyEyeItem.applyToTarget(stack, player, mob));
            }
            return;
        }

        if (event.getLevel().isClientSide()) {
            return;
        }

        // Shears (with Optometrist) are the non-lethal harvest.
        if (stack.getItem() instanceof ShearsItem && EyeState.hasEyes(mob) && hasOptometrist(stack)) {
            harvest(event, player, mob, stack);
        }
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
        if (!(weapon.getItem() instanceof ShearsItem)) {
            return;
        }

        if (mob.getRandom().nextInt(100) >= ServerConfig.HARVEST_ON_KILL_PERCENT.get()) {
            return;
        }

        HeadInfo helper = helperFor(mob);
        if (!helper.hasConfig()) {
            return; // no geometry to sample an appearance from
        }

        ItemStack drop = buildEyeDrop(helper, EyeState.readProperties(mob));
        ItemEntity entity = new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), drop);
        entity.setDefaultPickUpDelay();
        event.getDrops().add(entity);

        weapon.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
    }
}
