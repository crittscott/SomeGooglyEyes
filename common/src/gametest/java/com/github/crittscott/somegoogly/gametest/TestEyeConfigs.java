package com.github.crittscott.somegoogly.gametest;

import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfigSet;
import com.github.crittscott.somegoogly.config.ServerEyeConfigs;
import net.minecraft.resources.Identifier;

import java.util.Map;

/** Installs a test's eye definitions through the same path a datapack reload uses. */
final class TestEyeConfigs {

    private TestEyeConfigs() {
    }

    static void install(Map<Identifier, RuntimeConfigSet> next) {
        ServerEyeConfigs.replaceIfChanged(ServerEyeConfigs.packetFor(next));
    }
}
