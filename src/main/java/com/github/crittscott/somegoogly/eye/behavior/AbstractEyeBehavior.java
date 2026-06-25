package com.github.crittscott.somegoogly.eye.behavior;

import net.minecraft.resources.ResourceLocation;

/** Common id/duration plumbing for the concrete behaviors. */
abstract class AbstractEyeBehavior implements EyeBehavior {

    private final int defaultDuration;
    private final ResourceLocation id;

    AbstractEyeBehavior(String name, int defaultDuration) {
        this.id = new ResourceLocation("somegoogly", name);
        this.defaultDuration = defaultDuration;
    }

    @Override
    public int defaultDuration() {
        return defaultDuration;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }
}
