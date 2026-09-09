package com.github.crittscott.somegoogly.picker;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/** Shared regression checks for the picker command-spawn lifecycle. */
public final class PickerSpawnServiceGameTestsLogic {

    private PickerSpawnServiceGameTestsLogic() {
    }

    public static void commandSpawnFinalizesBeforeApplyingPickerState(GameTestHelper helper) {
        TrackingCow cow = new TrackingCow(helper.getLevel());
        BlockPos destination = helper.absolutePos(new BlockPos(2, 2, 2));
        float yaw = 37.0F;

        PickerSpawnService.prepareForCommandSpawn(
                helper.getLevel(), cow,
                destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5,
                yaw);

        helper.assertTrue(cow.finalizeCalls == 1, "Command-spawn preparation must finalize a mob exactly once");
        helper.assertTrue(cow.spawnType == MobSpawnType.COMMAND,
                "Command-spawn preparation must use the command spawn reason");
        helper.assertTrue(destination.equals(cow.positionAtFinalize),
                "Mob finalization must see the entity's destination");
        helper.assertTrue(!cow.noAiAtFinalize,
                "Picker NoAi must be applied after normal spawn finalization");
        helper.assertTrue(cow.isNoAi(), "A picker-spawned mob must have NoAi");
        helper.assertTrue(cow.isPersistenceRequired(), "A picker-spawned mob must be persistent");
        helper.assertTrue(cow.getYRot() == yaw && cow.getYHeadRot() == yaw && cow.yBodyRot == yaw,
                "Picker facing must override rotations assigned during spawn finalization");

        CompoundTag saved = cow.saveWithoutId(new CompoundTag());
        helper.assertTrue(!saved.isEmpty(), "A prepared picker mob must serialize successfully");
        helper.succeed();
    }

    /** Records the lifecycle state visible to a mob's spawn finalizer. */
    private static final class TrackingCow extends Cow {

        private int finalizeCalls;
        private boolean noAiAtFinalize;
        private BlockPos positionAtFinalize;
        private MobSpawnType spawnType;

        private TrackingCow(Level level) {
            super(EntityType.COW, level);
        }

        @Override
        public SpawnGroupData finalizeSpawn(
                ServerLevelAccessor level,
                DifficultyInstance difficulty,
                MobSpawnType spawnType,
                @Nullable SpawnGroupData spawnGroupData) {
            finalizeCalls++;
            noAiAtFinalize = isNoAi();
            positionAtFinalize = blockPosition();
            this.spawnType = spawnType;
            setYRot(-90.0F);
            setYHeadRot(-90.0F);
            setYBodyRot(-90.0F);
            return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        }
    }
}
