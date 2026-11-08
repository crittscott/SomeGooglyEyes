package com.github.crittscott.somegoogly.gametest;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.listener.EventListener;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Forge GameTest entry points for {@link PickerAuthGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class PickerAuthGameTests {

    private PickerAuthGameTests() {
    }

    public static void bulkSpawnContinuesBeyondNearbyChunks(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.bulkSpawnContinuesBeyondNearbyChunks(helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    private static void checkRiderInsertion(GameTestHelper helper, boolean refuseLast) {
        List<Entity> members = new ArrayList<>();
        Consumer<MobSpawnEvent.FinalizeSpawn> riders = event -> {
            if (event.getEntity().getType() == EntityTypes.COW && members.isEmpty()) {
                PickerSpawnServiceGameTestsLogic.addCommandRiders(helper, event.getEntity(), members);
            }
        };
        Predicate<EntityJoinLevelEvent> refusal = event ->
                refuseLast && members.size() == 3 && event.getEntity() == members.get(2);
        EventListener riderListener = MobSpawnEvent.FinalizeSpawn.BUS.addListener(riders);
        EventListener refusalListener = EntityJoinLevelEvent.BUS.addListener(refusal);
        try {
            PickerSpawnServiceGameTestsLogic.bulkSpawnRiderInsertion(
                    helper, SomeGooglyGameTests.survivalPlayer(helper), members, refuseLast);
        } finally {
            EntityJoinLevelEvent.BUS.removeListener(refusalListener);
            MobSpawnEvent.FinalizeSpawn.BUS.removeListener(riderListener);
        }
    }

    public static void entityTargetingStopsAtWalls(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.entityTargetingStopsAtWalls(helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    public static void pickerCommandsFailForSurvivalOperators(GameTestHelper helper) {
        var level = helper.getLevel();
        ServerPlayer player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "survival_op"), ClientInformation.createDefault()) {
            @Override
            public boolean isCreative() {
                return false;
            }
        };
        PickerAuthGameTestsLogic.pickerCommandsFailForSurvivalOperators(helper, player);
    }

    public static void pickerRequestsThrottlePerTick(GameTestHelper helper) {
        PickerAuthGameTestsLogic.pickerRequestsThrottlePerTick(
                helper, helper.makeMockServerPlayerInLevel());
    }

    public static void refusedFinalizationLeavesTerrain(GameTestHelper helper) {
        int[] refused = {0};
        Consumer<MobSpawnEvent.FinalizeSpawn> refusal = event -> {
            refused[0]++;
            event.setSpawnCancelled(true);
        };
        EventListener listener = MobSpawnEvent.FinalizeSpawn.BUS.addListener(refusal);
        try {
            PickerSpawnServiceGameTestsLogic.refusedBulkSpawnLeavesTerrain(
                    helper, SomeGooglyGameTests.survivalPlayer(helper));
            helper.assertTrue(refused[0] == 2, Component.literal("both the land and water candidate reach the refusing listener"));
            helper.succeed();
        } finally {
            MobSpawnEvent.FinalizeSpawn.BUS.removeListener(listener);
        }
    }

    public static void refusedInsertionLeavesTerrain(GameTestHelper helper) {
        int[] refused = {0};
        Predicate<EntityJoinLevelEvent> refusal = event -> {
            if (event.getEntity().getType() == EntityTypes.COW
                    || event.getEntity().getType() == EntityTypes.SQUID) {
                refused[0]++;
                return true;
            }
            return false;
        };
        EventListener listener = EntityJoinLevelEvent.BUS.addListener(refusal);
        try {
            PickerSpawnServiceGameTestsLogic.refusedBulkSpawnLeavesTerrain(
                    helper, SomeGooglyGameTests.survivalPlayer(helper));
            helper.assertTrue(refused[0] == 2, Component.literal("both the land and water candidate reach the refusing listener"));
            helper.succeed();
        } finally {
            EntityJoinLevelEvent.BUS.removeListener(listener);
        }
    }

    public static void refusedRiderLeavesTerrain(GameTestHelper helper) {
        checkRiderInsertion(helper, true);
        helper.succeed();
    }

    public static void riderChainSurvivesInsertion(GameTestHelper helper) {
        checkRiderInsertion(helper, false);
        helper.succeed();
    }

    public static void spawnAllHasAServerWideCooldown(GameTestHelper helper) {
        PickerAuthGameTestsLogic.spawnAllHasAServerWideCooldown(helper);
    }
}
