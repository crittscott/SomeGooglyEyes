package com.github.crittscott.somegoogly.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/**
 * Returns the mod-owned persistent compound attached to an entity. NeoForge and Forge expose their
 * patched {@code Entity#getPersistentData()} store; Fabric supplies an equivalent persistent attachment.
 */
public final class EntityPersistentData {

    private EntityPersistentData() {
    }

    /** The entity's live, mutable persistent compound; never null, and written in place. */
    @ExpectPlatform
    public static CompoundTag get(Entity entity) {
        throw new AssertionError();
    }
}
