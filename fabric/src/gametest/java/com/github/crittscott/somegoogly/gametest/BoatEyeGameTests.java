package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Loader entry points for boat state, application, and ambient animation checks. */
public final class BoatEyeGameTests implements FabricGameTest {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(template = TEMPLATE)
    public static void boatsBeginEyelessAndSelectGeometry(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsBeginEyelessAndSelectGeometry(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void boatsCrossOnlyWithConfiguredPartners(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsCrossOnlyWithConfiguredPartners(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void boatsEvictIdleSimulations(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsEvictIdleSimulations(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void boatsFollowGravityAndOwnerMotion(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsFollowGravityAndOwnerMotion(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void boatsHarvestOneEye(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsHarvestOneEye(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE)
    public static void boatsHaveIndependentAmbientAndPhysics(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsHaveIndependentAmbientAndPhysics(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void boatsHonorCreativeConsumption(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsHonorCreativeConsumption(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE)
    public static void boatsHonorInteractionGates(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsHonorInteractionGates(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE)
    public static void boatsReceiveAppearanceAndRefuseRestyling(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsReceiveAppearanceAndRefuseRestyling(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE)
    public static void boatsRefuseSpectatorMutation(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsRefuseSpectatorMutation(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void boatsReplaceAndClearSimulations(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsReplaceAndClearSimulations(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void boatsRequireMatchingEnabledAttachments(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsRequireMatchingEnabledAttachments(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void boatsSaveAndSynchronizeSnapshots(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsSaveAndSynchronizeSnapshots(helper);
    }
}
