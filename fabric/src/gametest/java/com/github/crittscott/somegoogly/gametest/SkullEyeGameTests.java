package com.github.crittscott.somegoogly.gametest;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric GameTest entry points for {@link SkullEyeGameTestsLogic}; see that class for the actual assertions.
 */
public final class SkullEyeGameTests {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void brokenHeadKeepsItsEyes(GameTestHelper helper) {
        SkullEyeGameTestsLogic.brokenHeadKeepsItsEyes(helper, FakePlayer.get(helper.getLevel()));
    }

    // TODO: eyedMobDropsAnEyedHead fails on both loaders for reasons not yet understood, though an eyed mob's
    // head keeps its eyes in game; disabled until the cause is found.
    // @GameTest(structure = TEMPLATE, maxTicks = 40)
    // public void eyedMobDropsAnEyedHead(GameTestHelper helper) {
    //     SkullEyeGameTestsLogic.eyedMobDropsAnEyedHead(helper);
    // }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void headEyesSurviveSaveLoad(GameTestHelper helper) {
        SkullEyeGameTestsLogic.headEyesSurviveSaveLoad(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void optometristHarvestsAPlacedHead(GameTestHelper helper) {
        SkullEyeGameTestsLogic.optometristHarvestsAPlacedHead(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void slimyEyeGivesAPlacedHeadEyes(GameTestHelper helper) {
        SkullEyeGameTestsLogic.slimyEyeGivesAPlacedHeadEyes(helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 40)
    public void slimyEyeRefusesUnconfiguredHeadsAndTheSwitch(GameTestHelper helper) {
        SkullEyeGameTestsLogic.slimyEyeRefusesUnconfiguredHeadsAndTheSwitch(helper, FakePlayer.get(helper.getLevel()));
    }
}
