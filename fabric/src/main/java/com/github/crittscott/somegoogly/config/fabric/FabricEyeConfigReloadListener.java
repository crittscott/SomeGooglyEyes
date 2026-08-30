package com.github.crittscott.somegoogly.config.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.EyeConfigReloadListener;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;

/** Fabric identity bridge for the loader-neutral datapack reload listener. */
public final class FabricEyeConfigReloadListener extends EyeConfigReloadListener
        implements IdentifiableResourceReloadListener {

    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(SomeGooglyCommon.MOD_ID, "eye_configs");

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
}
