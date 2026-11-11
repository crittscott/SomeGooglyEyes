package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/** Installs a test's eye definitions through the same path a datapack reload uses. */
final class TestEyeConfigs {

    private TestEyeConfigs() {
    }

    static void install(Map<ResourceLocation, RuntimeConfigSet> next) {
        ServerEyeConfigs.replaceIfChanged(ServerEyeConfigs.packetFor(next));
    }
}
