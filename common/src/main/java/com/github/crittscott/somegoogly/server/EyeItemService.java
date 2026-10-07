package com.github.crittscott.somegoogly.server;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.item.SlimyEyeItem;
import com.github.crittscott.somegoogly.registry.ModContent;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import javax.annotation.Nullable;
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
     * a mob with no config, {@code googlyEyesEnabled} off, or the client side of the shears path —
     * returns {@link InteractionResult#PASS} so the vanilla interaction is left to proceed.
     */
    public static InteractionResult interact(Player player, Level level, InteractionHand hand,
                                             LivingEntity mob) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof SlimyEyeItem) {
            return level.isClientSide() ? InteractionResult.SUCCESS
                    : applySlimyEye(stack, (ServerPlayer) player, mob);
        }
        if (level.isClientSide() || !(stack.getItem() instanceof ShearsItem)
                || !EyeState.hasEyes(mob) || !hasOptometrist(stack, level.registryAccess())
                || !ServerConfig.GOOGLY_EYES_ENABLED.get()) {
            return InteractionResult.PASS;
        }
        HeadInfo helper = helperFor(mob);
        if (!helper.hasConfig()) {
            return InteractionResult.PASS;
        }
        shearEyes((ServerLevel) level, mob, player, hand, buildEyeDrop(helper, EyeState.readProperties(mob)));
        return InteractionResult.SUCCESS;
    }

    /**
     * The Slimy Eye apply verb, server side: an eyeless target passing the shared eligibility predicate
     * gains eyes carrying the stack's appearance on a freshly rolled placement variant, consuming one eye.
     * An already-eyed or ineligible target refuses ({@code FAIL}) and consumes nothing, as does any
     * target while {@code googlyEyesEnabled} is off — the master switch blocks hand application the same
     * as it blocks the at-spawn roll. Applying to another player additionally requires server PvP to be
     * enabled and {@code canHarmPlayer} to hold, so it can't be used to restyle a teammate or anyone in a
     * PvP-off world. Both the mob path ({@link #interact}) and the sneak self-apply
     * ({@code SlimyEyeItem#use}) route through here.
     */
    public static InteractionResult applySlimyEye(ItemStack stack, ServerPlayer player, LivingEntity target) {
        if (!ServerConfig.GOOGLY_EYES_ENABLED.get() || EyeState.hasEyes(target) || !ServerEyeConfigs.isEligible(target)) {
            return InteractionResult.FAIL;
        }
        if (target instanceof Player victim && victim != player) {
            if (!player.serverLevel().getServer().isPvpAllowed() || !player.canHarmPlayer(victim)) {
                return InteractionResult.FAIL;
            }
        }
        EyeState.enableWithProperties(target, EyeItemProperties.get(stack));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0F, 1.0F);
        target.gameEvent(GameEvent.ENTITY_INTERACT, player);
        return InteractionResult.SUCCESS;
    }

    /**
     * Attempt the shears-on-kill harvest, routing the resulting eye through each loader's death-loot
     * phase: NeoForge and Forge add it to the {@code LivingDropsEvent} collection, Fabric appends it
     * from a {@code dropCustomDeathLoot} mixin.
     */
    public static void onDeath(LivingEntity mob, DamageSource source, Consumer<ItemStack> dropSink) {
        if (!ServerConfig.GOOGLY_EYES_ENABLED.get() || !EyeState.hasEyes(mob)
                || !(source.getEntity() instanceof Player player)
                || source.getDirectEntity() != player) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof ShearsItem)
                || mob.getRandom().nextInt(ServerConfig.PERCENT_MAX) >= ServerConfig.HARVEST_ON_KILL_PERCENT.get()) {
            return;
        }
        HeadInfo helper = helperFor(mob);
        if (!helper.hasConfig()) {
            return;
        }
        dropSink.accept(buildEyeDrop(helper, EyeState.readProperties(mob)));
        weapon.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        mob.gameEvent(GameEvent.SHEAR, player);
    }

    /**
     * Sneak + right-click air with shears to shear your own eyes off — the self-serve counterpart to
     * another player having applied a Slimy Eye to you. One Googly Eye drops carrying your effective
     * appearance and the shears lose one durability. Optometrist shears do it cleanly; plain shears
     * additionally deal one melee hit's worth of {@link ModContent#SELF_SHEAR} damage, which has no
     * attacker so PvP and team rules don't cancel it. Returns {@link InteractionResult#PASS}
     * when it doesn't apply (not sneaking, not shears, no eyes, {@code googlyEyesEnabled} off) so the
     * vanilla item use proceeds.
     */
    public static InteractionResult selfRemoveWithShears(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!ServerConfig.GOOGLY_EYES_ENABLED.get() || !player.isShiftKeyDown()
                || !(stack.getItem() instanceof ShearsItem) || !EyeState.hasEyes(player)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel level = (ServerLevel) player.level();
        boolean clean = hasOptometrist(stack, level.registryAccess());
        HeadInfo helper = helperFor(player);
        shearEyes(level, player, player, hand,
                helper.hasConfig() ? buildEyeDrop(helper, EyeState.readProperties(player)) : null);
        if (!clean) {
            player.hurtServer(level, new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                            .getOrThrow(ModContent.SELF_SHEAR)),
                    (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * The shared outcome of shearing eyes off {@code target} with the shears in {@code actor}'s {@code hand}:
     * drop {@code drop} (if any) at the target, clear its eyes, wear the shears, play the shearing sound,
     * and emit {@link GameEvent#SHEAR}.
     */
    private static void shearEyes(ServerLevel level, LivingEntity target, Player actor, InteractionHand hand,
                                  @Nullable ItemStack drop) {
        if (drop != null) {
            target.spawnAtLocation(level, drop);
        }
        EyeState.disableAndClearProperties(target);
        actor.getItemInHand(hand).hurtAndBreak(1, actor, LivingEntity.getSlotForHand(hand));
        playShearSound(target);
        target.gameEvent(GameEvent.SHEAR, actor);
    }

    private static void playShearSound(LivingEntity target) {
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static boolean hasOptometrist(ItemStack stack, HolderLookup.Provider registries) {
        return EnchantmentHelper.getItemEnchantmentLevel(
                registries.lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(ModContent.OPTOMETRIST),
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
