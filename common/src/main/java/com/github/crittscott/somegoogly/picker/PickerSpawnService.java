package com.github.crittscott.somegoogly.picker;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import com.github.crittscott.somegoogly.platform.MobSpawning;
import com.github.crittscott.somegoogly.util.LookTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Authoring aid behind {@code /sg spawnall}: spawns one of every mob that <i>could</i> wear
 * googly eyes — every summonable living entity, whether or not it has an eye config yet — so the
 * picker can be used on any of them without hunting each one down in survival. The single-mob
 * sibling {@code /sg spawn <type>} lands in {@link #spawnOne}. The ender dragon
 * ({@link ServerEyeConfigs#ENDER_DRAGON}) is skipped: it's hard-excluded from eyes, and NoAi
 * doesn't subdue it (its flight/phase logic ignores the flag), so spawning one wrecks the grid.
 * Both verbs also skip any type an admin listed in the server config's {@code spawnExcludedMods} /
 * {@code spawnExcludedEntities} ({@link ServerConfig#isSpawnExcluded}).
 *
 * <p>The server-owned {@code /sg spawn} and {@code /sg spawnall} commands call
 * {@link #spawn(CommandSourceStack, ServerPlayer, String)} /
 * {@link #spawnOne(CommandSourceStack, ServerPlayer, EntityType)} on the server
 * thread, so the verbs work from a remote client as well as in single-player.
 *
 * <p>Layout: mobs are grouped by mod, sorted by id within each mod. Each mod gets its own row heading
 * in the cardinal direction nearest the player's facing (mobs spaced one cell apart), and each
 * subsequent mod's row is shifted one cell counter-clockwise. Spawned mobs have their AI disabled, sit
 * on the ground, face the player, and water-dwellers get a small water pocket. (Whether they actually
 * show eyes still follows the server spawn-chance config; toggle the picker on to force eyes.)
 *
 * <p>If the player is standing over air when the command runs, the per-cell terrain is ignored and the
 * whole grid is laid on a flat sandstone floor at the player's level — a 5x5 tile under each mob, with
 * water mobs getting a walled 3x3 basin instead. This is a throwaway-test-world convenience and freely
 * overwrites whatever blocks it lands on.
 */
public final class PickerSpawnService {

    /** Half-width of the 5x5 sandstone platform/basin built under each mob in platform mode. */
    private static final int PLATFORM_RADIUS = 2;

    /** Blocks between the platform floor and the sky-blocking roof above it (clearance for tall mobs). */
    private static final int ROOF_HEIGHT = 8;

    /** Edge of each mob's cell — wide enough to seat a 5x5 platform/basin without overlap. */
    private static final int SPACING = 5;
    /** Reach of the {@code /sg spawn} placement raytrace. */
    private static final double SPAWN_REACH = LookTarget.DEFAULT_REACH;
    /** How far in front of the player the first mob in each row stands. */
    private static final int START_OFFSET = 3;

    private PickerSpawnService() {
    }

    private enum InsertionFailure {
        MOB_REFUSED("somegoogly.command.spawn.refused", "somegoogly.command.spawnall.dropped_add_refused"),
        PASSENGER_REFUSED("somegoogly.command.spawn.passenger_refused", "somegoogly.command.spawnall.dropped_passenger_refused");

        private final String spawnAllKey;
        private final String spawnKey;

        InsertionFailure(String spawnKey, String spawnAllKey) {
            this.spawnAllKey = spawnAllKey;
            this.spawnKey = spawnKey;
        }
    }

    /**
     * Why {@code /sg spawn} and {@code /sg spawnall} refuse an entity type, in the order {@link #refusal}
     * checks them, with the feedback each command gives. Players are covered by {@link #NOT_SUMMONABLE}.
     */
    public enum SpawnRefusal {
        ENDER_DRAGON("somegoogly.command.spawn.ender_dragon_excluded", "somegoogly.command.spawnall.dropped_ender_dragon"),
        NOT_SUMMONABLE("somegoogly.command.picker.unknown_entity_type", "somegoogly.command.spawnall.dropped_not_summonable"),
        EXCLUDED("somegoogly.command.spawn.excluded", "somegoogly.command.spawnall.dropped_excluded");

        private final String spawnKey;
        private final String spawnAllKey;

        SpawnRefusal(String spawnKey, String spawnAllKey) {
            this.spawnKey = spawnKey;
            this.spawnAllKey = spawnAllKey;
        }
    }

    /** Whether the authoring spawn commands may create {@code type}; the filter for their suggestions too. */
    public static boolean isSpawnable(EntityType<?> type) {
        return refusal(type) == null;
    }

    /** The first reason the authoring spawn commands refuse {@code type}, or {@code null} if they don't. */
    @Nullable
    public static SpawnRefusal refusal(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (ServerEyeConfigs.isExcluded(id)) {
            return SpawnRefusal.ENDER_DRAGON;
        }
        if (!type.canSummon()) {
            return SpawnRefusal.NOT_SUMMONABLE;
        }
        if (ServerConfig.isSpawnExcluded(id)) {
            return SpawnRefusal.EXCLUDED;
        }
        return null;
    }

    /** A terrain edit held until both finalization and entity insertion have accepted the spawn. */
    private record BlockChange(BlockPos pos, BlockState state) {
    }

    /** A living entity built and waiting to be placed, with its registry id for sorting/grouping. */
    private record Candidate(ResourceLocation id, Entity entity) {
    }

    /** Insert each member using its insertion result, rolling back accepted members if another is refused. */
    @Nullable
    private static InsertionFailure addCommandEntity(ServerLevel level, Entity entity) {
        List<Entity> members = entity.getSelfAndPassengers().toList();
        List<Entity> added = new ArrayList<>();
        boolean complete = false;
        try {
            for (Entity member : members) {
                // An accepted distant entity need not be visible to an immediate UUID lookup.
                if (!level.addFreshEntity(member)) {
                    return member == entity ? InsertionFailure.MOB_REFUSED : InsertionFailure.PASSENGER_REFUSED;
                }
                added.add(member);
            }
            complete = true;
            return null;
        } finally {
            if (!complete) {
                for (int index = added.size() - 1; index >= 0; index--) {
                    added.get(index).discard();
                }
            }
        }
    }

    /** Report a world change to the sender, other operators, and the console, per vanilla convention. */
    private static void announce(CommandSourceStack source, String key, Object... args) {
        PickerGate.tellSuccess(source, true, key, args);
    }

    /**
     * Plan a basin for a water mob: raise the 5x5 perimeter into sandstone walls
     * and flood the inner 3x3, both as tall as the mob's water column so it stays submerged and the water
     * cannot spill over the rim. Follows {@link #planPlatform} in the pending edit list.
     */
    private static void planBasin(List<BlockChange> changes, int cellX, int cellZ, int platformY, Entity entity) {
        int height = waterColumnHeight(entity);
        for (int dy = 0; dy < height; dy++) {
            for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
                for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
                    boolean wall = Math.abs(dx) == PLATFORM_RADIUS || Math.abs(dz) == PLATFORM_RADIUS;
                    changes.add(new BlockChange(new BlockPos(cellX + dx, platformY + dy, cellZ + dz),
                            wall ? Blocks.SANDSTONE.defaultBlockState() : Blocks.WATER.defaultBlockState()));
                }
            }
        }
    }

    /**
     * Plan a 5x5 sandstone tile centered on the cell with its top one block below {@code platformY} (the
     * player's standing level). Existing blocks are overwritten — this is a debug command meant for
     * throwaway test worlds, so it makes no attempt to preserve anything underneath.
     */
    private static void planPlatform(List<BlockChange> changes, int cellX, int cellZ, int platformY) {
        for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
            for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
                changes.add(new BlockChange(new BlockPos(cellX + dx, platformY - 1, cellZ + dz),
                        Blocks.SANDSTONE.defaultBlockState()));
            }
        }
    }

    /**
     * Plan a second 5x5 layer capping the platform cell {@value #ROOF_HEIGHT} blocks above the floor, so
     * sun-sensitive mobs (skeletons, zombies) sit in shade instead of bursting into flames. NoAi does not
     * stop the sun-burn tick and there's no clean per-mob "don't burn" flag, so we block the sky instead.
     * The center block (directly over the mob) is glowstone, lighting the otherwise-dark cell.
     */
    private static void planRoof(List<BlockChange> changes, int cellX, int cellZ, int platformY) {
        int roofY = platformY - 1 + ROOF_HEIGHT;
        for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
            for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
                boolean center = dx == 0 && dz == 0;
                changes.add(new BlockChange(new BlockPos(cellX + dx, roofY, cellZ + dz),
                        center ? Blocks.GLOWSTONE.defaultBlockState() : Blocks.SANDSTONE.defaultBlockState()));
            }
        }
    }

    /**
     * Whether a mob needs to be in water to survive (so it should be pocketed in water rather than
     * left to flop on land). Inferred at runtime from the vanilla aquatic bases ({@link WaterAnimal},
     * {@link Guardian}) plus the water spawn categories, which also catches modded aquatic mobs.
     */
    private static boolean requiresWater(Entity entity) {
        if (entity instanceof WaterAnimal || entity instanceof Guardian) {
            return true;
        }
        MobCategory category = entity.getType().getCategory();
        return category == MobCategory.WATER_CREATURE
                || category == MobCategory.WATER_AMBIENT
                || category == MobCategory.UNDERGROUND_WATER_CREATURE;
    }

    /**
     * Spawn the grid around {@code player} (must run on the server thread). Reports the result.
     *
     * @param modFilter if non-null/blank, only spawn mobs whose namespace equals it (e.g. {@code "minecraft"}
     *                  or {@code "exoticbirds"}), and additionally report each type in that namespace that was
     *                  dropped and why — a debugging aid for mods whose mobs don't appear.
     * @return the number of mobs spawned
     */
    public static int spawn(CommandSourceStack source, ServerPlayer player, String modFilter) {
        ServerLevel level = player.level();

        boolean filtering = modFilter != null && !modFilter.isBlank();
        // When narrowed to one mod, note every living-ish type we couldn't place and why, since the point of
        // filtering is usually to chase down mobs that don't appear. (Plain non-living types are expected
        // drops and stay quiet.)
        List<Component> dropped = new ArrayList<>();

        // Build one instance of every summonable living entity type (the eye layer can attach to any of
        // them). We create up front so we can sort, then place; non-summonable, non-living, and
        // uncreatable types are dropped. The shared refusal check matches /sg spawn; its canSummon gate keeps
        // utility entities (seats, holograms, boss parts) that happen to extend LivingEntity out of the grid.
        List<Candidate> candidates = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (filtering && !id.getNamespace().equals(modFilter)) {
                continue;
            }
            SpawnRefusal refusal = refusal(type);
            if (refusal != null) {
                if (filtering) {
                    dropped.add(Component.translatable(refusal.spawnAllKey, id.toString()));
                }
                continue;
            }
            Entity entity;
            try {
                entity = type.create(level, EntitySpawnReason.COMMAND);
            } catch (Exception e) {
                SomeGooglyCommon.LOGGER.warn("Skipping {} in /sg spawnall: entity factory threw", id, e);
                if (filtering) {
                    dropped.add(Component.translatable(
                            "somegoogly.command.spawnall.dropped_create_threw",
                            id.toString(), e.getClass().getSimpleName()));
                }
                continue; // a modded type that won't build with the plain factory — skip it
            }
            if (entity instanceof LivingEntity) {
                candidates.add(new Candidate(id, entity));
            } else if (filtering && entity == null) {
                dropped.add(Component.translatable("somegoogly.command.spawnall.dropped_create_null", id.toString()));
            }
        }
        if (filtering && candidates.isEmpty() && dropped.isEmpty()) {
            PickerGate.tellFailure(source, "somegoogly.command.spawnall.no_types", modFilter);
            return 0;
        }
        // Sort by mod (namespace) first, then by mob id within the mod.
        candidates.sort(Comparator.comparing((Candidate c) -> c.id.getNamespace()).thenComparing(c -> c.id.getPath()));

        // Forward = cardinal nearest the player's facing; rows stack counter-clockwise from there.
        Direction forward = Direction.fromYRot(player.getYRot());
        Direction sideways = forward.getCounterClockWise();
        int baseX = Mth.floor(player.getX());
        int baseZ = Mth.floor(player.getZ());

        // When the player is standing over air (a deliberate "void display" setup), abandon the per-cell
        // heightmap and lay every mob out on a flat sandstone floor pinned to the player's own level, so
        // the diorama is tidy instead of strewn across whatever terrain each column happens to hit.
        boolean buildPlatforms = level.getBlockState(player.blockPosition().below()).isAir();
        int platformY = Mth.floor(player.getY());

        String currentMod = null;
        int rowIndex = -1;
        int columnIndex = 0;
        int spawned = 0;
        int skipped = 0;
        for (Candidate candidate : candidates) {
            if (!candidate.id.getNamespace().equals(currentMod)) {
                currentMod = candidate.id.getNamespace();
                rowIndex++;
                columnIndex = 0;
            }

            int forwardDist = START_OFFSET + SPACING * columnIndex;
            int sidewaysDist = SPACING * rowIndex;
            int cellX = baseX + forward.getStepX() * forwardDist + sideways.getStepX() * sidewaysDist;
            int cellZ = baseZ + forward.getStepZ() * forwardDist + sideways.getStepZ() * sidewaysDist;

            Entity entity = candidate.entity;
            boolean water = requiresWater(entity);
            List<BlockChange> terrain = new ArrayList<>();

            double y;
            if (buildPlatforms) {
                // Flat floor at the player's level; water mobs get a walled basin instead of an open tile.
                // A sandstone roof shades the cell so sun-sensitive mobs don't burn.
                planPlatform(terrain, cellX, cellZ, platformY);
                if (water) {
                    planBasin(terrain, cellX, cellZ, platformY, entity);
                }
                planRoof(terrain, cellX, cellZ, platformY);
                y = platformY;
            } else {
                BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(cellX, 0, cellZ));
                y = ground.getY();
                // Water-dwellers flop and die on land; give them a small water column to sit in. We don't
                // care about flow — a source block per cell is enough (the solid floor below keeps it from
                // draining). Column height tracks the mob's size so taller mobs stay submerged.
                if (water) {
                    BlockPos feet = new BlockPos(cellX, ground.getY(), cellZ);
                    int columnHeight = waterColumnHeight(entity);
                    for (int dy = 0; dy < columnHeight; dy++) {
                        terrain.add(new BlockChange(feet.above(dy), Blocks.WATER.defaultBlockState()));
                    }
                }
            }

            double x = cellX + 0.5;
            double z = cellZ + 0.5;
            float yaw = yawToward(x, z, player);

            boolean finalized;
            try {
                finalized = prepareForCommandSpawn(level, entity, x, y, z, yaw);
            } catch (Exception e) {
                skipped++;
                SomeGooglyCommon.LOGGER.warn(
                        "Skipping {} in /sg spawnall: finalizeSpawn() threw", candidate.id, e);
                if (filtering) {
                    dropped.add(Component.translatable(
                            "somegoogly.command.spawnall.dropped_finalize_threw",
                            candidate.id.toString(), e.getClass().getSimpleName()));
                }
                continue;
            }
            if (!finalized) {
                skipped++;
                if (filtering) {
                    dropped.add(Component.translatable(
                            "somegoogly.command.spawnall.dropped_cancelled", candidate.id.toString()));
                }
                continue;
            }

            InsertionFailure insertionFailure = addCommandEntity(level, entity);
            if (insertionFailure == null) {
                // No terrain is touched until every spawn refusal has been resolved.
                for (BlockChange change : terrain) {
                    level.setBlockAndUpdate(change.pos(), change.state());
                }
                spawned++;
                columnIndex++;
            } else {
                skipped++;
                if (filtering) {
                    dropped.add(Component.translatable(
                            insertionFailure.spawnAllKey, candidate.id.toString()));
                }
            }
        }

        int rows = rowIndex + 1;
        if (filtering) {
            announce(source, "somegoogly.command.spawnall.result_mod", modFilter, spawned, rows);
        } else {
            announce(source, "somegoogly.command.spawnall.result", spawned, rows);
        }
        if (buildPlatforms) {
            announce(source, "somegoogly.command.spawnall.on_platforms");
        }
        if (skipped > 0) {
            announce(source, "somegoogly.command.spawnall.skipped", skipped);
        }
        if (filtering && !dropped.isEmpty()) {
            PickerGate.tellSuccess(source, false, "somegoogly.command.spawnall.dropped_header", dropped.size());
            for (Component entry : dropped) {
                source.sendSuccess(() -> Component.translatable("somegoogly.command.spawnall.dropped_entry", entry), false);
            }
        }
        return spawned;
    }

    /**
     * Spawn a single {@code type} at the block the player is targeting (within {@value #SPAWN_REACH}
     * blocks) — the {@code /sg spawn <type>} server-thread half. Shares spawnall's conventions (NoAi +
     * persistent, facing the player, ender dragon excluded) but never terraforms: water mobs spawn
     * wherever the player aims, so aim into water to keep one alive. Fails with feedback when no block
     * is targeted or the mob's bounding box doesn't fit at the target.
     *
     * @return whether the mob spawned
     */
    public static boolean spawnOne(CommandSourceStack source, ServerPlayer player, EntityType<?> type) {
        ServerLevel level = player.level();
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        SpawnRefusal refusal = refusal(type);
        if (refusal != null) {
            PickerGate.tellFailure(source, refusal.spawnKey, id.toString());
            return false;
        }

        // Block raytrace along the player's view. Fluids are clipped through (Fluid.NONE), so aiming
        // at open water places the mob on the bed beneath — submerged, which suits a water mob anyway.
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(SPAWN_REACH));
        BlockHitResult hit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            PickerGate.tellFailure(source, "somegoogly.command.spawn.no_block_targeted", (int) SPAWN_REACH);
            return false;
        }

        Entity entity;
        try {
            entity = type.create(level, EntitySpawnReason.COMMAND);
        } catch (Exception e) {
            SomeGooglyCommon.LOGGER.warn("/sg spawn {}: entity factory threw", id, e);
            PickerGate.tellFailure(source, "somegoogly.command.spawn.create_threw", id.toString(), e.getClass().getSimpleName());
            return false;
        }
        if (entity == null) {
            PickerGate.tellFailure(source, "somegoogly.command.spawn.create_null", id.toString());
            return false;
        }
        if (!(entity instanceof LivingEntity)) {
            PickerGate.tellFailure(source, "somegoogly.command.spawn.not_living", id.toString());
            return false;
        }

        // Feet centered in the block adjacent to the hit face (on top, for an upward face). NoAi mobs
        // don't fall, so a side-face placement simply floats there — acceptable for an authoring aid.
        BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        float yaw = yawToward(x, z, player);
        boolean finalized;
        try {
            finalized = prepareForCommandSpawn(level, entity, x, pos.getY(), z, yaw);
        } catch (Exception e) {
            SomeGooglyCommon.LOGGER.warn("/sg spawn {}: finalizeSpawn() threw", id, e);
            PickerGate.tellFailure(source, "somegoogly.command.spawn.finalize_threw", id.toString(), e.getClass().getSimpleName());
            return false;
        }
        if (!finalized) {
            PickerGate.tellFailure(source, "somegoogly.command.spawn.cancelled", id.toString());
            return false;
        }

        if (!level.noCollision(entity)) {
            PickerGate.tellFailure(source, "somegoogly.command.spawn.doesnt_fit", id.toString());
            return false;
        }

        InsertionFailure insertionFailure = addCommandEntity(level, entity);
        if (insertionFailure == null) {
            announce(source, "somegoogly.command.spawn.spawned", id.toString());
            return true;
        }
        PickerGate.tellFailure(source, insertionFailure.spawnKey, id.toString());
        return false;
    }

    /**
     * Put an entity at its destination and complete the normal command-spawn lifecycle before applying
     * the picker's frozen display state. Some modded mobs leave required persistent fields unset until
     * {@link Mob#finalizeSpawn}; inserting a factory-created mob without this step can make it impossible
     * to save. Finalization goes through the loader's finalize-spawn event, as {@code /summon} does, so
     * other mods' listeners initialize the mob too. Public for the shared GameTest that checks this ordering,
     * which lives in another module.
     *
     * @return false when a finalize-spawn listener cancelled the spawn; the entity must then not be added
     */
    public static boolean prepareForCommandSpawn(
            ServerLevel level, Entity entity, double x, double y, double z, float yaw) {
        entity.snapTo(x, y, z, yaw, 0.0F);
        if (entity instanceof Mob mob) {
            if (!MobSpawning.finalizeSpawn(mob, level, EntitySpawnReason.COMMAND)) {
                return false;
            }
            // Freeze finalize-spawned riders (spider jockeys) with their mount.
            for (Entity rider : mob.getIndirectPassengers()) {
                if (rider instanceof Mob riderMob) {
                    riderMob.setNoAi(true);
                    riderMob.setPersistenceRequired();
                }
            }
            mob.setNoAi(true);
            // NoAi mobs still run checkDespawn(); persistence keeps distant grid cells populated.
            mob.setPersistenceRequired();
            mob.setYRot(yaw);
            mob.setYHeadRot(yaw);
            mob.setYBodyRot(yaw);
        }
        return true;
    }

    /** Source-block column height that keeps a water mob of this size submerged (at least one block). */
    private static int waterColumnHeight(Entity entity) {
        return Math.max(1, Mth.ceil(entity.getBbHeight()));
    }

    /** Yaw (degrees) for an entity at (fromX, fromZ) to look at {@code target}. */
    private static float yawToward(double fromX, double fromZ, Entity target) {
        double dx = target.getX() - fromX;
        double dz = target.getZ() - fromZ;
        // Minecraft yaw 0 faces +Z and increases clockwise; this is the look vector inverse.
        return (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(-dx, dz)));
    }
}
