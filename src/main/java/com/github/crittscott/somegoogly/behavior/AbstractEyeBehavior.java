package com.github.crittscott.somegoogly.behavior;

import net.minecraft.resources.ResourceLocation;

/** Common id/duration plumbing for the concrete behaviours. */
abstract class AbstractEyeBehavior implements EyeBehavior {

    private final ResourceLocation id;
    private final int defaultDuration;

    AbstractEyeBehavior(String name, int defaultDuration) {
        this.id = new ResourceLocation("somegoogly", name);
        this.defaultDuration = defaultDuration;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public int defaultDuration() {
        return defaultDuration;
    }
}
