package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.migration.fabric.FabricEntityDataMigration;
import com.github.crittscott.somegoogly.platform.EntityPersistentData;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;

import java.util.Objects;

/**
 * Fabric GameTest entry points for the shared core and picker-spawn assertions. Listed under the
 * {@code somegoogly_gametest} dev-mod's {@code fabric-gametest}
 * entrypoint for annotation-based discovery.
 */
public final class SomeGooglyGameTests {

    private static final String TEMPLATE = "somegoogly:empty";
    private static final String RELEASED_PERSISTENT_DATA_KEY = "somegoogly:persistentData";

    @GameTest(structure = TEMPLATE, maxTicks = 20)
    public void commandSpawnFinalizesBeforeApplyingPickerState(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.commandSpawnFinalizesBeforeApplyingPickerState(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 20)
    public void spawnCommandsRefuseDragonPlayersAndExcludedTypes(GameTestHelper helper) {
        PickerSpawnServiceGameTestsLogic.spawnCommandsRefuseDragonPlayersAndExcludedTypes(helper);
    }

    /**
     * Exercises Fabric's persistent-data attachment save/load rather than only the shared in-memory boundary.
     * In game: see {@link SomeGooglyGameTestsLogic#entityPersistentDataSurvivesSaveLoad}.
     */
    @GameTest(structure = TEMPLATE, maxTicks = 20)
    public void fabricEntityPersistentDataSurvivesSaveLoad(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.entityPersistentDataSurvivesSaveLoad(helper, "Fabric");
    }

    /**
     * Entity eye data saved by the previous released Fabric build loads into the current attachment and is
     * not written back under the released key. In game: open a world saved by the previous release that
     * has an eyed mob; the mob still shows its eyes, and after saving, its entity data (seen with
     * {@code /data get entity}) no longer has a {@code somegoogly:persistentData} entry.
     */
    @GameTest(structure = TEMPLATE, maxTicks = 20)
    public void fabricReleasedEntityDataMigratesOnLoad(GameTestHelper helper) {
        // Never added to the level, so it carries no attachment and the tag holds only the released format.
        Cow source = Objects.requireNonNull(EntityType.COW.create(helper.getLevel(), EntitySpawnReason.LOAD));
        CompoundTag saved = source.saveWithoutId(new CompoundTag());
        CompoundTag released = new CompoundTag();
        released.putBoolean(EyeState.HAS_EYES, true);
        released.putFloat(EyeState.VARIANT_ROLL, 0.625F);
        saved.put(RELEASED_PERSISTENT_DATA_KEY, released);

        Cow restored = Objects.requireNonNull(EntityType.COW.create(helper.getLevel(), EntitySpawnReason.LOAD));
        restored.load(saved);

        helper.assertTrue(EyeState.hasEyes(restored), Component.literal("Released has-eyes flag should migrate"));
        helper.assertTrue(EyeState.getVariantRoll(restored) == 0.625F,
                Component.literal("Released placement-variant roll should migrate"));
        helper.assertTrue(!restored.saveWithoutId(new CompoundTag()).contains(RELEASED_PERSISTENT_DATA_KEY),
                Component.literal("Migrated data should not be written under the released key"));
        helper.succeed();
    }

    /**
     * Released-key data that is not a compound is kept, not discarded, and the server log names the entity.
     * In game: give a mob a non-compound {@code somegoogly:persistentData} value in a world saved by the
     * previous release and open it; the log warns about setting the data aside, and {@code /data get entity}
     * on the saved mob shows it under {@code somegoogly:unrecognizedReleasedData} in the mod's attachment.
     */
    @GameTest(structure = TEMPLATE, maxTicks = 20)
    public void fabricUnrecognizedReleasedEntityDataIsSetAside(GameTestHelper helper) {
        Cow source = Objects.requireNonNull(EntityType.COW.create(helper.getLevel(), EntitySpawnReason.LOAD));
        CompoundTag saved = source.saveWithoutId(new CompoundTag());
        saved.putString(RELEASED_PERSISTENT_DATA_KEY, "not a compound");

        Cow restored = Objects.requireNonNull(EntityType.COW.create(helper.getLevel(), EntitySpawnReason.LOAD));
        restored.load(saved);

        helper.assertTrue(!EyeState.hasEyes(restored), Component.literal("Unrecognized released data must not grant eyes"));
        helper.assertTrue("not a compound".equals(EntityPersistentData.get(restored)
                        .getStringOr(FabricEntityDataMigration.SET_ASIDE_KEY, "")),
                Component.literal("Unrecognized released data should be set aside in the attachment"));
        helper.assertTrue(!restored.saveWithoutId(new CompoundTag()).contains(RELEASED_PERSISTENT_DATA_KEY),
                Component.literal("Set-aside data should not be written under the released key"));
        helper.succeed();
    }

    @GameTest(structure = TEMPLATE, maxTicks = 20)
    public void conversionKeepsEyeState(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.conversionKeepsEyeState(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 20)
    public void googlyEyeItemStoresAppearanceOverride(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.googlyEyeItemStoresAppearanceOverride(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 20)
    public void optometristAcceptsOnlyShears(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.optometristAcceptsOnlyShears(helper);
    }

    @GameTest(structure = TEMPLATE, maxTicks = 20)
    public void deathHarvestUsesTheSuppliedDropSink(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.deathHarvestUsesTheSuppliedDropSink(
                helper, FakePlayer.get(helper.getLevel()));
    }

    @GameTest(structure = TEMPLATE, maxTicks = 20)
    public void deathHarvestRejectsNonqualifyingKills(GameTestHelper helper) {
        SomeGooglyGameTestsLogic.deathHarvestRejectsNonqualifyingKills(
                helper, FakePlayer.get(helper.getLevel()));
    }
}
