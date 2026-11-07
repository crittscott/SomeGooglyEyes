package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.client.ClientEyeRuntime;
import com.github.crittscott.somegoogly.client.PresentationEyes;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.config.EyeConfigLimits;
import com.github.crittscott.somegoogly.config.EyeConfigModel.HeadConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.BoatEyeLayout;
import com.github.crittscott.somegoogly.eye.EyeDefinition;
import com.github.crittscott.somegoogly.eye.EyePlacement;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorInstance;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeInfluence;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeAppearance;
import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.item.EyeItemProperties;
import com.github.crittscott.somegoogly.network.EyeStatePacket;
import com.github.crittscott.somegoogly.registry.ModContent;
import com.github.crittscott.somegoogly.item.EyeItemService;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;

/** AbstractBoat state and ambient checks; composed poses, event ordering, and remote tracking need client checks. */
public final class BoatEyeGameTestsLogic {

    private static final int OBSERVATION_TICKS = 400;
    private static final UUID SEED = new UUID(11L, 19L);

    private BoatEyeGameTestsLogic() {
    }

    /** In game: place every wood's boat and chest boat; they start eyeless and retain eyes only after application. */
    public static void boatsBeginEyelessAndSelectGeometry(GameTestHelper helper) {
        for (var type : BoatEyeLayout.TYPES) {
            AbstractBoat boat = Objects.requireNonNull(type.create(helper.getLevel(), EntitySpawnReason.LOAD));
            helper.assertTrue(!EyeState.hasEyes(boat), "placement never gives a boat random eyes");
            var config = ServerEyeConfigs.get(EntityType.getKey(type), false);
            helper.assertTrue(BoatEyeLayout.isEligible(boat, config), "each vanilla wood and cargo type has a definition");
            var layout = BoatEyeLayout.resolve(boat, config, 0.5F);
            helper.assertTrue(layout.getHeadCount() == 1 && layout.getEyeCount(0) == 2,
                    "the matching geometry carries two eyes total");
            helper.assertTrue(layout.getAttachToken(0).equals(BoatEyeLayout.attachment(boat)), "the correct hull/raft geometry is selected");
        }
        helper.assertTrue(!BoatEyeLayout.supports(Objects.requireNonNull(EntityType.COW.create(helper.getLevel(), EntitySpawnReason.LOAD))),
                "the boat path excludes other entity families");
        helper.succeed();
    }

    /** In game: a datapack's paired eyes cross; the bundled opposite-side eyes and unpaired eyes never cross. */
    public static void boatsCrossOnlyWithConfiguredPartners(GameTestHelper helper) {
        AbstractBoat boat = Objects.requireNonNull(EntityType.OAK_BOAT.create(helper.getLevel(), EntitySpawnReason.LOAD));
        var source = ServerEyeConfigs.get(EntityType.getKey(EntityType.OAK_BOAT), false);
        var state = new EyeState.Snapshot(true, 0F, AppearanceOverride.EMPTY);
        HeadConfig pair = new HeadConfig();
        pair.attachPoint = "hull";
        pair.eyes = List.of(
                new EyeDefinition(new EyePlacement(new Vec3(0.3D, 0D, -0.63D), 0.85F, 0.6F, 1F, 90F, 270F, 1),
                        EyeAppearance.DEFAULT),
                new EyeDefinition(new EyePlacement(new Vec3(0.7D, 0D, -0.63D), 0.85F, 0.6F, 1F, 90F, 270F, 0),
                        EyeAppearance.DEFAULT));
        Variant pairVariant = new Variant();
        pairVariant.heads = List.of(pair);
        RuntimeConfig pairConfig = new RuntimeConfig();
        pairConfig.variants = List.of(pairVariant);
        var paired = PresentationEyes.create(SEED, EntityType.getKey(EntityType.OAK_BOAT), state,
                BoatEyeLayout.resolve(boat, pairConfig, 0F));
        var sides = PresentationEyes.create(SEED, EntityType.getKey(EntityType.OAK_BOAT), state,
                BoatEyeLayout.resolve(boat, source, 0F));
        var cross = settings(List.of(EyeBehavior.CROSS_EYE));
        boolean crossed = false;
        for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
            paired.tick(tick, cross);
            crossed |= paired.active != null;
            sides.tick(tick, cross);
            helper.assertTrue(sides.active == null, "opposite-side eyes have no cross-eye partner");
        }
        helper.assertTrue(crossed, "a configured pair can cross");
        var influence = new EyeInfluence();
        var crossing = BehaviorInstance.start(
                EyeBehavior.CROSS_EYE, paired.helper, EyeBehavior.CROSS_EYE.defaultDuration(), 1L);
        crossing.age = crossing.duration / 2;
        EyeBehavior.CROSS_EYE.influence(crossing, 0, 0, influence);
        float firstAnchor = influence.anchorX;
        influence.reset();
        EyeBehavior.CROSS_EYE.influence(crossing, 0, 1, influence);
        helper.assertTrue(firstAnchor * influence.anchorX < 0F, "the paired pupils converge from opposite sides");
        for (int count : List.of(1, 2)) {
            HeadConfig head = new HeadConfig();
            head.attachPoint = "hull";
            head.eyes = java.util.stream.IntStream.range(0, count).mapToObj(index ->
                    new EyeDefinition(new EyePlacement(new Vec3(index, 0D, 0D), 1F, 0.6F, 1F, 90F, 180F, -1),
                            EyeAppearance.DEFAULT)).toList();
            Variant variant = new Variant();
            variant.heads = List.of(head);
            RuntimeConfig config = new RuntimeConfig();
            config.variants = List.of(variant);
            var eyes = PresentationEyes.create(SEED, EntityType.getKey(EntityType.OAK_BOAT), state, BoatEyeLayout.resolve(boat, config, 0F));
            for (int tick = 0; tick < OBSERVATION_TICKS; tick++) {
                eyes.tick(tick, cross);
                helper.assertTrue(eyes.active == null, "unpaired eyes cannot schedule cross-eye");
            }
        }
        helper.succeed();
    }

    /** In game: pause beside an eyed boat, look away, then return; pause retains pupils and unseen boats retire. */
    public static void boatsEvictIdleSimulations(GameTestHelper helper) {
        var definitions = ClientEyeConfigs.all();
        var behaviors = ClientEyeConfigs.behaviors();
        boolean enabled = ClientEyeConfigs.googlyEyesEnabled();
        AbstractBoat boat = Objects.requireNonNull(EntityType.OAK_BOAT.create(helper.getLevel(), EntitySpawnReason.LOAD));
        try {
            ClientEyeConfigs.replaceAll(ServerEyeConfigs.all());
            ClientEyeConfigs.applySettings(true, BehaviorSettings.DEFAULTS);
            EyeState.applySnapshot(boat, new EyeState.Snapshot(true, 0F, AppearanceOverride.EMPTY));
            ClientEyeRuntime.BoatSimulations cache = new ClientEyeRuntime.BoatSimulations();
            var eyes = cache.get(boat);
            var blink = settings(List.of(EyeBehavior.BLINK));
            cache.tick(1L, blink);
            helper.assertTrue(eyes.active != null && eyes.active.age == 1, "recently rendered boats simulate");
            cache.tick(2L, blink);
            helper.assertTrue(eyes.active.age == 1, "unrendered boats stop simulating after their grace tick");
            for (int tick = 0; tick < 12; tick++) {
                cache.tick(3L + tick, blink);
            }
            var recreated = cache.get(boat);
            helper.assertTrue(recreated != eyes, "boats unseen past the idle limit retire their pupils");
            cache.tick(100001L, blink);
            helper.assertTrue(recreated.active != null && recreated.active.age == 1,
                    "a reappearing boat follows current expression time without replaying missed slots");
            EyeState.Snapshot glowing = new EyeState.Snapshot(true, 0F, AppearanceOverride.EMPTY.withGlow(true));
            EyeState.applySnapshot(boat, glowing);
            cache.onStateChanged(boat, glowing);
            cache.tick(100002L, blink);
            helper.assertTrue(cache.get(boat) != recreated, "unrendered state changes invalidate cached pupils");
            recreated = cache.get(boat);
            cache.clear();
            helper.assertTrue(cache.get(boat) != recreated, "clearing drops every cached boat");
            boat.discard();
            cache.tick(100003L, blink);
            helper.assertTrue(cache.get(boat) == null, "removed boats cannot retain or rebuild a simulation");
        } finally {
            ClientEyeConfigs.replaceAll(definitions);
            ClientEyeConfigs.applySettings(enabled, behaviors);
        }
        helper.succeed();
    }

    /** In game: with ambient expressions off, row and turn an eyed boat; its pupils lag motion and settle under gravity. */
    public static void boatsFollowGravityAndOwnerMotion(GameTestHelper helper) {
        var definitions = ClientEyeConfigs.all();
        var behaviors = ClientEyeConfigs.behaviors();
        boolean enabled = ClientEyeConfigs.googlyEyesEnabled();
        AbstractBoat moving = Objects.requireNonNull(EntityType.OAK_BOAT.create(helper.getLevel(), EntitySpawnReason.LOAD));
        AbstractBoat still = Objects.requireNonNull(EntityType.OAK_BOAT.create(helper.getLevel(), EntitySpawnReason.LOAD));
        moving.setUUID(SEED);
        still.setUUID(SEED);
        try {
            ClientEyeConfigs.replaceAll(ServerEyeConfigs.all());
            ClientEyeConfigs.applySettings(true, BehaviorSettings.NONE);
            var state = new EyeState.Snapshot(true, 0F, AppearanceOverride.EMPTY);
            EyeState.applySnapshot(moving, state);
            EyeState.applySnapshot(still, state);
            ClientEyeRuntime.BoatSimulations cache = new ClientEyeRuntime.BoatSimulations();
            var movingEyes = cache.get(moving);
            var stillEyes = cache.get(still);
            for (var eyes : List.of(movingEyes, stillEyes)) {
                for (int index = 0; index < eyes.eyes[0].length; index++) {
                    var hullPose = new Matrix3f().scaling(-1F, -1F, 1F)
                            .rotate(eyes.helper.placementAt(0, index).orientation());
                    eyes.eyes[0][index].capturePose(hullPose);
                }
            }
            for (int tick = 0; tick < 8; tick++) {
                cache.get(moving);
                cache.get(still);
                cache.tick(tick, BehaviorSettings.NONE);
            }
            helper.assertTrue(movingEyes.eyes[0][0].deltaY < -0.5F
                            && movingEyes.eyes[0][1].deltaY < -0.5F, "both side pupils settle under hull-relative world gravity");
            moving.setPos(moving.getX() + 0.05D, moving.getY(), moving.getZ());
            cache.get(moving);
            cache.get(still);
            cache.tick(8L, BehaviorSettings.NONE);
            helper.assertTrue(Math.abs(movingEyes.eyes[0][0].deltaX - stillEyes.eyes[0][0].deltaX) > 0.2F,
                    "boat translation reaches pupil physics");
            helper.assertTrue(movingEyes.eyes[0][0].deltaX * movingEyes.eyes[0][1].deltaX < 0F,
                    "opposite-facing side eyes project acceleration into their own planes");
            moving.setYRot(35F);
            cache.get(moving);
            cache.tick(9L, BehaviorSettings.NONE);
            helper.assertTrue(movingEyes.eyes[0][0].rotationYaw == moving.getYRot(), "boat rotation reaches pupil physics");
        } finally {
            ClientEyeConfigs.replaceAll(definitions);
            ClientEyeConfigs.applySettings(enabled, behaviors);
        }
        helper.succeed();
    }

    /** In game: Optometrist shears remove a boat's eyes, drop their appearance, and cost one durability. */
    public static void boatsHarvestOneEye(GameTestHelper helper, Player player) {
        AbstractBoat boat = spawn(helper, EntityType.BAMBOO_CHEST_RAFT);
        var appearance = AppearanceOverride.EMPTY.withIrisColor(new EyeColor(0.3F, 0.7F, 0.2F)).withGlow(true);
        EyeState.enableWithProperties(boat, appearance);
        ItemStack shears = new ItemStack(Items.SHEARS);
        shears.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ModContent.OPTOMETRIST), 1);
        player.setItemInHand(InteractionHand.OFF_HAND, shears);
        helper.assertTrue(EyeItemService.interact(player, helper.getLevel(), InteractionHand.OFF_HAND, boat)
                == InteractionResult.CONSUME, "successful harvesting consumes chest interaction");
        helper.assertTrue(!EyeState.hasEyes(boat) && EyeState.readProperties(boat).isEmpty(), "harvest clears state");
        helper.assertTrue(shears.getDamageValue() == 1, "one durability is spent");
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, boat.getBoundingBox().inflate(1D),
                item -> item.getItem().is(ModContent.GOOGLY_EYE.get()));
        helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().getCount() == 1, "one eye drops");
        var harvested = EyeItemProperties.get(drops.getFirst().getItem());
        helper.assertTrue(harvested.iris().equals(appearance.iris()), "harvest preserves the overridden iris");
        helper.assertTrue(harvested.glow().equals(appearance.glow()), "harvest preserves the overridden glow");
        helper.assertTrue(harvested.cornea().orElseThrow().equals(EyeColor.WHITE),
                "harvest captures the configured white cornea when no override was applied");
        helper.succeed();
    }

    /** In game: let two eyed boats blink, pause, then resume after hiding one; pupils remain independent. */
    public static void boatsHaveIndependentAmbientAndPhysics(GameTestHelper helper) {
        AbstractBoat boat = Objects.requireNonNull(EntityType.OAK_BOAT.create(helper.getLevel(), EntitySpawnReason.LOAD));
        var config = ServerEyeConfigs.get(EntityType.getKey(EntityType.OAK_BOAT), false);
        var layout = BoatEyeLayout.resolve(boat, config, 0F);
        var state = new EyeState.Snapshot(true, 0F, AppearanceOverride.EMPTY);
        var first = PresentationEyes.create(SEED, EntityType.getKey(EntityType.OAK_BOAT), state, layout);
        var second = PresentationEyes.create(new UUID(23L, 31L), EntityType.getKey(EntityType.OAK_BOAT), state, layout);
        var blink = settings(List.of(EyeBehavior.BLINK));
        first.tick(1L, blink);
        second.tick(1L, blink);
        helper.assertTrue(first.eyes[0][0] != second.eyes[0][0], "boats never share mutable pupil state");
        first.eyes[0][0].deltaX = 0.75F;
        helper.assertTrue(second.eyes[0][0].deltaX != 0.75F, "one boat's physics cannot alter another");
        helper.assertTrue(first.active != null && first.active.age == 1, "absolute world time determines expression age");
        first.tick(100001L, blink);
        helper.assertTrue(first.active != null && first.active.age == 1, "time jumps select the current slot without a backlog");
        first.tick(100002L, BehaviorSettings.NONE);
        helper.assertTrue(first.active == null && Float.isFinite(first.eyes[0][0].deltaY),
                "ambient off clears expressions while leaving physics active");
        first.tick(100003L, settings(List.of()));
        helper.assertTrue(first.active == null, "an empty pool starts nothing");
        helper.succeed();
    }

    /** In game: apply eyes in creative; the Slimy Eye remains in your hand. */
    public static void boatsHonorCreativeConsumption(GameTestHelper helper, Player player) {
        boolean creative = player.getAbilities().instabuild;
        try {
            player.getAbilities().instabuild = true;
            AbstractBoat boat = spawn(helper, EntityType.OAK_BOAT);
            ItemStack stack = new ItemStack(ModContent.SLIMY_EYE.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            helper.assertTrue(EyeItemService.interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, boat)
                    == InteractionResult.CONSUME && stack.getCount() == 1 && EyeState.hasEyes(boat),
                    "creative application preserves the eye item");
        } finally {
            player.getAbilities().instabuild = creative;
        }
        helper.succeed();
    }

    /** In game: turn off the mod or disable the boat definition; using eyes or plain shears leaves the boat alone. */
    public static void boatsHonorInteractionGates(GameTestHelper helper, Player player) {
        AbstractBoat boat = spawn(helper, EntityType.OAK_BOAT);
        ItemStack slimy = new ItemStack(ModContent.SLIMY_EYE.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, slimy);
        boolean enabled = ServerConfig.GOOGLY_EYES_ENABLED.get();
        var configs = ServerEyeConfigs.all();
        try {
            ServerConfig.GOOGLY_EYES_ENABLED.set(false);
            helper.assertTrue(EyeItemService.interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, boat)
                    == InteractionResult.PASS, "the master switch preserves vanilla interaction");
            ServerConfig.GOOGLY_EYES_ENABLED.set(true);
            TestEyeConfigs.install(Map.of());
            helper.assertTrue(EyeItemService.interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, boat)
                    == InteractionResult.PASS, "missing definitions preserve vanilla interaction");
            helper.assertTrue(slimy.getCount() == 2 && !EyeState.hasEyes(boat), "refusals consume nothing");
        } finally {
            ServerConfig.GOOGLY_EYES_ENABLED.set(enabled);
            TestEyeConfigs.install(configs);
        }
        EyeState.setHasEyes(boat, true);
        ItemStack plain = new ItemStack(Items.SHEARS);
        player.setItemInHand(InteractionHand.MAIN_HAND, plain);
        helper.assertTrue(EyeItemService.interact(player, helper.getLevel(), InteractionHand.MAIN_HAND, boat)
                == InteractionResult.PASS && plain.getDamageValue() == 0 && EyeState.hasEyes(boat),
                "plain shears do not harvest or wear");
        helper.succeed();
    }

    /** In game: apply a dyed, glowing Slimy Eye in either hand, then try another; only the first is consumed. */
    public static void boatsReceiveAppearanceAndRefuseRestyling(GameTestHelper helper, Player player) {
        for (var type : List.of(EntityType.OAK_BOAT, EntityType.OAK_CHEST_BOAT)) {
            AbstractBoat boat = spawn(helper, type);
            for (InteractionHand hand : InteractionHand.values()) {
                EyeState.disableAndClearProperties(boat);
                ItemStack slimy = new ItemStack(ModContent.SLIMY_EYE.get(), 2);
                var appearance = AppearanceOverride.EMPTY.withIrisColor(new EyeColor(0.2F, 0.6F, 0.8F)).withGlow(true);
                EyeItemProperties.set(slimy, appearance);
                player.setItemInHand(hand, slimy);
                helper.assertTrue(EyeItemService.interact(player, helper.getLevel(), hand, boat) == InteractionResult.CONSUME,
                        "Slimy Eye application consumes the boat interaction");
                helper.assertTrue(EyeState.hasEyes(boat) && appearance.equals(EyeState.readProperties(boat)),
                        "the whole appearance transfers to the boat");
                helper.assertTrue(slimy.getCount() == 1, "survival application consumes one eye");
                var snapshot = EyeState.snapshot(boat);
                helper.assertTrue(EyeItemService.interact(player, helper.getLevel(), hand, boat) == InteractionResult.PASS,
                        "an already-eyed boat keeps vanilla use");
                helper.assertTrue(slimy.getCount() == 1 && snapshot.equals(EyeState.snapshot(boat)),
                        "a refused restyle spends and changes nothing");
            }
            boat.discard();
        }
        helper.succeed();
    }

    /** In game: try Slimy Eyes and Optometrist shears in spectator mode; the boat, tools, and stacks stay unchanged. */
    public static void boatsRefuseSpectatorMutation(GameTestHelper helper) {
        ServerPlayer spectator = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "boat_spectator"), ClientInformation.createDefault()) {
            @Override
            public boolean isSpectator() {
                return true;
            }
        };
        helper.assertTrue(spectator.isSpectator(), "the test player reports spectator status");
        AbstractBoat boat = spawn(helper, EntityType.OAK_BOAT);
        ItemStack slimy = new ItemStack(ModContent.SLIMY_EYE.get(), 2);
        spectator.setItemInHand(InteractionHand.MAIN_HAND, slimy);
        helper.assertTrue(EyeItemService.interact(spectator, helper.getLevel(), InteractionHand.MAIN_HAND, boat)
                == InteractionResult.PASS, "spectator interaction passes without mutating the boat");
        helper.assertTrue(EyeItemService.applySlimyEye(slimy, spectator, boat) == InteractionResult.FAIL
                && !EyeState.hasEyes(boat) && slimy.getCount() == 2, "the application verb also refuses spectators");
        EyeState.enableWithProperties(boat, AppearanceOverride.EMPTY.withGlow(true));
        var snapshot = EyeState.snapshot(boat);
        ItemStack shears = new ItemStack(Items.SHEARS);
        shears.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ModContent.OPTOMETRIST), 1);
        spectator.setItemInHand(InteractionHand.OFF_HAND, shears);
        helper.assertTrue(EyeItemService.interact(spectator, helper.getLevel(), InteractionHand.OFF_HAND, boat)
                == InteractionResult.PASS && snapshot.equals(EyeState.snapshot(boat)) && shears.getDamageValue() == 0,
                "spectators cannot harvest eyes or wear tools");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, boat.getBoundingBox().inflate(1D),
                item -> item.getItem().is(ModContent.GOOGLY_EYE.get())).isEmpty(), "spectator use creates no eye drops");
        helper.succeed();
    }

    /** In game: nearby boats keep separate simulations; restyling, hiding, resource reload, and unloading retire old pupils. */
    public static void boatsReplaceAndClearSimulations(GameTestHelper helper) {
        var definitions = ClientEyeConfigs.all();
        var behaviors = ClientEyeConfigs.behaviors();
        boolean enabled = ClientEyeConfigs.googlyEyesEnabled();
        AbstractBoat boat = Objects.requireNonNull(EntityType.OAK_BOAT.create(helper.getLevel(), EntitySpawnReason.LOAD));
        try {
            ClientEyeConfigs.replaceAll(ServerEyeConfigs.all());
            ClientEyeConfigs.applySettings(true, BehaviorSettings.DEFAULTS);
            EyeState.applySnapshot(boat, new EyeState.Snapshot(true, 0F, AppearanceOverride.EMPTY));
            var first = ClientEyeRuntime.boatEyes(boat);
            helper.assertTrue(first != null && first == ClientEyeRuntime.boatEyes(boat), "unchanged boats retain pupils");
            EyeState.applySnapshot(boat, new EyeState.Snapshot(true, 0F, AppearanceOverride.EMPTY.withGlow(true)));
            ClientEyeRuntime.onBoatState(boat, EyeState.snapshot(boat));
            var changed = ClientEyeRuntime.boatEyes(boat);
            helper.assertTrue(changed != first, "appearance changes replace pupils");
            AbstractBoat raft = Objects.requireNonNull(EntityType.BAMBOO_RAFT.create(helper.getLevel(), EntitySpawnReason.LOAD));
            EyeState.applySnapshot(raft, new EyeState.Snapshot(true, 0F, AppearanceOverride.EMPTY));
            var raftEyes = ClientEyeRuntime.boatEyes(raft);
            helper.assertTrue(raftEyes != changed && raftEyes.helper.getAttachToken(0).equals("raft"),
                    "raft entities have an independent geometry and simulation");
            var current = ClientEyeRuntime.boatEyes(boat);
            current.tick(1L, settings(List.of(EyeBehavior.BLINK)));
            ClientEyeRuntime.tick(helper.getLevel(), false);
            helper.assertTrue(current.active.age == 1, "pause leaves the cached expression unchanged");
            ClientEyeRuntime.onEntityRemoved(boat);
            helper.assertTrue(ClientEyeRuntime.boatEyes(boat) != current, "unloading retires the boat simulation");
            current = ClientEyeRuntime.boatEyes(boat);
            ClientEyeRuntime.clear();
            helper.assertTrue(ClientEyeRuntime.boatEyes(boat) != current, "resource reset retires the boat simulation");
            current = ClientEyeRuntime.boatEyes(boat);
            ClientEyeRuntime.onLevelChanged();
            helper.assertTrue(ClientEyeRuntime.boatEyes(boat) != current, "level replacement retires the boat simulation");
            boat.setInvisible(true);
            helper.assertTrue(ClientEyeRuntime.boatEyes(boat) == null, "invisible boats hide eyes and retire pupils");
            boat.setInvisible(false);
            ClientEyeConfigs.replaceAll(ServerEyeConfigs.all());
            ClientEyeConfigs.applySettings(false, BehaviorSettings.DEFAULTS);
            helper.assertTrue(ClientEyeRuntime.boatEyes(boat) == null, "master-off suppresses animation");
        } finally {
            ClientEyeRuntime.clear();
            ClientEyeConfigs.replaceAll(definitions);
            ClientEyeConfigs.applySettings(enabled, behaviors);
        }
        helper.succeed();
    }

    /** In game: switch boat definitions between hull/raft-only and disabled; only configured geometry accepts eyes. */
    public static void boatsRequireMatchingEnabledAttachments(GameTestHelper helper) {
        AbstractBoat boat = Objects.requireNonNull(EntityType.OAK_BOAT.create(helper.getLevel(), EntitySpawnReason.LOAD));
        var config = ServerEyeConfigs.get(EntityType.getKey(EntityType.OAK_BOAT), false);
        helper.assertTrue(EyeConfigLimits.validateSync(Map.of(EntityType.getKey(EntityType.OAK_BOAT),
                ServerEyeConfigs.all().get(EntityType.getKey(EntityType.OAK_BOAT)))) == null, "bundled geometry obeys sync limits");
        RuntimeConfig hullOnly = new RuntimeConfig();
        Variant variant = new Variant();
        variant.heads = config.variants.getFirst().heads.stream().filter(head -> head.attachPoint.equals("hull")).toList();
        hullOnly.variants = List.of(variant);
        boat = Objects.requireNonNull(EntityType.BAMBOO_RAFT.create(helper.getLevel(), EntitySpawnReason.LOAD));
        helper.assertTrue(!BoatEyeLayout.isEligible(boat, hullOnly), "a raft cannot borrow hull geometry");
        boat = Objects.requireNonNull(EntityType.OAK_BOAT.create(helper.getLevel(), EntitySpawnReason.LOAD));
        helper.assertTrue(BoatEyeLayout.isEligible(boat, hullOnly), "the hull retains its geometry");
        hullOnly.enabled = false;
        helper.assertTrue(!BoatEyeLayout.isEligible(boat, hullOnly), "disabled configurations cannot receive eyes");
        helper.succeed();
    }

    /** In game: eyed boats retain their layout and appearance through saving and chunk reload, including disabled worlds. */
    public static void boatsSaveAndSynchronizeSnapshots(GameTestHelper helper) {
        for (var type : List.of(EntityType.OAK_BOAT, EntityType.OAK_CHEST_BOAT, EntityType.BAMBOO_RAFT, EntityType.BAMBOO_CHEST_RAFT)) {
            AbstractBoat original = spawn(helper, type);
            EyeState.initialize(original, true, 0.375F);
            EyeState.setIrisTint(original, new EyeColor(0.2F, 0.4F, 0.6F));
            CompoundTag saved = original.saveWithoutId(new CompoundTag());
            AbstractBoat restored = Objects.requireNonNull(type.create(helper.getLevel(), EntitySpawnReason.LOAD));
            restored.load(saved);
            helper.assertTrue(EyeState.snapshot(original).equals(EyeState.snapshot(restored)), "native persistence retains a complete snapshot");
            helper.assertTrue(restored.getType() == original.getType(), "the wood/cargo/raft entity type survives saving");
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                EyeStatePacket.STREAM_CODEC.encode(buffer, new EyeStatePacket(original.getId(), EyeState.snapshot(original)));
                EyeStatePacket decoded = EyeStatePacket.STREAM_CODEC.decode(buffer);
                EyeState.applySnapshot(restored, decoded.snapshot());
                helper.assertTrue(decoded.entityId() == original.getId() && EyeState.snapshot(restored).equals(EyeState.snapshot(original)),
                        "the existing state payload carries boat state without a new wire shape");
            } finally {
                buffer.release();
            }
            original.discard();
        }
        helper.succeed();
    }

    private static BehaviorSettings settings(List<EyeBehavior> pool) {
        var defaults = BehaviorSettings.DEFAULTS;
        return new BehaviorSettings(true, 1, 1, pool, 0, false,
                defaults.swirlHealCooldownTicks(), defaults.ongoing());
    }

    private static AbstractBoat spawn(GameTestHelper helper, EntityType<? extends AbstractBoat> type) {
        AbstractBoat boat = Objects.requireNonNull(type.create(helper.getLevel(), EntitySpawnReason.LOAD));
        Vec3 at = helper.absoluteVec(new Vec3(2.5, 2D, 2.5));
        boat.moveTo(at.x, at.y, at.z);
        helper.assertTrue(helper.getLevel().addFreshEntity(boat), "the test boat is accepted");
        return boat;
    }
}
