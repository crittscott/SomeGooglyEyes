package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.eye.state.EyeColor;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;

import java.util.Objects;

/**
 * Fabric GameTest entry points for the shared core and picker-spawn assertions. Listed under the
 * {@code somegoogly_gametest} dev-mod's {@code fabric-gametest}
 * entrypoint since Fabric, unlike Forge's {@code @GameTestHolder} scan, requires explicit enumeration.
 */
public final class SomeGooglyGameTests implements FabricGameTest {

    private static final String TEMPLATE = "somegoogly:empty";
    private static final String RELEASED_PERSISTENT_DATA_KEY = "somegoogly:persistentData";

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void configuredCowHasServerGeometry(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.configuredCowHasServerGeometry(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void spawnInitializesEyePersistentData(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.spawnInitializesEyePersistentData(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void commandSpawnFinalizesBeforeApplyingPickerState(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.commandSpawnFinalizesBeforeApplyingPickerState(helper);
    }

    /** Exercises Fabric's persistent-data attachment save/load rather than only the shared in-memory boundary. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void fabricEntityPersistentDataSurvivesSaveLoad(GameTestHelper helper) {
        Cow original = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 2));
        EyeColor iris = new EyeColor(0.2F, 0.4F, 0.6F);
        EyeState.initialize(original, true, 0.375F);
        EyeState.setIrisTint(original, iris);

        CompoundTag saved = original.saveWithoutId(new CompoundTag());
        Cow restored = Objects.requireNonNull(EntityType.COW.create(helper.getLevel(), EntitySpawnReason.LOAD));
        restored.load(saved);

        helper.assertTrue(EyeState.hasEyes(restored), "Fabric should restore the has-eyes flag");
        helper.assertTrue(EyeState.getVariantRoll(restored) == 0.375F,
                "Fabric should restore the placement-variant roll");
        helper.assertTrue(iris.equals(EyeState.readProperties(restored).iris().orElse(null)),
                "Fabric should restore appearance overrides");
        helper.succeed();
    }

    /**
     * Entity eye data saved by the previous released Fabric build loads into the current attachment and is
     * not written back under the released key. In game: open a world saved by the previous release that
     * has an eyed mob; the mob still shows its eyes, and after saving, its entity data (seen with
     * {@code /data get entity}) no longer has a {@code somegoogly:persistentData} entry.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void fabricReleasedEntityDataMigratesOnLoad(GameTestHelper helper) {
        // Never added to the level, so it carries no attachment and the tag holds only the released format.
        Cow source = Objects.requireNonNull(EntityType.COW.create(helper.getLevel(), EntitySpawnReason.LOAD));
        CompoundTag saved = source.saveWithoutId(new CompoundTag());
        CompoundTag released = new CompoundTag();
        released.putBoolean(EyeState.HAS_EYES, true);
        released.putFloat(EyeState.VARIANT_ROLL, 0.625F);
        saved.put(RELEASED_PERSISTENT_DATA_KEY, released);

        Cow restored = Objects.requireNonNull(EntityType.COW.create(helper.getLevel(), EntitySpawnReason.LOAD));
        restored.load(saved);

        helper.assertTrue(EyeState.hasEyes(restored), "Released has-eyes flag should migrate");
        helper.assertTrue(EyeState.getVariantRoll(restored) == 0.625F,
                "Released placement-variant roll should migrate");
        helper.assertTrue(!restored.saveWithoutId(new CompoundTag()).contains(RELEASED_PERSISTENT_DATA_KEY),
                "Migrated data should not be written under the released key");
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
