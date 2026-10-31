package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;

import java.util.function.Consumer;

/**
 * Forge GameTest entry points for {@link PickerAuthGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class PickerAuthGameTests {

    private PickerAuthGameTests() {
    }

    public static void pickerRequestsThrottlePerTick(GameTestHelper helper) {
        PickerAuthGameTestsLogic.pickerRequestsThrottlePerTick(
                helper, helper.makeMockServerPlayerInLevel());
    }

    public static void spawnAllHasAServerWideCooldown(GameTestHelper helper) {
        PickerAuthGameTestsLogic.spawnAllHasAServerWideCooldown(helper);
    }

    public static void entityTargetingStopsAtWalls(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.entityTargetingStopsAtWalls(helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

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
            helper.assertTrue(refused[0] == 2, Component.literal("both the land and water candidate reach the refusing listener"));
            helper.succeed();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(refusal);
        }
    }

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
            helper.assertTrue(refused[0] == 2, Component.literal("both the land and water candidate reach the refusing listener"));
            helper.succeed();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(refusal);
        }
    }
}
