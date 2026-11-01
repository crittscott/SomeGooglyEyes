package com.github.crittscott.somegoogly.gametest;

import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** Loader entry points for boat state, application, and ambient animation checks. */
public final class BoatEyeGameTests {


    private BoatEyeGameTests() {
    }

    public static void boatsBeginEyelessAndSelectGeometry(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsBeginEyelessAndSelectGeometry(helper);
    }

    public static void boatsCrossOnlyWithConfiguredPartners(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsCrossOnlyWithConfiguredPartners(helper);
    }

    public static void boatsEvictIdleAndPauseSimulations(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsEvictIdleAndPauseSimulations(helper);
    }

    public static void boatsFollowGravityAndOwnerMotion(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsFollowGravityAndOwnerMotion(helper);
    }

    public static void boatsHarvestOneEye(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsHarvestOneEye(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void boatsHaveIndependentAmbientAndPhysics(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsHaveIndependentAmbientAndPhysics(helper);
    }

    public static void boatsHonorCreativeConsumption(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsHonorCreativeConsumption(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void boatsHonorInteractionGates(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsHonorInteractionGates(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void boatsReceiveAppearanceAndRefuseRestyling(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsReceiveAppearanceAndRefuseRestyling(helper, FakePlayerFactory.getMinecraft(helper.getLevel()));
    }

    public static void boatsRefuseSpectatorMutation(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsRefuseSpectatorMutation(helper);
    }

    public static void boatsReplaceAndClearSimulations(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsReplaceAndClearSimulations(helper);
    }

    public static void boatsRequireMatchingEnabledAttachments(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsRequireMatchingEnabledAttachments(helper);
    }

    public static void boatsSaveAndSynchronizeSnapshots(GameTestHelper helper) {
        BoatEyeGameTestsLogic.boatsSaveAndSynchronizeSnapshots(helper);
    }
}
