package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import com.github.crittscott.somegoogly.eye.state.SkullEyeState;
import com.github.crittscott.somegoogly.picker.PickerSpawnService.SpawnRefusal;
import com.github.crittscott.somegoogly.picker.PickerSpawnService;
import com.github.crittscott.somegoogly.util.LookTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The picker command-spawn lifecycle: which types the spawn commands accept, and the finalize order. */
public final class PickerSpawnServiceGameTestsLogic {

    private PickerSpawnServiceGameTestsLogic() {
    }

    /**
     * Extended entity targeting stops at blocks. In game: stand behind a wall from an eyed cow and aim at
     * it with an eye item or an admin command; neither inspection nor editing selects it through the wall.
     */
    public static void entityTargetingStopsAtWalls(GameTestHelper helper, ServerPlayer player) {
        var originalPosition = player.position();
        float originalYaw = player.getYRot();
        float originalPitch = player.getXRot();
        try {
            BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 0));
            player.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0F, 0F);
            helper.setBlock(new BlockPos(2, 2, 1), Blocks.AIR);
            helper.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
            Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 3));
            helper.assertTrue(LookTarget.livingInCrosshair(player, LookTarget.DEFAULT_REACH) == cow,
                    Component.literal("the unobstructed cow is selected by the extended ray"));
            helper.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
            helper.assertTrue(LookTarget.livingInCrosshair(player, LookTarget.DEFAULT_REACH) == null,
                    Component.literal("an intervening wall prevents selecting the hidden cow"));
        } finally {
            player.snapTo(originalPosition.x, originalPosition.y, originalPosition.z, originalYaw, originalPitch);
        }
        helper.succeed();
    }

    /**
     * A native finalization or insertion refusal leaves planned floors, water basins, and roofs untouched.
     * The loader wrapper installs the refusing listener. In game: refuse command spawns with a listener,
     * stand over air, and run spawnall; the command skips the mobs without replacing terrain or head data.
     */
    public static void refusedBulkSpawnLeavesTerrain(GameTestHelper helper, ServerPlayer player) {
        var originalPosition = player.position();
        float originalYaw = player.getYRot();
        float originalPitch = player.getXRot();
        List<String> excluded = ServerConfig.SPAWN_EXCLUDED_ENTITIES.get();
        List<String> excludedMods = ServerConfig.SPAWN_EXCLUDED_MODS.get();
        try {
            List<String> otherTypes = new ArrayList<>();
            BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
                if (type != EntityType.COW && type != EntityType.SQUID) {
                    otherTypes.add(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
                }
            });
            ServerConfig.SPAWN_EXCLUDED_ENTITIES.set(otherTypes);
            ServerConfig.SPAWN_EXCLUDED_MODS.set(List.of());
            BlockPos origin = helper.absolutePos(new BlockPos(2, 3, 0));
            player.snapTo(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0F, 0F);
            helper.setBlock(new BlockPos(2, 2, 0), Blocks.AIR);
            helper.setBlock(new BlockPos(2, 2, 3), Blocks.SKELETON_SKULL);
            SkullBlockEntity skull = helper.getBlockEntity(new BlockPos(2, 2, 3), SkullBlockEntity.class);
            EyeState.Snapshot eyes = new EyeState.Snapshot(true, 0.5F, AppearanceOverride.EMPTY);
            SkullEyeState.set(skull, eyes);
            Map<BlockPos, BlockState> before = new LinkedHashMap<>();
            for (int x = 0; x <= 4; x++) {
                for (int z = 1; z <= 5; z++) {
                    for (int y = 2; y <= 10; y++) {
                        BlockPos pos = helper.absolutePos(new BlockPos(x, y, z));
                        before.put(pos, helper.getLevel().getBlockState(pos));
                    }
                }
            }
            PickerSpawnService.spawn(player, "minecraft");
            before.forEach((pos, state) -> helper.assertTrue(helper.getLevel().getBlockState(pos).equals(state),
                    Component.literal("refused bulk spawn must leave terrain unchanged at " + pos)));
            helper.assertTrue(helper.getLevel().getBlockEntity(skull.getBlockPos()) == skull
                            && SkullEyeState.snapshot(skull).equals(eyes),
                    Component.literal("refused terrain edits preserve the original block entity and its eye data"));
        } finally {
            ServerConfig.SPAWN_EXCLUDED_ENTITIES.set(excluded);
            ServerConfig.SPAWN_EXCLUDED_MODS.set(excludedMods);
            player.snapTo(originalPosition.x, originalPosition.y, originalPosition.z, originalYaw, originalPitch);
        }
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
                    Component.literal("The ender dragon must be refused"));
            helper.assertTrue(PickerSpawnService.refusal(EntityType.PLAYER) == SpawnRefusal.NOT_SUMMONABLE,
                    Component.literal("Players must be refused as not summonable"));
            helper.assertTrue(PickerSpawnService.isSpawnable(EntityType.COW), Component.literal("A cow must be spawnable by default"));

            ServerConfig.SPAWN_EXCLUDED_ENTITIES.set(List.of("minecraft:cow"));
            helper.assertTrue(PickerSpawnService.refusal(EntityType.COW) == SpawnRefusal.EXCLUDED,
                    Component.literal("A config-excluded type must be refused"));
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

        helper.assertTrue(finalized, Component.literal("An uncancelled command spawn must report successful finalization"));
        helper.assertTrue(cow.finalizeCalls == 1, Component.literal("Command-spawn preparation must finalize a mob exactly once"));
        helper.assertTrue(cow.spawnType == EntitySpawnReason.COMMAND,
                Component.literal("Command-spawn preparation must use the command spawn reason"));
        helper.assertTrue(destination.equals(cow.positionAtFinalize),
                Component.literal("Mob finalization must see the entity's destination"));
        helper.assertTrue(!cow.noAiAtFinalize,
                Component.literal("Picker NoAi must be applied after normal spawn finalization"));
        helper.assertTrue(cow.isNoAi(), Component.literal("A picker-spawned mob must have NoAi"));
        helper.assertTrue(cow.isPersistenceRequired(), Component.literal("A picker-spawned mob must be persistent"));
        helper.assertTrue(cow.getYRot() == yaw && cow.getYHeadRot() == yaw && cow.yBodyRot == yaw,
                Component.literal("Picker facing must override rotations assigned during spawn finalization"));

        CompoundTag saved = cow.saveWithoutId(new CompoundTag());
        helper.assertTrue(!saved.isEmpty(), Component.literal("A prepared picker mob must serialize successfully"));
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
