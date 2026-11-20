package com.github.crittscott.somegoogly.clientgametest;

import java.util.List;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers the client tests' built-in datapacks, each off until a scenario enables it with
 * {@code /datapack enable}. They supply definitions vanilla has no use for: paired and one-eye boat layouts,
 * alternate cow layouts, a disabled cow definition, and a stick tagged as shears.
 */
public final class TestPacks implements ModInitializer {
    public static final String MOD_ID = "somegoogly_clientgametest";
    public static final String BOAT_ONE_EYE = "boat_one_eye";
    public static final String BOAT_PAIRED = "boat_paired";
    public static final String COW_OFF = "cow_off";
    public static final String LAYOUT_A = "layout_a";
    public static final String LAYOUT_B = "layout_b";
    public static final String SHEARS = "shears";

    private static final List<String> PACKS = List.of(BOAT_ONE_EYE, BOAT_PAIRED, COW_OFF, LAYOUT_A, LAYOUT_B, SHEARS);

    /** The pack id {@code /datapack} uses, quoted because it contains a colon. */
    public static String commandId(String pack) {
        return "\"" + MOD_ID + ":" + pack + "\"";
    }

    @Override
    public void onInitialize() {
        ModContainer container = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
        for (String pack : PACKS) {
            ResourceManagerHelper.registerBuiltinResourcePack(ResourceLocation.fromNamespaceAndPath(MOD_ID, pack),
                    container, Component.literal("Client test pack: " + pack), ResourcePackActivationType.NORMAL);
        }
    }
}
