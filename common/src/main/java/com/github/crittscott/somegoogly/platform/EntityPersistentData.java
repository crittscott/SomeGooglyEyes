package com.github.crittscott.somegoogly.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/**
 * Returns the mod-owned persistent compound attached to an entity. Forge supplies its patched
 * {@code Entity#getPersistentData()} store; Fabric supplies an equivalent field through a Mixin.
 */
public final class EntityPersistentData {

    private EntityPersistentData() {
    }

    @ExpectPlatform
    public static CompoundTag get(Entity entity) {
        throw new AssertionError();
    }
}
