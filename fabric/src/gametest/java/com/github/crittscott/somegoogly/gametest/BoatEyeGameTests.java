package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Loader entry points for boat state, application, and ambient animation checks. */
public final class BoatEyeGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void boatsBeginEyelessAndSelectGeometry(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsBeginEyelessAndSelectGeometry(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void boatsCrossOnlyWithConfiguredPartners(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsCrossOnlyWithConfiguredPartners(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void boatsEvictIdleAndPauseSimulations(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsEvictIdleAndPauseSimulations(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void boatsFollowGravityAndOwnerMotion(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsFollowGravityAndOwnerMotion(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void boatsHarvestOneEye(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsHarvestOneEye(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void boatsHaveIndependentAmbientAndPhysics(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsHaveIndependentAmbientAndPhysics(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void boatsHonorCreativeConsumption(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsHonorCreativeConsumption(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void boatsHonorInteractionGates(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsHonorInteractionGates(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void boatsReceiveAppearanceAndRefuseRestyling(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsReceiveAppearanceAndRefuseRestyling(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void boatsRefuseSpectatorMutation(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsRefuseSpectatorMutation(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void boatsReplaceAndClearSimulations(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsReplaceAndClearSimulations(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void boatsRequireMatchingEnabledAttachments(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsRequireMatchingEnabledAttachments(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 100)
    public void boatsSaveAndSynchronizeSnapshots(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsSaveAndSynchronizeSnapshots(helper);
    }
}
