package com.github.crittscott.somegoogly.eye.state;

import net.minecraft.world.entity.LivingEntity;

/**
 * {@link EyeHolder} backed by a living entity's {@link EyeState} (the only concrete holder for now).
 * Server-side: every mutation here writes NBT and re-syncs to tracking clients via {@code EyeState}.
 */
public final class EntityEyeHolder implements EyeHolder {

    private final LivingEntity entity;

    public EntityEyeHolder(LivingEntity entity) {
        this.entity = entity;
    }

    public LivingEntity entity() {
        return entity;
    }

    @Override
    public boolean hasEyes() {
        return EyeState.hasEyes(entity);
    }

    @Override
    public void setHasEyes(boolean hasEyes) {
        EyeState.setHasEyes(entity, hasEyes);
    }

    @Override
    public AppearanceOverride getEyeProperties() {
        return EyeState.readProperties(entity);
    }

    @Override
    public void setEyeProperties(AppearanceOverride properties) {
        EyeState.setProperties(entity, properties);
    }
}
