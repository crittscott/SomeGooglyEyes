package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;

/**
 * The at-spawn eye roll is probabilistic, but its endpoints are deterministic: {@code globalPercent = 100}
 * always grants eyes to an eligible mob, {@code 0} never does. Driven by forcing {@link ServerConfig#GLOBAL_PERCENT}
 * (restored afterwards) and reading the persisted flag the join handler writes. The decision fires
 * synchronously on entity join, so the flag is set by the time {@code spawn} returns.
 */
public final class SpawnGatingGameTestsLogic {

    private SpawnGatingGameTestsLogic() {
    }

    /**
     * {@code globalPercent = 100} always grants eyes and {@code 0} never does. In game: with no
     * {@code entityOverrides}, set {@code globalPercent = 100} and every spawned cow has eyes; set it to
     * {@code 0} and none of the new ones do.
     */
    public static void fullPercentGrantsEyesAndZeroDeniesThem(GameTestHelper helper) {
        int original = ServerConfig.GLOBAL_PERCENT.get();
        try {
            ServerConfig.GLOBAL_PERCENT.set(100);
            Cow eyed = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
            helper.assertTrue(EyeState.hasEyes(eyed), "an eligible cow at globalPercent=100 should spawn with eyes");

            ServerConfig.GLOBAL_PERCENT.set(0);
            Cow bare = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(4, 2, 2));
            helper.assertTrue(!EyeState.hasEyes(bare), "an eligible cow at globalPercent=0 should spawn without eyes");
        } finally {
            ServerConfig.GLOBAL_PERCENT.set(original);
        }
        helper.succeed();
    }

    /**
     * Every spawned mob stores a placement roll, with or without eyes. In game: set {@code globalPercent = 0},
     * spawn a pig, and on NeoForge or Forge {@code /data get entity <pig>} shows a
     * {@code somegoogly:eyeVariantRoll} key in {@code NeoForgeData} or {@code ForgeData}. Aiming at it and running
     * {@code /sg admin eyes true} gives it the placement that stored roll selects.
     */
    public static void spawnAlwaysAssignsAVariantRoll(GameTestHelper helper) {
        // Independent of the has-eyes roll, a variant roll in [0,1) is always stored so a later
        // /sg admin eyes true uses this mob's own arrangement.
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        float roll = EyeState.getVariantRoll(cow);
        helper.assertTrue(roll >= 0.0F && roll < 1.0F, "variant roll should be in [0, 1), got " + roll);
        helper.succeed();
    }
}
