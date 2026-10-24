package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.picker.PickerSpawnService;
import com.github.crittscott.somegoogly.picker.PickerSpawnService.SpawnRefusal;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The picker command-spawn lifecycle: which types the spawn commands accept, and the finalize order. */
public final class PickerSpawnServiceGameTestsLogic {

    private PickerSpawnServiceGameTestsLogic() {
    }

    /**
     * {@code /sg spawn} and {@code /sg spawnall} refuse the ender dragon, players, and config-excluded types,
     * and {@code /sg spawn} suggests only types it accepts. In game: type {@code /sg spawn minecraft:ender}
     * and see no ender dragon offered; add {@code "minecraft:cow"} to {@code spawnExcludedEntities}, then
     * {@code /sg spawn minecraft:cow} reports the cow as excluded and it is no longer suggested.
     */
    public static void spawnCommandsRefuseDragonPlayersAndExcludedTypes(GameTestHelper helper) {
        List<String> original = ServerConfig.SPAWN_EXCLUDED_ENTITIES.get();
        try {
            helper.assertTrue(PickerSpawnService.refusal(EntityType.ENDER_DRAGON) == SpawnRefusal.ENDER_DRAGON,
                    "The ender dragon must be refused");
            helper.assertTrue(PickerSpawnService.refusal(EntityType.PLAYER) == SpawnRefusal.NOT_SUMMONABLE,
                    "Players must be refused as not summonable");
            helper.assertTrue(PickerSpawnService.isSpawnable(EntityType.COW), "A cow must be spawnable by default");

            ServerConfig.SPAWN_EXCLUDED_ENTITIES.set(List.of("minecraft:cow"));
            helper.assertTrue(PickerSpawnService.refusal(EntityType.COW) == SpawnRefusal.EXCLUDED,
                    "A config-excluded type must be refused");
        } finally {
            ServerConfig.SPAWN_EXCLUDED_ENTITIES.set(original);
        }
        helper.succeed();
    }

    /**
     * A command-spawned mob finishes its normal spawn before the picker freezes, persists, and turns it. In
     * game: as a creative operator, {@code /sg spawn minecraft:zombie} at a block; the zombie appears frozen,
     * facing you, with the equipment and variants a {@code /summon} zombie gets, and it is still
     * there after the world is saved and reopened.
     */
    public static void commandSpawnFinalizesBeforeApplyingPickerState(GameTestHelper helper) {
        TrackingCow cow = new TrackingCow(helper.getLevel());
        BlockPos destination = helper.absolutePos(new BlockPos(2, 2, 2));
        float yaw = 37.0F;

        boolean finalized = PickerSpawnService.prepareForCommandSpawn(
                helper.getLevel(), cow,
                destination.getX() + 0.5, destination.getY(), destination.getZ() + 0.5,
                yaw);

        helper.assertTrue(finalized, "An uncancelled command spawn must report successful finalization");
        helper.assertTrue(cow.finalizeCalls == 1, "Command-spawn preparation must finalize a mob exactly once");
        helper.assertTrue(cow.spawnType == EntitySpawnReason.COMMAND,
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
        private EntitySpawnReason spawnType;

        private TrackingCow(Level level) {
            super(EntityType.COW, level);
        }

        @Override
        public SpawnGroupData finalizeSpawn(
                ServerLevelAccessor level,
                DifficultyInstance difficulty,
                EntitySpawnReason spawnType,
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
