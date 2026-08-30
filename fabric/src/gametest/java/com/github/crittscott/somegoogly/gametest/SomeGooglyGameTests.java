package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;

import java.util.Objects;

/**
 * Fabric GameTest entry points for {@link SomeGooglyGameTestsLogic}; see that class for the actual
 * assertions. Listed under the {@code somegoogly_gametest} dev-mod's {@code fabric-gametest}
 * entrypoint since Fabric, unlike Forge's {@code @GameTestHolder} scan, requires explicit enumeration.
 */
public final class SomeGooglyGameTests implements FabricGameTest {

    private static final String TEMPLATE = "somegoogly:empty";

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void configuredCowHasServerGeometry(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.configuredCowHasServerGeometry(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void spawnInitializesEyePersistentData(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.spawnInitializesEyePersistentData(helper);
    }

    /** Exercises Fabric's entity save/load Mixin rather than only the shared in-memory boundary. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void fabricEntityPersistentDataSurvivesSaveLoad(GameTestHelper helper) {
        Cow original = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        EyeColor iris = new EyeColor(0.2F, 0.4F, 0.6F);
        EyeState.initialize(original, true, 0.375F);
        EyeState.setIrisTint(original, iris);

        CompoundTag saved = original.saveWithoutId(new CompoundTag());
        Cow restored = Objects.requireNonNull(EntityType.COW.create(helper.getLevel()));
        restored.load(saved);

        helper.assertTrue(EyeState.hasEyes(restored), "Fabric should restore the has-eyes flag");
        helper.assertTrue(EyeState.getVariantRoll(restored) == 0.375F,
                "Fabric should restore the placement-variant roll");
        helper.assertTrue(iris.equals(EyeState.readProperties(restored).iris().orElse(null)),
                "Fabric should restore appearance overrides");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void eyeStateAppearanceOverridesRoundTrip(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.eyeStateAppearanceOverridesRoundTrip(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void googlyEyeItemStoresAppearanceOverride(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.googlyEyeItemStoresAppearanceOverride(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void optometristAcceptsOnlyShears(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.optometristAcceptsOnlyShears(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void deathHarvestUsesTheSuppliedDropSink(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.deathHarvestUsesTheSuppliedDropSink(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void deathHarvestRejectsNonqualifyingKills(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.deathHarvestRejectsNonqualifyingKills(
                helper, FakePlayer.get(helper.getLevel()));
    }
}
