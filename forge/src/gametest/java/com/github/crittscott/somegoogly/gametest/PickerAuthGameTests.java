package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.gametest.GameTestHolder;

import java.util.function.Consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Forge GameTest entry points for {@link PickerAuthGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
public final class PickerAuthGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    private PickerAuthGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void bulkSpawnContinuesBeyondNearbyChunks(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.bulkSpawnContinuesBeyondNearbyChunks(helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    private static void checkRiderInsertion(GameTestHelper helper, boolean refuseLast) {
        List<Entity> members = new ArrayList<>();
        Consumer<MobSpawnEvent.FinalizeSpawn> riders = event -> {
            if (event.getEntity().getType() == EntityType.COW && members.isEmpty()) {
                PickerSpawnServiceGameTestsLogic.addCommandRiders(helper, event.getEntity(), members);
            }
        };
        Consumer<EntityJoinLevelEvent> refusal = event -> {
            if (refuseLast && members.size() == 3 && event.getEntity() == members.get(2)) {
                event.setCanceled(true);
            }
        };
        MinecraftForge.EVENT_BUS.addListener(riders);
        MinecraftForge.EVENT_BUS.addListener(refusal);
        try {
            PickerSpawnServiceGameTestsLogic.bulkSpawnRiderInsertion(
                    helper, SomeGooglyGameTests.survivalPlayer(helper), members, refuseLast);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(refusal);
            MinecraftForge.EVENT_BUS.unregister(riders);
        }
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void entityTargetingStopsAtWalls(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.entityTargetingStopsAtWalls(helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void pickerCommandsFailForSurvivalOperators(GameTestHelper helper) {
        // The mock player always reports creative, so this needs a plain survival player.
        PickerAuthGameTestsLogic.pickerCommandsFailForSurvivalOperators(helper,
                new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                        new GameProfile(UUID.randomUUID(), "survival_operator"), ClientInformation.createDefault()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void pickerRequestsThrottlePerTick(GameTestHelper helper) {
        PickerAuthGameTestsLogic.pickerRequestsThrottlePerTick(
                helper, helper.makeMockServerPlayerInLevel());
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void refusedFinalizationLeavesTerrain(GameTestHelper helper) {
        int[] refused = {0};
        Consumer<MobSpawnEvent.FinalizeSpawn> refusal = event -> {
            refused[0]++;
            event.setSpawnCancelled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(refusal);
        try {
            PickerSpawnServiceGameTestsLogic.refusedBulkSpawnLeavesTerrain(
                    helper, SomeGooglyGameTests.survivalPlayer(helper));
            helper.assertTrue(refused[0] == 2, "both the land and water candidate reach the refusing listener");
            helper.succeed();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(refusal);
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
        MinecraftForge.EVENT_BUS.addListener(refusal);
        try {
            PickerSpawnServiceGameTestsLogic.refusedBulkSpawnLeavesTerrain(
                    helper, SomeGooglyGameTests.survivalPlayer(helper));
            helper.assertTrue(refused[0] == 2, "both the land and water candidate reach the refusing listener");
            helper.succeed();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(refusal);
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
