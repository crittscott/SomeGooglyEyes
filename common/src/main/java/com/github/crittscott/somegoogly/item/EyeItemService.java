package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.BoatEyeLayout;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.platform.ItemDestruction;
import com.github.crittscott.somegoogly.registry.ModContent;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/** Loader-neutral implementation of applying and harvesting googly-eye items. */
public final class EyeItemService {

    private EyeItemService() {
    }

    /** Whether a target can receive eyes, and why not; each side answers from the state it holds. */
    public enum Verdict {
        ALREADY_HAS, CAN_HAVE, CANNOT, ONLY_AS_ADULT, ONLY_AS_BABY
    }

    /**
     * The Slimy Eye apply verb on either side: a target whose {@link #verdictFor(Entity) verdict} is
     * {@link Verdict#CAN_HAVE} gains eyes carrying the stack's appearance on a freshly rolled placement variant,
     * consuming one eye. Any other target passes ({@code PASS}), so its own use (riding, trading, a chest) still
     * happens; another player {@link #mayAffect} protects refuses ({@code FAIL}). The client answers from the same
     * verdict, so its {@code SUCCESS} predicts exactly the server's application. The mob and boat paths
     * ({@link #interact}) and the sneak self-apply ({@code SlimyEyeItem#use}) route through here. Vanilla records
     * no item use for any of them, so this does.
     */
    public static InteractionResult applySlimyEye(ItemStack stack, Player player, Entity target) {
        if (verdictFor(target) != Verdict.CAN_HAVE) {
            return InteractionResult.PASS;
        }
        if (!mayAffect(player, target)) {
            return InteractionResult.FAIL;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        EyeState.enableWithProperties(target, EyeItemProperties.get(stack));
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        consumeOne(player, stack);
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0F, 1.0F);
        target.gameEvent(GameEvent.ENTITY_INTERACT, player);
        return InteractionResult.CONSUME;
    }

    /**
     * The Slimy Eye apply verb for a placed mob head whose {@link #verdictFor(BlockEntity) verdict}
     * {@link #useOnSkull} found {@link Verdict#CAN_HAVE}, server side, clicked on its {@code face}: the head gains
     * eyes carrying the stack's appearance on a freshly rolled placement variant, consuming one eye outside
     * creative. The player must be allowed to use the item on that block, as placing a block there would require;
     * otherwise it refuses ({@code FAIL}) and consumes nothing.
     */
    private static InteractionResult applySlimyEye(ItemStack stack, ServerPlayer player, BlockEntity skull,
                                                   Direction face) {
        BlockPos pos = skull.getBlockPos();
        if (!mayAlter(player, pos, face, stack)) {
            return InteractionResult.FAIL;
        }
        SkullEyeState.set(skull, new EyeState.Snapshot(true, player.getRandom().nextFloat(), EyeItemProperties.get(stack)));
        recordBlockUse(player, pos, stack);
        consumeOne(player, stack);
        Level level = player.level();
        level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return InteractionResult.CONSUME;
    }

    /** A Googly Eye carrying the effective appearance of the first eye on head {@code headIndex}. */
    private static ItemStack buildEyeDrop(HeadInfo helper, int headIndex, AppearanceOverride override) {
        AppearanceOverride harvested = helper.appearanceAt(headIndex, 0).overlay(override).toOverride();
        return GooglyEyeItem.create(harvested, 1);
    }

    /** The definition of {@code type} at the given age that {@code level}'s side holds. */
    @Nullable
    private static RuntimeConfig config(Level level, ResourceLocation type, boolean baby) {
        return level.isClientSide() ? ClientEyeConfigs.get(type, baby) : ServerEyeConfigs.get(type, baby);
    }

    /**
     * Use up one of {@code stack}, unless {@code player} is in creative, reporting the stack to the loader when
     * that empties it: the loaders' click hooks that lead here end the click before the vanilla code that reports
     * a used-up item.
     */
    private static void consumeOne(Player player, ItemStack stack) {
        ItemStack original = stack.copy();
        stack.consume(1, player);
        if (stack.isEmpty()) {
            InteractionHand hand = player.getMainHandItem() == stack ? InteractionHand.MAIN_HAND
                    : InteractionHand.OFF_HAND;
            ItemDestruction.report(player, original, hand);
        }
    }

    /** Whether the {@code googlyEyesEnabled} master switch is on, as {@code level}'s side knows it. */
    private static boolean enabled(Level level) {
        return level.isClientSide() ? ClientEyeConfigs.googlyEyesEnabled() : ServerConfig.GOOGLY_EYES_ENABLED.get();
    }

    /**
     * The Optometrist harvest for an eyed placed mob head {@link #useOnSkull} accepted, server side, clicked on
     * its {@code face}: one Googly Eye carrying the head's effective appearance pops out of the block, the head
     * loses its eyes, the shears in {@code hand} lose one durability, the shearing sound plays, and
     * {@link GameEvent#SHEAR} is emitted. Returns {@link InteractionResult#PASS} when the player isn't allowed
     * to use the shears on that block.
     */
    private static InteractionResult harvestSkull(ServerPlayer player, InteractionHand hand, BlockEntity skull,
                                                  Direction face) {
        ItemStack shears = player.getItemInHand(hand);
        BlockPos pos = skull.getBlockPos();
        ResourceLocation entity = SkullEyeState.entityFor(skull.getBlockState().getBlock());
        if (!mayAlter(player, pos, face, shears)) {
            return InteractionResult.PASS;
        }
        Level level = player.level();
        EyeState.Snapshot state = SkullEyeState.snapshot(skull);
        HeadInfo helper = ServerEyeConfigs.resolveHead(entity, state.variantRoll());
        int[] parts = SkullEyeState.headParts(entity, helper);
        if (parts.length > 0) {
            Block.popResource(level, pos, buildEyeDrop(helper, parts[0], state.properties()));
        }
        SkullEyeState.set(skull, EyeState.Snapshot.NONE);
        recordBlockUse(player, pos, shears);
        wearShears(player, hand);
        level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.SHEAR, pos);
        return InteractionResult.CONSUME;
    }

    private static boolean hasOptometrist(ItemStack stack, HolderLookup.Provider registries) {
        return EnchantmentHelper.getItemEnchantmentLevel(
                registries.lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(ModContent.OPTOMETRIST),
                stack) > 0;
    }

    /** Whether {@code stack} harvests applied eyes: mod shears carrying Optometrist. */
    private static boolean isHarvestTool(ItemStack stack, HolderLookup.Provider registries) {
        return stack.is(ModContent.SHEARS) && hasOptometrist(stack, registries);
    }

    private static HeadInfo helperFor(LivingEntity mob) {
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        float roll = EyeState.getVariantRoll(mob);
        return mob.level().isClientSide() ? ClientEyeConfigs.resolve(type, mob.isBaby(), roll)
                : ServerEyeConfigs.resolve(type, mob.isBaby(), roll);
    }

    /**
     * Route supported entities through normal loader interaction hooks. Eligible application and harvesting are
     * predicted on the client, so success also consumes the target's own use, and on Fabric the click's second
     * interact packet: that packet's callback would otherwise find a just-harvested mob eyeless and let vanilla
     * shear it. Irrelevant actions pass through to vanilla. Spectators pass here and at the other use entry
     * points because the loaders' use hooks can fire before vanilla's spectator test.
     */
    public static InteractionResult interact(Player player, Level level, InteractionHand hand,
                                             Entity target) {
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (BoatEyeLayout.supports(target)) {
            return interactBoat(player, level, hand, (Boat) target);
        }
        if (!(target instanceof LivingEntity mob)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof SlimyEyeItem) {
            return applySlimyEye(stack, player, mob);
        }
        if (!isHarvestTool(stack, level.registryAccess()) || !EyeState.hasEyes(mob) || !enabled(level)) {
            return InteractionResult.PASS;
        }
        HeadInfo helper = helperFor(mob);
        if (!helper.hasConfig()) {
            return InteractionResult.PASS;
        }
        if (!mayAffect(player, mob)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        shearEyes(mob, player, hand, buildEyeDrop(helper, 0, EyeState.readProperties(mob)));
        return InteractionResult.CONSUME;
    }

    /** Vanilla boat use, after the loader's normal protection listeners have accepted the interaction. */
    private static InteractionResult interactBoat(Player player, Level level, InteractionHand hand, Boat boat) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof SlimyEyeItem) {
            return applySlimyEye(stack, player, boat);
        }
        if (!isHarvestTool(stack, level.registryAccess())) {
            return InteractionResult.PASS;
        }
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(boat.getType());
        RuntimeConfig config = config(level, type, false);
        if (!enabled(level) || !BoatEyeLayout.isEligible(boat, config) || !EyeState.hasEyes(boat)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        HeadInfo helper = BoatEyeLayout.resolve(boat, config, EyeState.getVariantRoll(boat));
        shearEyes(boat, player, hand, buildEyeDrop(helper, 0, EyeState.readProperties(boat)));
        return InteractionResult.CONSUME;
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
        ResourceLocation entity = SkullEyeState.entityFor(stack);
        if (entity != null && entity.equals(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()))
                && SkullEyeState.canHaveEyes(entity, ServerEyeConfigs.get(entity, false))
                && SkullEyeState.itemSnapshot(stack) == null) {
            SkullEyeState.stampItem(stack, EyeState.snapshot(mob));
        }
    }

    /**
     * Whether {@code actor} may change {@code target}'s eyes: anyone but another player, or one it could hurt, so
     * eyes can't be put on or taken off a teammate or anyone in a PvP-off world. The client doesn't know the
     * server's PvP setting; its answer only predicts the click.
     */
    private static boolean mayAffect(Player actor, Entity target) {
        return !(target instanceof Player victim) || victim == actor || actor.canHarmPlayer(victim);
    }

    /**
     * Whether {@code player} may use {@code stack} on the block at {@code pos}, clicked on its {@code face}, as
     * building there would need. {@code mayUseItemAt} tests the block behind the position it gets, so as for a
     * bucket that is the position in front of the clicked face.
     */
    private static boolean mayAlter(Player player, BlockPos pos, Direction face, ItemStack stack) {
        return player.level().mayInteract(player, pos) && player.mayUseItemAt(pos.relative(face), face, stack);
    }

    /**
     * Attempt the shears-on-kill harvest, routing the resulting eye through each loader's death-loot
     * phase: NeoForge and Forge add it to the {@code LivingDropsEvent} collection, Fabric spawns it
     * from a {@code dropAllDeathLoot} mixin at the same point. Neither depends on {@code doMobLoot}, so the rule
     * is checked here.
     */
    public static void onDeath(LivingEntity mob, DamageSource source, Consumer<ItemStack> dropSink) {
        if (!ServerConfig.GOOGLY_EYES_ENABLED.get() || !EyeState.hasEyes(mob)
                || !mob.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)
                || !source.is(DamageTypes.PLAYER_ATTACK) || !(source.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (!weapon.is(ModContent.SHEARS)
                || !ServerConfig.roll(mob.getRandom(), ServerConfig.HARVEST_ON_KILL_PERCENT.get())) {
            return;
        }
        HeadInfo helper = helperFor(mob);
        if (!helper.hasConfig()) {
            return;
        }
        dropSink.accept(buildEyeDrop(helper, 0, EyeState.readProperties(mob)));
        // The attack's own broken-weapon report has already looked at the stack by the time death loot runs.
        wearShears(player, InteractionHand.MAIN_HAND);
        mob.gameEvent(GameEvent.SHEAR, player);
    }

    /**
     * Report {@code stack}'s use on the block at {@code pos} to statistics and advancements; the head paths run
     * from the loaders' block-click hooks, ahead of the vanilla code that does this for an item's {@code useOn}.
     * Call before the use spends the stack.
     */
    private static void recordBlockUse(ServerPlayer player, BlockPos pos, ItemStack stack) {
        CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(player, pos, stack);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
    }

    /**
     * Sneak + right-click air with shears to shear your own eyes off — the self-serve counterpart to
     * another player having applied a Slimy Eye to you. One Googly Eye drops carrying your effective
     * appearance and the shears lose one durability. Optometrist shears do it cleanly; plain shears
     * additionally deal a bare-handed hit's worth (base attack damage, whatever the main hand holds) of
     * {@link ModContent#SELF_SHEAR} damage, which has no
     * attacker so PvP and team rules don't cancel it. Returns {@link InteractionResult#PASS}
     * when it doesn't apply (spectating, not sneaking, not shears, no eyes, {@code googlyEyesEnabled} off) so
     * the vanilla item use proceeds.
     */
    public static InteractionResult selfRemoveWithShears(Player player, InteractionHand hand) {
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!enabled(player.level()) || !player.isShiftKeyDown()
                || !stack.is(ModContent.SHEARS) || !EyeState.hasEyes(player)) {
            return InteractionResult.PASS;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        boolean clean = hasOptometrist(stack, player.level().registryAccess());
        HeadInfo helper = helperFor(player);
        shearEyes(player, player, hand,
                helper.hasConfig() ? buildEyeDrop(helper, 0, EyeState.readProperties(player)) : null);
        if (!clean) {
            player.hurt(new DamageSource(player.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                            .getOrThrow(ModContent.SELF_SHEAR)),
                    (float) player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
        }
        return InteractionResult.CONSUME;
    }

    /**
     * The shared outcome of shearing eyes off {@code target} with the shears in {@code actor}'s {@code hand}:
     * drop {@code drop} (if any) at the target, clear its eyes, wear the shears, play the shearing sound,
     * and emit {@link GameEvent#SHEAR}.
     */
    private static void shearEyes(Entity target, Player actor, InteractionHand hand, @Nullable ItemStack drop) {
        if (drop != null) {
            target.spawnAtLocation(drop);
        }
        EyeState.disableAndClearProperties(target);
        wearShears(actor, hand);
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 1.0F, 1.0F);
        target.gameEvent(GameEvent.SHEAR, actor);
    }

    /**
     * Right-click-on-block entry point for both eye items aimed at a placed mob head: a Slimy Eye on a head whose
     * {@link #verdictFor(BlockEntity) verdict} is {@link Verdict#CAN_HAVE} goes to
     * {@link #applySlimyEye(ItemStack, ServerPlayer, BlockEntity, Direction)}, Optometrist shears on an eyed
     * head, with eyes enabled, to {@link #harvestSkull}. Both sides decide those cases from the state they hold,
     * and the client answers {@link InteractionResult#SUCCESS} in exactly them, so it neither runs nor sends the
     * item's own use on top (a sneaking player's self-apply or self-shear). Anything else, including plain
     * shears, an eyed head, or a spectator's click, returns {@link InteractionResult#PASS}. The
     * Slimy Eye comes here rather than through {@code Item#useOn} for the reason a mob's click does: a head block
     * can have a use of its own that takes the click first, as Twilight Forest's trophies do.
     */
    public static InteractionResult useOnSkull(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }
        BlockEntity skull = level.getBlockEntity(hit.getBlockPos());
        if (!(skull instanceof SkullEyeState.Holder)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        boolean slimy = stack.getItem() instanceof SlimyEyeItem;
        if (slimy ? verdictFor(skull) != Verdict.CAN_HAVE
                : !isHarvestTool(stack, level.registryAccess()) || !enabled(level)
                        || SkullEyeState.entityFor(skull.getBlockState().getBlock()) == null
                        || !SkullEyeState.hasEyes(skull)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerPlayer serverPlayer = (ServerPlayer) player;
        return slimy ? applySlimyEye(stack, serverPlayer, skull, hit.getDirection())
                : harvestSkull(serverPlayer, hand, skull, hit.getDirection());
    }

    /** Whether the entity {@code target} can receive eyes, by what its side knows. */
    public static Verdict verdictFor(Entity target) {
        Level level = target.level();
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (!enabled(level)) {
            return Verdict.CANNOT;
        }
        if (target instanceof LivingEntity living) {
            boolean baby = living.isBaby();
            if (RuntimeConfig.isUsable(config(level, type, baby))) {
                return EyeState.hasEyes(target) ? Verdict.ALREADY_HAS : Verdict.CAN_HAVE;
            }
            if (RuntimeConfig.isUsable(config(level, type, !baby))) {
                return baby ? Verdict.ONLY_AS_ADULT : Verdict.ONLY_AS_BABY;
            }
            return Verdict.CANNOT;
        }
        if (target instanceof Boat boat && BoatEyeLayout.isEligible(boat, config(level, type, false))) {
            return EyeState.hasEyes(target) ? Verdict.ALREADY_HAS : Verdict.CAN_HAVE;
        }
        return Verdict.CANNOT;
    }

    /**
     * Whether the placed head {@code skull} can receive eyes, by what its side knows: its mob needs an adult
     * definition, which the head borrows, with eyes the head shows.
     */
    public static Verdict verdictFor(BlockEntity skull) {
        Level level = skull.getLevel();
        ResourceLocation entity = SkullEyeState.entityFor(skull.getBlockState().getBlock());
        if (!enabled(level) || entity == null
                || !SkullEyeState.canHaveEyes(entity, config(level, entity, false))) {
            return Verdict.CANNOT;
        }
        return SkullEyeState.hasEyes(skull) ? Verdict.ALREADY_HAS : Verdict.CAN_HAVE;
    }

    /** Spend one durability of the shears in {@code hand}, reporting them to the loader if that breaks them. */
    private static void wearShears(Player player, InteractionHand hand) {
        ItemStack shears = player.getItemInHand(hand);
        ItemStack original = shears.copy();
        shears.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        if (shears.isEmpty()) {
            ItemDestruction.report(player, original, hand);
        }
    }
}
