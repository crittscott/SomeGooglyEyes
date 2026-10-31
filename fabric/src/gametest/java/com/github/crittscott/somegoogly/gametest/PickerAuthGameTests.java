package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link PickerAuthGameTestsLogic}; see that class for the actual
 * assertions.
 */
public final class PickerAuthGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void pickerRequestsThrottlePerTick(GameTestHelper helper) {
        PickerAuthGameTestsLogic.pickerRequestsThrottlePerTick(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void spawnAllHasAServerWideCooldown(GameTestHelper helper) {
        PickerAuthGameTestsLogic.spawnAllHasAServerWideCooldown(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void entityTargetingStopsAtWalls(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.entityTargetingStopsAtWalls(helper, FakePlayer.get(helper.getLevel()));
    }
}
