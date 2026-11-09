package com.github.crittscott.somegoogly.platform.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

/**
 * Fabric implementation of the shared entity-persistence boundary: a persistent data attachment holding
 * the same compound Forge keeps in its entity persistent data.
 */
public final class EntityPersistentDataImpl {

    public static final AttachmentType<CompoundTag> TYPE = AttachmentRegistry.<CompoundTag>builder()
            .persistent(CompoundTag.CODEC)
            .initializer(CompoundTag::new)
            .buildAndRegister(SomeGooglyCommon.id("persistent_data"));

    private EntityPersistentDataImpl() {
    }

    /** The attached compound, if any; reading must not add a persistent attachment. */
    @Nullable
    public static CompoundTag find(Entity entity) {
        return entity.getAttached(TYPE);
    }

    /** Registers {@link #TYPE}; must run during mod initialization, before any entity loads. */
    public static void register() {
    }

    public static CompoundTag get(Entity entity) {
        return entity.getAttachedOrCreate(TYPE);
    }
}
