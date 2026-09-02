package com.github.crittscott.somegoogly.server;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.enchant.ModEnchantments;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.item.SlimyEyeItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** Loader-neutral implementation of applying and harvesting googly-eye items. */
public final class EyeItemService {

    private EyeItemService() {
    }

    /**
     * Right-click-on-entity entry point for both eye items. A Slimy Eye returns
     * {@link InteractionResult#SUCCESS} on the client and defers the mutation to the server; an
     * Optometrist-shears harvest runs server-side only, dropping one eye and clearing the mob's eyes
     * before returning {@code SUCCESS}. Every other case — plain shears, no Optometrist, an eyeless mob,
     * a mob with no config, or the client side of the shears path — returns {@link InteractionResult#PASS}
     * so the vanilla interaction is left to proceed.
     */
    public static InteractionResult interact(Player player, Level level, InteractionHand hand,
                                             LivingEntity mob) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof SlimyEyeItem) {
            return level.isClientSide() ? InteractionResult.SUCCESS
                    : SlimyEyeItem.applyToTarget(stack, player, mob);
        }
        if (level.isClientSide() || !(stack.getItem() instanceof ShearsItem)
                || !EyeState.hasEyes(mob) || !hasOptometrist(stack, level.registryAccess())) {
            return InteractionResult.PASS;
        }
        HeadInfo helper = helperFor(mob);
        if (!helper.hasConfig()) {
            return InteractionResult.PASS;
        }
        mob.spawnAtLocation(buildEyeDrop(helper, EyeState.readProperties(mob)));
        EyeState.disableAndClearProperties(mob);
        stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        return InteractionResult.SUCCESS;
    }

    /**
     * Attempt the shears-on-kill harvest, routing the resulting eye through the loader's death-drop
     * mechanism. Forge supplies its event collection; Fabric supplies the ordinary world-drop path.
     */
    public static void onDeath(LivingEntity mob, DamageSource source, Consumer<ItemStack> dropSink) {
        if (!EyeState.hasEyes(mob)
                || !(source.getEntity() instanceof Player player)
                || source.getDirectEntity() != player) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof ShearsItem)
                || mob.getRandom().nextInt(100) >= ServerConfig.HARVEST_ON_KILL_PERCENT.get()) {
            return;
        }
        HeadInfo helper = helperFor(mob);
        if (!helper.hasConfig()) {
            return;
        }
        dropSink.accept(buildEyeDrop(helper, EyeState.readProperties(mob)));
        weapon.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
    }

    private static boolean hasOptometrist(ItemStack stack, HolderLookup.Provider registries) {
        return EnchantmentHelper.getItemEnchantmentLevel(
                registries.lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(ModEnchantments.OPTOMETRIST),
                stack) > 0;
    }

    private static HeadInfo helperFor(LivingEntity mob) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return ServerEyeConfigs.resolve(type, mob, EyeState.getVariantRoll(mob));
    }

    private static ItemStack buildEyeDrop(HeadInfo helper, AppearanceOverride override) {
        AppearanceOverride harvested = helper.appearanceAt(0, 0).overlay(override).toOverride();
        return GooglyEyeItem.create(harvested, 1);
    }
}
