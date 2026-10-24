package com.github.crittscott.somegoogly.migration.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.platform.fabric.EntityPersistentDataImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Converts entity data saved by the previous released Fabric build (Minecraft 1.21.1) as the entity loads.
 * That build stored the mod's persistent compound under the {@value #RELEASED_KEY} entity-tag key; this
 * moves it into the persistent-data attachment, after which the key is never written again. A value under
 * that key that is not a compound is kept in the attachment under {@value #SET_ASIDE_KEY} and logged.
 */
public final class FabricEntityDataMigration {

    public static final String RELEASED_KEY = "somegoogly:persistentData";
    public static final String SET_ASIDE_KEY = "somegoogly:unrecognizedReleasedData";

    private FabricEntityDataMigration() {
    }

    /** Called at the end of {@code Entity.load} with the tag the entity was loaded from. */
    public static void migrate(Entity entity, CompoundTag tag) {
        Tag released = tag.get(RELEASED_KEY);
        if (released == null) {
            return;
        }
        if (released instanceof CompoundTag compound) {
            entity.setAttached(EntityPersistentDataImpl.TYPE, compound.copy());
            return;
        }
        EntityPersistentDataImpl.get(entity).put(SET_ASIDE_KEY, released.copy());
        SomeGooglyCommon.LOGGER.warn("Set aside unrecognized released eye data on {} {} under {}: {}",
                EntityType.getKey(entity.getType()), entity.getUUID(), SET_ASIDE_KEY, released);
    }
}
