package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.function.Consumer;

/**
 * Forge GameTest entry points for {@link PickerAuthGameTestsLogic}; see that class for the actual
 * assertions.
 */
@GameTestHolder(SomeGooglyCommon.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PickerAuthGameTests {

    private static final String TEMPLATE = "empty";

    private PickerAuthGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void entityTargetingStopsAtWalls(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.entityTargetingStopsAtWalls(helper, SomeGooglyGameTests.survivalPlayer(helper));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void pickerRequestsThrottlePerTick(GameTestHelper helper) {
        PickerAuthGameTestsLogic.pickerRequestsThrottlePerTick(
                helper, SomeGooglyGameTests.survivalPlayer(helper));
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
    public static void spawnAllHasAServerWideCooldown(GameTestHelper helper) {
        PickerAuthGameTestsLogic.spawnAllHasAServerWideCooldown(helper);
    }
}
