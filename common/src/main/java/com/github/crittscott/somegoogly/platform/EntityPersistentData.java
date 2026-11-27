package com.github.crittscott.somegoogly.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

/**
 * Returns the mod-owned persistent compound attached to an entity. Forge exposes its patched {@code
 * Entity#getPersistentData()} store; Fabric supplies an equivalent persistent attachment.
 */
public final class EntityPersistentData {

    private EntityPersistentData() {}

    /**
     * The entity's compound for reading, or {@code null} if it has none. Forge has no read that
     * doesn't create it, so only Fabric returns {@code null}.
     */
    // Architectury replaces this stub body with the loader implementation.
    @SuppressWarnings("DoNotCallSuggester")
    @ExpectPlatform
    @Nullable
    public static CompoundTag find(Entity entity) {
        throw new AssertionError();
    }

    /** The entity's live, mutable persistent compound; never null, and written in place. */
    // Architectury replaces this stub body with the loader implementation.
    @SuppressWarnings("DoNotCallSuggester")
    @ExpectPlatform
    public static CompoundTag get(Entity entity) {
        throw new AssertionError();
    }
}
