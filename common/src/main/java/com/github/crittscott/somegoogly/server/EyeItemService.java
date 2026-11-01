package com.github.crittscott.somegoogly.server;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.BoatEyeLayout;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.item.GooglyEyeItem;
import com.github.crittscott.somegoogly.item.SlimyEyeItem;
import com.github.crittscott.somegoogly.registry.ModContent;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Loader-neutral implementation of applying and harvesting googly-eye items. */
public final class EyeItemService {

    private EyeItemService() {
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
    public static InteractionResult applySlimyEye(ItemStack stack, ServerPlayer player, Entity target) {
        boolean eligible = target instanceof LivingEntity living ? ServerEyeConfigs.isEligible(living)
                : target instanceof AbstractBoat boat && BoatEyeLayout.isEligible(boat,
                        ServerEyeConfigs.get(BuiltInRegistries.ENTITY_TYPE.getKey(boat.getType()), false));
        if (player.isSpectator() || !ServerConfig.GOOGLY_EYES_ENABLED.get() || EyeState.hasEyes(target) || !eligible) {
            return InteractionResult.FAIL;
        }
        if (target instanceof Player victim && victim != player) {
            if (!player.level().isPvpAllowed() || !player.canHarmPlayer(victim)) {
                return InteractionResult.FAIL;
            }
        }
        EyeState.enableWithProperties(target, EyeItemProperties.get(stack));
        stack.consume(1, player);
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0F, 1.0F);
        target.gameEvent(GameEvent.ENTITY_INTERACT, player);
        return InteractionResult.SUCCESS;
    }

    /**
     * The Slimy Eye apply verb for a placed mob head, server side, clicked on its {@code face}: an eyeless head
     * whose mob has an adult definition gains eyes carrying the stack's appearance on a freshly rolled placement
     * variant, consuming one eye outside creative. The player must be allowed to use the item on that block, as
     * placing a block there would require. Otherwise, or while {@code googlyEyesEnabled} is off, it refuses
     * ({@code FAIL}) and consumes nothing.
     */
    public static InteractionResult applySlimyEye(ItemStack stack, ServerPlayer player, SkullBlockEntity skull,
                                                  Direction face) {
        BlockPos pos = skull.getBlockPos();
        if (!ServerConfig.GOOGLY_EYES_ENABLED.get() || !canHeadHaveEyes(skull) || SkullEyeState.hasEyes(skull)
                || !mayAlter(player, pos, face, stack)) {
            return InteractionResult.FAIL;
        }
        SkullEyeState.set(skull, new EyeState.Snapshot(true, player.getRandom().nextFloat(), EyeItemProperties.get(stack)));
        stack.consume(1, player);
        Level level = player.level();
        level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return InteractionResult.SUCCESS;
    }

    /** A Googly Eye carrying the effective appearance of the first eye on head {@code headIndex}. */
    private static ItemStack buildEyeDrop(HeadInfo helper, int headIndex, AppearanceOverride override) {
        AppearanceOverride harvested = helper.appearanceAt(headIndex, 0).overlay(override).toOverride();
        return GooglyEyeItem.create(harvested, 1);
    }

    /** Whether the placed head's mob has an adult definition to borrow with eyes the head shows. */
    private static boolean canHeadHaveEyes(SkullBlockEntity skull) {
        Identifier entity = SkullEyeState.entityFor(skull.getBlockState().getBlock());
        return entity != null && SkullEyeState.canHaveEyes(entity, ServerEyeConfigs.get(entity, false));
    }

    /**
     * The Optometrist harvest for a placed mob head, server side, clicked on its {@code face}: one Googly Eye
     * carrying the head's effective appearance pops out of the block, the head loses its eyes, the shears in
     * {@code hand} lose one durability, the shearing sound plays, and {@link GameEvent#SHEAR} is emitted. The
     * player must be allowed to use the shears on that block. Returns {@link InteractionResult#PASS} when it
     * doesn't apply (no eyes, not allowed, {@code googlyEyesEnabled} off).
     */
    public static InteractionResult harvestSkull(ServerPlayer player, InteractionHand hand, SkullBlockEntity skull,
                                                 Direction face) {
        ItemStack shears = player.getItemInHand(hand);
        BlockPos pos = skull.getBlockPos();
        Identifier entity = SkullEyeState.entityFor(skull.getBlockState().getBlock());
        if (!ServerConfig.GOOGLY_EYES_ENABLED.get() || entity == null || !SkullEyeState.hasEyes(skull)
                || !mayAlter(player, pos, face, shears)) {
            return InteractionResult.PASS;
        }
        ServerLevel level = player.level();
        EyeState.Snapshot state = SkullEyeState.snapshot(skull);
        HeadInfo helper = ServerEyeConfigs.resolveHead(entity, state.variantRoll());
        int[] parts = SkullEyeState.headParts(helper);
        if (parts.length > 0) {
            Block.popResource(level, pos, buildEyeDrop(helper, parts[0], state.properties()));
        }
        SkullEyeState.set(skull, EyeState.Snapshot.NONE);
        shears.hurtAndBreak(1, player, hand.asEquipmentSlot());
        level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.SHEAR, pos);
        return InteractionResult.SUCCESS;
    }

    private static boolean hasOptometrist(ItemStack stack, HolderLookup.Provider registries) {
        return EnchantmentHelper.getItemEnchantmentLevel(
                registries.lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(ModContent.OPTOMETRIST),
                stack) > 0;
    }

    private static HeadInfo helperFor(LivingEntity mob) {
        Identifier type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return ServerEyeConfigs.resolve(type, mob, EyeState.getVariantRoll(mob));
    }

    /**
     * Route supported entities through normal loader interaction hooks. Living entities retain their
     * item behavior; boats predict eligible application and harvesting on the client so success also
     * consumes boarding or cargo use. Irrelevant boat actions pass through to vanilla.
     */
    public static InteractionResult interact(Player player, Level level, InteractionHand hand,
                                             Entity target) {
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (BoatEyeLayout.supports(target)) {
            return interactBoat(player, level, hand, (AbstractBoat) target);
        }
        if (!(target instanceof LivingEntity mob)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof SlimyEyeItem) {
            return level.isClientSide() ? InteractionResult.SUCCESS
                    : applySlimyEye(stack, (ServerPlayer) player, mob);
        }
        if (level.isClientSide() || !stack.is(ModContent.SHEARS)
                || !EyeState.hasEyes(mob) || !hasOptometrist(stack, level.registryAccess())
                || !ServerConfig.GOOGLY_EYES_ENABLED.get()) {
            return InteractionResult.PASS;
        }
        HeadInfo helper = helperFor(mob);
        if (!helper.hasConfig()) {
            return InteractionResult.PASS;
        }
        shearEyes((ServerLevel) level, mob, player, hand, buildEyeDrop(helper, 0, EyeState.readProperties(mob)));
        return InteractionResult.SUCCESS;
    }

    /** Vanilla boat use, after the loader's normal protection listeners have accepted the interaction. */
    private static InteractionResult interactBoat(Player player, Level level, InteractionHand hand, AbstractBoat boat) {
        ItemStack stack = player.getItemInHand(hand);
        boolean slimy = stack.getItem() instanceof SlimyEyeItem;
        boolean optometrist = stack.is(ModContent.SHEARS) && hasOptometrist(stack, level.registryAccess());
        if (!slimy && !optometrist) {
            return InteractionResult.PASS;
        }
        Identifier type = BuiltInRegistries.ENTITY_TYPE.getKey(boat.getType());
        boolean enabled = level.isClientSide() ? ClientEyeConfigs.googlyEyesEnabled()
                : ServerConfig.GOOGLY_EYES_ENABLED.get();
        var config = level.isClientSide() ? ClientEyeConfigs.get(type, false) : ServerEyeConfigs.get(type, false);
        if (!enabled || !BoatEyeLayout.isEligible(boat, config)) {
            return InteractionResult.PASS;
        }
        if (slimy) {
            if (EyeState.hasEyes(boat)) {
                return InteractionResult.PASS;
            }
            return level.isClientSide() ? InteractionResult.SUCCESS
                    : applySlimyEye(stack, (ServerPlayer) player, boat);
        }
        if (!EyeState.hasEyes(boat)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            HeadInfo helper = BoatEyeLayout.resolve(boat, config, EyeState.getVariantRoll(boat));
            shearEyes((ServerLevel) level, boat, player, hand, buildEyeDrop(helper, 0, EyeState.readProperties(boat)));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Give a head item an eyed mob drops in its death loot the mob's whole eye state, when the head is the mob's
     * own kind ({@link SkullEyeState#entityFor}) and carries no eyes yet, so the placed head shows the mob's
     * arrangement and colors. Players are left out: their death drops are their inventory. Each loader routes
     * every stack of the death-loot phase here; any other stack is left alone.
     */
    public static void markHeadDrop(LivingEntity mob, ItemStack stack) {
        if (!ServerConfig.GOOGLY_EYES_ENABLED.get() || mob instanceof Player || !EyeState.hasEyes(mob)) {
            return;
        }
        Identifier entity = SkullEyeState.entityFor(stack);
        if (entity != null && entity.equals(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()))
                && SkullEyeState.canHaveEyes(entity, ServerEyeConfigs.get(entity, false))
                && SkullEyeState.itemSnapshot(stack) == null) {
            SkullEyeState.stampItem(stack, EyeState.snapshot(mob));
        }
    }

    /** Whether {@code player} may use {@code stack} on the block at {@code pos}, as building there would need. */
    private static boolean mayAlter(Player player, BlockPos pos, Direction face, ItemStack stack) {
        return player.level().mayInteract(player, pos) && player.mayUseItemAt(pos, face, stack);
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
        if (!weapon.is(ModContent.SHEARS)
                || mob.getRandom().nextInt(ServerConfig.PERCENT_MAX) >= ServerConfig.HARVEST_ON_KILL_PERCENT.get()) {
            return;
        }
        HeadInfo helper = helperFor(mob);
        if (!helper.hasConfig()) {
            return;
        }
        dropSink.accept(buildEyeDrop(helper, 0, EyeState.readProperties(mob)));
        weapon.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        mob.gameEvent(GameEvent.SHEAR, player);
    }

    private static void playShearSound(Entity target) {
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
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
                || !stack.is(ModContent.SHEARS) || !EyeState.hasEyes(player)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel level = (ServerLevel) player.level();
        boolean clean = hasOptometrist(stack, level.registryAccess());
        HeadInfo helper = helperFor(player);
        shearEyes(level, player, player, hand,
                helper.hasConfig() ? buildEyeDrop(helper, 0, EyeState.readProperties(player)) : null);
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
    private static void shearEyes(ServerLevel level, Entity target, Player actor, InteractionHand hand,
                                  @Nullable ItemStack drop) {
        if (drop != null) {
            target.spawnAtLocation(level, drop);
        }
        EyeState.disableAndClearProperties(target);
        actor.getItemInHand(hand).hurtAndBreak(1, actor, hand.asEquipmentSlot());
        playShearSound(target);
        target.gameEvent(GameEvent.SHEAR, actor);
    }

    /**
     * Right-click-on-block entry point for both eye items aimed at a placed mob head: a Slimy Eye goes to
     * {@link #applySlimyEye(ItemStack, ServerPlayer, SkullBlockEntity, Direction)}, Optometrist shears on an eyed
     * head to {@link #harvestSkull}. The client answers {@link InteractionResult#SUCCESS} in exactly those cases,
     * so it neither runs nor sends the item's own use on top (a sneaking player's self-apply or self-shear).
     * Anything else, including plain shears, returns {@link InteractionResult#PASS}.
     */
    public static InteractionResult useOnSkull(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        boolean slimy = stack.getItem() instanceof SlimyEyeItem;
        if (!slimy && !(stack.is(ModContent.SHEARS) && hasOptometrist(stack, level.registryAccess()))
                || !(level.getBlockEntity(hit.getBlockPos()) instanceof SkullBlockEntity skull)
                || !slimy && !SkullEyeState.hasEyes(skull)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer serverPlayer = (ServerPlayer) player;
        return slimy ? applySlimyEye(stack, serverPlayer, skull, hit.getDirection())
                : harvestSkull(serverPlayer, hand, skull, hit.getDirection());
    }
}
