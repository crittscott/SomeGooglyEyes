package com.github.crittscott.somegoogly;

import com.github.crittscott.somegoogly.item.ModCreativeTabs;
import com.github.crittscott.somegoogly.item.ModDataComponents;
import com.github.crittscott.somegoogly.item.ModItems;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.recipe.ModRecipes;
import com.github.crittscott.somegoogly.registry.ContentRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod identity shared by every platform. Kept separate from the platform-specific mod entry point
 * class so common code never needs to depend on it.
 */
public final class SomeGooglyCommon {
    public static final String MOD_ID = "somegoogly";
    public static final String MOD_NAME = "Some Googly Eyes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private SomeGooglyCommon() {
    }

    /** Register content shared by every loader. Called once from each loader's entry point. */
    public static void init(ContentRegistrar registrar) {
        ModDataComponents.register(registrar);
        NetworkHandler.registerCommon();
        ModItems.register(registrar);
        ModCreativeTabs.register(registrar);
        ModRecipes.register(registrar);
    }
}
