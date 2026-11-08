package com.github.crittscott.somegoogly.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;

import java.util.function.Consumer;

/**
 * NeoForge GameTest entry points for {@link PickerAuthGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class PickerAuthGameTests {

    private PickerAuthGameTests() {
    }

    public static void bulkSpawnContinuesBeyondNearbyChunks(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.bulkSpawnContinuesBeyondNearbyChunks(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    private static void checkRiderInsertion(GameTestHelper helper, boolean refuseLast) {
        List<Entity> members = new ArrayList<>();
        Consumer<FinalizeSpawnEvent> riders = event -> {
            if (event.getEntity().getType() == EntityTypes.COW && members.isEmpty()) {
                PickerSpawnServiceGameTestsLogic.addCommandRiders(helper, event.getEntity(), members);
            }
        };
        Consumer<EntityJoinLevelEvent> refusal = event -> {
            if (refuseLast && members.size() == 3 && event.getEntity() == members.get(2)) {
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(riders);
        NeoForge.EVENT_BUS.addListener(refusal);
        try {
            PickerSpawnServiceGameTestsLogic.bulkSpawnRiderInsertion(
                    helper, FakePlayerFactory.getMinecraft(helper.getLevel()), members, refuseLast);
        } finally {
            NeoForge.EVENT_BUS.unregister(refusal);
            NeoForge.EVENT_BUS.unregister(riders);
        }
    }

    public static void entityTargetingStopsAtWalls(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.entityTargetingStopsAtWalls(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void pickerCommandsFailForSurvivalOperators(GameTestHelper helper) {
        PickerAuthGameTestsLogic.pickerCommandsFailForSurvivalOperators(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void pickerRequestsThrottlePerTick(GameTestHelper helper) {
        PickerAuthGameTestsLogic.pickerRequestsThrottlePerTick(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void refusedFinalizationLeavesTerrain(GameTestHelper helper) {
        int[] refused = {0};
        Consumer<FinalizeSpawnEvent> refusal = event -> {
            refused[0]++;
            event.setSpawnCancelled(true);
        };
        NeoForge.EVENT_BUS.addListener(refusal);
        try {
            PickerSpawnServiceGameTestsLogic.refusedBulkSpawnLeavesTerrain(
                    helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
            helper.assertTrue(refused[0] == 2, Component.literal("both the land and water candidate reach the refusing listener"));
            helper.succeed();
        } finally {
            NeoForge.EVENT_BUS.unregister(refusal);
        }
    }

    public static void refusedInsertionLeavesTerrain(GameTestHelper helper) {
        int[] refused = {0};
        Consumer<EntityJoinLevelEvent> refusal = event -> {
            if (event.getEntity().getType() == EntityTypes.COW
                    || event.getEntity().getType() == EntityTypes.SQUID) {
                refused[0]++;
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(refusal);
        try {
            PickerSpawnServiceGameTestsLogic.refusedBulkSpawnLeavesTerrain(
                    helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
            helper.assertTrue(refused[0] == 2, Component.literal("both the land and water candidate reach the refusing listener"));
            helper.succeed();
        } finally {
            NeoForge.EVENT_BUS.unregister(refusal);
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
