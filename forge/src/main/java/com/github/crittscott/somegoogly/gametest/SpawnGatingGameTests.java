package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGoogly;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * The at-spawn eye roll is probabilistic, but its endpoints are deterministic: {@code globalPercent = 100}
 * always grants eyes to an eligible mob, {@code 0} never does. Driven by forcing {@link ServerConfig#GLOBAL_PERCENT}
 * (restored afterwards) and reading the persisted flag the join handler writes. The decision fires
 * synchronously on entity join, so the flag is set by the time {@code spawn} returns.
 */
@GameTestHolder(SomeGoogly.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpawnGatingGameTests {

    private static final String TEMPLATE = "empty";

    private SpawnGatingGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
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

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void spawnAlwaysAssignsAVariantRoll(GameTestHelper helper) {
        // Independent of the has-eyes roll, a variant roll in [0,1) is always stored so a later application
        // uses this mob's own arrangement.
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        float roll = EyeState.getVariantRoll(cow);
        helper.assertTrue(roll >= 0.0F && roll < 1.0F, "variant roll should be in [0, 1), got " + roll);
        helper.succeed();
    }
}
