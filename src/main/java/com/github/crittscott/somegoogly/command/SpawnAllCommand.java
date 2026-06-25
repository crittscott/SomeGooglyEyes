package com.github.crittscott.somegoogly.command;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Authoring aid behind {@code /sg spawnall}: spawns one of every mob that <i>could</i> wear
 * googly eyes — every registered living entity, whether or not it has an eye config yet — so the
 * picker can be used on any of them without hunting each one down in survival.
 *
 * <p>This is the server-thread half: the command itself is a client command ({@link GooglyClientCommands})
 * that hops onto the integrated server before calling {@link #spawn(ServerPlayer)}, so it only works in
 * single-player / LAN host — which is all the picker workflow supports anyway.
 *
 * <p>Layout: mobs are grouped by mod, sorted by id within each mod. Each mod gets its own row heading
 * in the cardinal direction nearest the player's facing (mobs spaced one 3x3 cell apart), and each
 * subsequent mod's row is shifted one cell counter-clockwise. Spawned mobs have their AI disabled, sit
 * on the ground, face the player, and water-dwellers get a small water pocket. (Whether they actually
 * show eyes still follows the server spawn-chance config; toggle the picker on to force eyes.)
 */
public final class SpawnAllCommand {

    private static final ResourceLocation PLAYER = new ResourceLocation("minecraft", "player");

    /** Edge of each mob's 3x3 cell — i.e. the spacing both along a row and between rows. */
    private static final int SPACING = 3;
    /** How far in front of the player the first mob in each row stands. */
    private static final int START_OFFSET = 3;

    private SpawnAllCommand() {
    }

    /** A living entity built and waiting to be placed, with its registry id for sorting/grouping. */
    private record Candidate(ResourceLocation id, Entity entity) {
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

    /** Spawn the grid around {@code player} (must run on the server thread). Reports the result. */
    public static void spawn(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        // Build one instance of every living entity type (the eye layer can attach to any of them).
        // We create up front so we can sort, then place; non-living and uncreatable types are dropped.
        List<Candidate> candidates = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id == null || id.equals(PLAYER)) {
                continue;
            }
            Entity entity;
            try {
                entity = type.create(level);
            } catch (Exception e) {
                continue; // a modded type that won't build with the plain factory — skip it
            }
            if (entity instanceof LivingEntity) {
                candidates.add(new Candidate(id, entity));
            }
        }
        // Sort by mod (namespace) first, then by mob id within the mod.
        candidates.sort(Comparator.comparing((Candidate c) -> c.id.getNamespace()).thenComparing(c -> c.id.getPath()));

        // Forward = cardinal nearest the player's facing; rows stack counter-clockwise from there.
        Direction forward = Direction.fromYRot(player.getYRot());
        Direction sideways = forward.getCounterClockWise();
        int baseX = Mth.floor(player.getX());
        int baseZ = Mth.floor(player.getZ());

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
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(cellX, 0, cellZ));

            double x = cellX + 0.5;
            double y = ground.getY();
            double z = cellZ + 0.5;
            float yaw = yawToward(x, z, player);

            Entity entity = candidate.entity;

            // Water-dwellers flop and die on land; give them a small water column to sit in. We don't
            // care about flow — a source block per cell is enough (the solid floor below keeps it from
            // draining). Column height tracks the mob's size so taller mobs stay submerged.
            if (requiresWater(entity)) {
                BlockPos feet = new BlockPos(cellX, ground.getY(), cellZ);
                int columnHeight = Math.max(1, Mth.ceil(entity.getBbHeight()));
                for (int dy = 0; dy < columnHeight; dy++) {
                    level.setBlockAndUpdate(feet.above(dy), Blocks.WATER.defaultBlockState());
                }
            }

            entity.moveTo(x, y, z, yaw, 0.0F);
            if (entity instanceof Mob mob) {
                mob.setNoAi(true);
                mob.setYHeadRot(yaw);
                mob.setYBodyRot(yaw);
            }

            if (level.addFreshEntity(entity)) {
                spawned++;
                columnIndex++;
            } else {
                skipped++;
            }
        }

        int finalSkipped = skipped;
        int finalRows = rowIndex + 1;
        int finalSpawned = spawned;
        player.sendSystemMessage(Component.literal("[Googly] Spawned " + finalSpawned + " mob(s) across "
                + finalRows + " mod row(s)" + (finalSkipped > 0 ? ", skipped " + finalSkipped : "") + "."));
    }

    /** Yaw (degrees) for an entity at (fromX, fromZ) to look at {@code target}. */
    private static float yawToward(double fromX, double fromZ, Entity target) {
        double dx = target.getX() - fromX;
        double dz = target.getZ() - fromZ;
        // Minecraft yaw 0 faces +Z and increases clockwise; this is the look vector inverse.
        return (float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(-dx, dz)));
    }

}
