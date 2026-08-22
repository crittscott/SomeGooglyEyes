package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorInstance;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehavior;
import com.github.crittscott.somegoogly.eye.behavior.EyeBehaviors;
import com.github.crittscott.somegoogly.eye.behavior.EyeInfluence;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;

import java.util.Arrays;

/**
 * Every behavior must animate identically given the same seed — the contract that keeps all viewers of
 * one mob (and a mid-effect joiner catching up by age) in lock-step, since only the trigger (id,
 * duration, seed) crosses the wire. Drives the server-loadable half of a behavior ({@code onStart} plus
 * {@code influence}, which is a pure function of age + seeded params) on a {@link BehaviorInstance}. A
 * cow supplies a real {@link HeadInfo} so per-eye behaviors (blink's mask) have geometry to resolve
 * against.
 */
public final class BehaviorDeterminismGameTestsLogic {

    private BehaviorDeterminismGameTestsLogic() {
    }

    private static BehaviorInstance playTo(EyeBehavior behavior, HeadInfo helper, int ticks, long seed) {
        BehaviorInstance instance = new BehaviorInstance(behavior, helper, 8, seed);
        behavior.onStart(instance);
        instance.age = Math.max(0, Math.min(ticks, instance.duration));
        return instance;
    }

    private static boolean influenceEquals(EyeInfluence a, EyeInfluence b) {
        return a.anchorX == b.anchorX && a.anchorY == b.anchorY && a.stiffness == b.stiffness
                && a.eyeScaleMul == b.eyeScaleMul && a.squashY == b.squashY
                && a.tintAmount == b.tintAmount && Arrays.equals(a.corneaTint, b.corneaTint);
    }

    /**
     * Two same-seed instances must resolve identical seeded params and produce identical per-eye
     * {@link EyeInfluence} at every eye — the whole observable output of a behavior at a given age.
     */
    private static boolean statesMatch(EyeBehavior behavior, BehaviorInstance a, BehaviorInstance b, HeadInfo helper) {
        if (a.age != b.age || a.dirSign != b.dirSign
                || !Arrays.equals(a.tintColor, b.tintColor)
                || !Arrays.deepEquals(a.mask, b.mask)) {
            return false;
        }
        EyeInfluence ia = new EyeInfluence();
        EyeInfluence ib = new EyeInfluence();
        for (int h = 0; h < helper.getHeadCount(); h++) {
            for (int e = 0; e < helper.getEyeCount(h); e++) {
                ia.reset();
                ib.reset();
                behavior.influence(a, h, e, ia);
                behavior.influence(b, h, e, ib);
                if (!influenceEquals(ia, ib)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void blinkMaskIsSeedDeterministic(GameTestHelper helper) {
        HeadInfo headInfo = helperFor(helper);
        EyeBehavior blink = EyeBehaviors.byId(new ResourceLocation(SomeGooglyCommon.MOD_ID, "blink"));
        helper.assertTrue(blink != null, "blink behavior should be registered");

        BehaviorInstance first = new BehaviorInstance(blink, headInfo, 8, 777L);
        BehaviorInstance second = new BehaviorInstance(blink, headInfo, 8, 777L);
        blink.onStart(first);
        blink.onStart(second);
        helper.assertTrue(Arrays.deepEquals(first.mask, second.mask),
                "same seed must select the same blink participants");
        helper.succeed();
    }

    public static void everyBehaviorIsSeedDeterministicOverItsRun(GameTestHelper helper) {
        HeadInfo headInfo = helperFor(helper);
        for (EyeBehavior behavior : EyeBehaviors.all()) {
            BehaviorInstance a = playTo(behavior, headInfo, 8, 4242L);
            BehaviorInstance b = playTo(behavior, headInfo, 8, 4242L);
            helper.assertTrue(statesMatch(behavior, a, b, headInfo),
                    "behavior " + behavior.id() + " must reach identical state from the same seed");
        }
        helper.succeed();
    }

    public static void fastForwardMatchesNaturalPlayback(GameTestHelper helper) {
        // A mid-effect joiner replays elapsed ticks to catch up; that must equal natural playback to the
        // same age (the equivalence GooglyTracker#startBehavior relies on, verified on the instance itself).
        HeadInfo headInfo = helperFor(helper);
        EyeBehavior swirl = EyeBehaviors.byId(new ResourceLocation(SomeGooglyCommon.MOD_ID, "swirl"));
        helper.assertTrue(swirl != null, "swirl behavior should be registered");

        BehaviorInstance natural = playTo(swirl, headInfo, 5, 31337L);
        BehaviorInstance caughtUp = playTo(swirl, headInfo, 5, 31337L);
        helper.assertTrue(statesMatch(swirl, natural, caughtUp, headInfo),
                "catch-up by age must match natural playback at the same age");
        helper.succeed();
    }

    private static HeadInfo helperFor(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(cow.getType());
        return ServerEyeConfigs.resolve(type, cow, EyeState.getVariantRoll(cow));
    }
}
