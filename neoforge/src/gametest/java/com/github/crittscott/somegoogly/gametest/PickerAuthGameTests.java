package com.github.crittscott.somegoogly.gametest;

import java.util.ArrayList;
import java.util.List;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.Consumer;

/**
 * NeoForge GameTest entry points for {@link PickerAuthGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PickerAuthGameTests {

    private static final String TEMPLATE = "empty";

    private PickerAuthGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void bulkSpawnContinuesBeyondNearbyChunks(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.bulkSpawnContinuesBeyondNearbyChunks(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    private static void checkRiderInsertion(GameTestHelper helper, boolean refuseLast) {
        List<Entity> members = new ArrayList<>();
        Consumer<FinalizeSpawnEvent> riders = event -> {
            if (event.getEntity().getType() == EntityType.COW && members.isEmpty()) {
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

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void entityTargetingStopsAtWalls(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.entityTargetingStopsAtWalls(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void pickerCommandsFailForSurvivalOperators(GameTestHelper helper) {
        PickerAuthGameTestsLogic.pickerCommandsFailForSurvivalOperators(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void pickerRequestsThrottlePerTick(GameTestHelper helper) {
        PickerAuthGameTestsLogic.pickerRequestsThrottlePerTick(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
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
            helper.assertTrue(refused[0] == 2, "both the land and water candidate reach the refusing listener");
            helper.succeed();
        } finally {
            NeoForge.EVENT_BUS.unregister(refusal);
        }
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void refusedInsertionLeavesTerrain(GameTestHelper helper) {
        int[] refused = {0};
        Consumer<EntityJoinLevelEvent> refusal = event -> {
            if (event.getEntity().getType() == EntityType.COW
                    || event.getEntity().getType() == EntityType.SQUID) {
                refused[0]++;
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(refusal);
        try {
            PickerSpawnServiceGameTestsLogic.refusedBulkSpawnLeavesTerrain(
                    helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
            helper.assertTrue(refused[0] == 2, "both the land and water candidate reach the refusing listener");
            helper.succeed();
        } finally {
            NeoForge.EVENT_BUS.unregister(refusal);
        }
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void refusedRiderLeavesTerrain(GameTestHelper helper) {
        checkRiderInsertion(helper, true);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void riderChainSurvivesInsertion(GameTestHelper helper) {
        checkRiderInsertion(helper, false);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void spawnAllHasAServerWideCooldown(GameTestHelper helper) {
        PickerAuthGameTestsLogic.spawnAllHasAServerWideCooldown(helper);
    }
}
