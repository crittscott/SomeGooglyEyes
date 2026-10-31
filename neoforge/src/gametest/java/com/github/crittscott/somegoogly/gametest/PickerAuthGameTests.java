package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
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

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void entityTargetingStopsAtWalls(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.entityTargetingStopsAtWalls(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
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
    public static void spawnAllHasAServerWideCooldown(GameTestHelper helper) {
        PickerAuthGameTestsLogic.spawnAllHasAServerWideCooldown(helper);
    }
}
