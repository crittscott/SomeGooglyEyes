package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
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

    public static void pickerRequestsThrottlePerTick(GameTestHelper helper) {
        PickerAuthGameTestsLogic.pickerRequestsThrottlePerTick(
                helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void spawnAllHasAServerWideCooldown(GameTestHelper helper) {
        PickerAuthGameTestsLogic.spawnAllHasAServerWideCooldown(helper);
    }

    public static void entityTargetingStopsAtWalls(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.entityTargetingStopsAtWalls(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
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
            helper.assertTrue(refused[0] == 2, Component.literal("both the land and water candidate reach the refusing listener"));
            helper.succeed();
        } finally {
            NeoForge.EVENT_BUS.unregister(refusal);
        }
    }
}
