package com.github.crittscott.somegoogly.picker;

import com.github.crittscott.somegoogly.command.GooglyClientCommands;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Authoring aid behind {@code /sg spawnall}: spawns one of every mob that <i>could</i> wear
 * googly eyes — every registered living entity, whether or not it has an eye config yet — so the
 * picker can be used on any of them without hunting each one down in survival. The single-mob
 * sibling {@code /sg spawn <type>} lands in {@link #spawnOne}. The ender dragon
 * ({@link ServerEyeConfigs#ENDER_DRAGON}) is skipped: it's hard-excluded from eyes, and NoAi
 * doesn't subdue it (its flight/phase logic ignores the flag), so spawning one wrecks the grid.
 *
 * <p>This is the server-side half: the commands themselves are client commands
 * ({@link GooglyClientCommands}) that send picker request packets ({@code PickerSpawnPacket} /
 * {@code PickerSpawnAllPacket}); their creative-gated handlers call
 * {@link #spawn(ServerPlayer, String)} / {@link #spawnOne(ServerPlayer, EntityType)} on the server
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

    private static final ResourceLocation PLAYER = new ResourceLocation("minecraft", "player");

    /** Blocks between the platform floor and the sky-blocking roof above it (clearance for tall mobs). */
    private static final int ROOF_HEIGHT = 8;

    /** Edge of each mob's cell — wide enough to seat a 5x5 platform/basin without overlap. */
    private static final int SPACING = 5;
    /** Reach of the {@code /sg spawn} placement raytrace (matches the admin command's target reach). */
    private static final double SPAWN_REACH = 20.0;
    /** How far in front of the player the first mob in each row stands. */
    private static final int START_OFFSET = 3;

    private PickerSpawnService() {
    }

    /** A living entity built and waiting to be placed, with its registry id for sorting/grouping. */
    private record Candidate(ResourceLocation id, Entity entity) {
    }

    /**
     * Turn the platform tile into a basin for a water mob: raise the 5x5 perimeter into sandstone walls
     * and flood the inner 3x3, both as tall as the mob's water column so it stays submerged and the water
     * cannot spill over the rim. Assumes {@link #buildPlatform} already laid the floor under it.
     */
    private static void buildBasin(ServerLevel level, int cellX, int cellZ, int platformY, Entity entity) {
        int height = waterColumnHeight(entity);
        for (int dy = 0; dy < height; dy++) {
            for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
                for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
                    boolean wall = Math.abs(dx) == PLATFORM_RADIUS || Math.abs(dz) == PLATFORM_RADIUS;
                    level.setBlockAndUpdate(new BlockPos(cellX + dx, platformY + dy, cellZ + dz),
                            wall ? Blocks.SANDSTONE.defaultBlockState() : Blocks.WATER.defaultBlockState());
                }
            }
        }
    }

    /**
     * Lay a 5x5 sandstone tile centered on the cell with its top one block below {@code platformY} (the
     * player's standing level). Existing blocks are overwritten — this is a debug command meant for
     * throwaway test worlds, so it makes no attempt to preserve anything underneath.
     */
    private static void buildPlatform(ServerLevel level, int cellX, int cellZ, int platformY) {
        for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
            for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
                level.setBlockAndUpdate(new BlockPos(cellX + dx, platformY - 1, cellZ + dz),
                        Blocks.SANDSTONE.defaultBlockState());
            }
        }
    }

    /**
     * Cap the platform cell with a second 5x5 layer {@value #ROOF_HEIGHT} blocks above the floor, so
     * sun-sensitive mobs (skeletons, zombies) sit in shade instead of bursting into flames. NoAi does not
     * stop the sun-burn tick and there's no clean per-mob "don't burn" flag, so we block the sky instead.
     * The center block (directly over the mob) is glowstone, lighting the otherwise-dark cell.
     */
    private static void buildRoof(ServerLevel level, int cellX, int cellZ, int platformY) {
        int roofY = platformY - 1 + ROOF_HEIGHT;
        for (int dx = -PLATFORM_RADIUS; dx <= PLATFORM_RADIUS; dx++) {
            for (int dz = -PLATFORM_RADIUS; dz <= PLATFORM_RADIUS; dz++) {
                boolean center = dx == 0 && dz == 0;
                level.setBlockAndUpdate(new BlockPos(cellX + dx, roofY, cellZ + dz),
                        center ? Blocks.GLOWSTONE.defaultBlockState() : Blocks.SANDSTONE.defaultBlockState());
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
     */
    public static void spawn(ServerPlayer player, String modFilter) {
        ServerLevel level = player.serverLevel();

        boolean filtering = modFilter != null && !modFilter.isBlank();
        // When narrowed to one mod, note every living-ish type we couldn't place and why, since the point of
        // filtering is usually to chase down mobs that don't appear. (Plain non-living types are expected
        // drops and stay quiet.)
        List<String> dropped = new ArrayList<>();

        // Build one instance of every living entity type (the eye layer can attach to any of them).
        // We create up front so we can sort, then place; non-living and uncreatable types are dropped.
        List<Candidate> candidates = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id == null || id.equals(PLAYER)) {
                continue;
            }
            if (filtering && !id.getNamespace().equals(modFilter)) {
                continue;
            }
            if (id.equals(ServerEyeConfigs.ENDER_DRAGON)) {
                if (filtering) {
                    dropped.add(id + " — hard-excluded from googly eyes");
                }
                continue;
            }
            Entity entity;
            try {
                entity = type.create(level);
            } catch (Exception e) {
                if (filtering) {
                    dropped.add(id + " — create() threw " + e.getClass().getSimpleName());
                }
                continue; // a modded type that won't build with the plain factory — skip it
            }
            if (entity instanceof LivingEntity) {
                candidates.add(new Candidate(id, entity));
            } else if (filtering && entity == null) {
                dropped.add(id + " — create() returned null");
            }
        }
        if (filtering && candidates.isEmpty() && dropped.isEmpty()) {
            player.sendSystemMessage(Component.translatable("somegoogly.command.spawnall.no_types", modFilter));
            return;
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

            double y;
            if (buildPlatforms) {
                // Flat floor at the player's level; water mobs get a walled basin instead of an open tile.
                // A sandstone roof shades the cell so sun-sensitive mobs don't burn.
                buildPlatform(level, cellX, cellZ, platformY);
                if (water) {
                    buildBasin(level, cellX, cellZ, platformY, entity);
                }
                buildRoof(level, cellX, cellZ, platformY);
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
                        level.setBlockAndUpdate(feet.above(dy), Blocks.WATER.defaultBlockState());
                    }
                }
            }

            double x = cellX + 0.5;
            double z = cellZ + 0.5;
            float yaw = yawToward(x, z, player);

            entity.moveTo(x, y, z, yaw, 0.0F);
            if (entity instanceof Mob mob) {
                mob.setNoAi(true);
                // NoAi mobs still run checkDespawn(); without this, any mob spawned past its category's
                // despawn distance (64 for ambient fish, 128 for most others) is discarded a few ticks
                // later, silently emptying the far cells regardless of chunk loading.
                mob.setPersistenceRequired();
                mob.setYHeadRot(yaw);
                mob.setYBodyRot(yaw);
            }

            if (level.addFreshEntity(entity)) {
                spawned++;
                columnIndex++;
            } else {
                skipped++;
                if (filtering) {
                    dropped.add(candidate.id + " — addFreshEntity() refused");
                }
            }
        }

        int finalSkipped = skipped;
        int finalRows = rowIndex + 1;
        int finalSpawned = spawned;
        Component platformNote = buildPlatforms
                ? Component.translatable("somegoogly.command.spawnall.on_platforms") : Component.empty();
        Component scope = filtering
                ? Component.translatable("somegoogly.command.spawnall.scope", modFilter) : Component.empty();
        Component skippedNote = finalSkipped > 0
                ? Component.translatable("somegoogly.command.spawnall.skipped", finalSkipped) : Component.empty();
        player.sendSystemMessage(Component.translatable("somegoogly.command.spawnall.result",
                finalSpawned, scope, platformNote, finalRows, skippedNote));
        if (filtering && !dropped.isEmpty()) {
            player.sendSystemMessage(Component.translatable("somegoogly.command.spawnall.dropped_header", dropped.size()));
            for (String entry : dropped) {
                player.sendSystemMessage(Component.translatable("somegoogly.command.spawnall.dropped_entry", entry));
            }
        }
    }

    /**
     * Spawn a single {@code type} at the block the player is targeting (within {@value #SPAWN_REACH}
     * blocks) — the {@code /sg spawn <type>} server-thread half. Shares spawnall's conventions (NoAi +
     * persistent, facing the player, ender dragon excluded) but never terraforms: water mobs spawn
     * wherever the player aims, so aim into water to keep one alive. Fails with feedback when no block
     * is targeted or the mob's bounding box doesn't fit at the target.
     */
    public static void spawnOne(ServerPlayer player, EntityType<?> type) {
        ServerLevel level = player.serverLevel();
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id.equals(ServerEyeConfigs.ENDER_DRAGON)) {
            player.sendSystemMessage(Component.translatable("somegoogly.command.spawn.ender_dragon_excluded", id));
            return;
        }

        // Block raytrace along the player's view. Fluids are clipped through (Fluid.NONE), so aiming
        // at open water places the mob on the bed beneath — submerged, which suits a water mob anyway.
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(SPAWN_REACH));
        BlockHitResult hit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.sendSystemMessage(Component.translatable("somegoogly.command.spawn.no_block_targeted", (int) SPAWN_REACH));
            return;
        }

        Entity entity;
        try {
            entity = type.create(level);
        } catch (Exception e) {
            player.sendSystemMessage(Component.translatable(
                    "somegoogly.command.spawn.create_threw", id, e.getClass().getSimpleName()));
            return;
        }
        if (entity == null) {
            player.sendSystemMessage(Component.translatable("somegoogly.command.spawn.create_null", id));
            return;
        }
        if (!(entity instanceof LivingEntity)) {
            player.sendSystemMessage(Component.translatable("somegoogly.command.spawn.not_living", id));
            return;
        }

        // Feet centered in the block adjacent to the hit face (on top, for an upward face). NoAi mobs
        // don't fall, so a side-face placement simply floats there — acceptable for an authoring aid.
        BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        float yaw = yawToward(x, z, player);
        entity.moveTo(x, pos.getY(), z, yaw, 0.0F);

        if (!level.noCollision(entity)) {
            player.sendSystemMessage(Component.translatable("somegoogly.command.spawn.doesnt_fit", id));
            return;
        }

        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
            // NoAi mobs still run checkDespawn(); without this a far-spawned mob silently despawns.
            mob.setPersistenceRequired();
            mob.setYHeadRot(yaw);
            mob.setYBodyRot(yaw);
        }
        if (level.addFreshEntity(entity)) {
            player.sendSystemMessage(Component.translatable("somegoogly.command.spawn.spawned", id));
        } else {
            player.sendSystemMessage(Component.translatable("somegoogly.command.spawn.refused", id));
        }
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
