package com.github.crittscott.somegoogly;

import com.github.crittscott.somegoogly.enchant.ModEnchantments;
import com.github.crittscott.somegoogly.item.ModCreativeTabs;
import com.github.crittscott.somegoogly.item.ModItems;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.recipe.ModRecipes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Mod identity shared by every platform. Kept separate from the platform-specific mod entry point
 * class so common code never needs to depend on it.
 */
public final class SomeGooglyCommon {
    public static final String MOD_ID = "somegoogly";
    public static final String MOD_NAME = "Some Googly Eyes";
    public static final Logger LOGGER = LogManager.getLogger();

    private SomeGooglyCommon() {
    }

    /** Register content shared by every loader. Called once from each loader's entry point. */
    public static void init() {
        NetworkHandler.registerCommon();
        ModEnchantments.register();
        ModItems.register();
        ModCreativeTabs.register();
        ModRecipes.register();
    }
}
