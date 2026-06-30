package com.github.crittscott.assets;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.nio.file.Path;
import java.util.Map;

/** Dumps the static resource-pack and data-pack assets (bare {@code /assets}). */
public class AssetDumper {

    public static void dumpAll(CommandSourceStack source) {
        try {
            // Create the output directory
            FileUtils.createDirectories(FileUtils.getOutputDirectory());

            // Get the client resource manager (assets/ - models, textures, sounds, lang)
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

            // Dump client-side (assets/) resources
            dumpModels(source, resourceManager);
            dumpTextures(source, resourceManager);
            dumpSounds(source, resourceManager);
            dumpLanguageFiles(source, resourceManager);

            // Data-pack (data/) resources live on the integrated server's resource
            // manager, not the client's. Only available in singleplayer/LAN.
            MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
            if (server != null) {
                ResourceManager serverResources = server.getResourceManager();
                dumpRecipes(source, serverResources);
                dumpLootTables(source, serverResources);
                dumpAdvancements(source, serverResources);
                dumpTags(source, serverResources);
            } else {
                sendMessage(source, "Skipping recipes/loot/advancements/tags - not in singleplayer");
            }

            sendMessage(source, "Done");

        } catch (Exception e) {
            AssetsMod.getLogger().error("Error during asset dumping", e);
            throw new RuntimeException("Asset dumping failed", e);
        }
    }

    private static void dumpModels(CommandSourceStack source, ResourceManager resourceManager) {
        sendMessage(source, "Starting models");

        Map<ResourceLocation, Resource> models = resourceManager.listResources("models",
                location -> location.getPath().endsWith(".json"));

        dumpResources(models);
    }

    private static void dumpTextures(CommandSourceStack source, ResourceManager resourceManager) {
        sendMessage(source, "Starting textures");

        Map<ResourceLocation, Resource> textures = resourceManager.listResources("textures",
                location -> location.getPath().endsWith(".png") || location.getPath().endsWith(".mcmeta"));

        dumpResources(textures);
    }

    private static void dumpSounds(CommandSourceStack source, ResourceManager resourceManager) {
        sendMessage(source, "Starting sounds");

        Map<ResourceLocation, Resource> sounds = resourceManager.listResources("sounds",
                location -> location.getPath().endsWith(".ogg"));

        dumpResources(sounds);

        // Also get sounds.json files
        Map<ResourceLocation, Resource> soundConfigs = resourceManager.listResources("",
                location -> location.getPath().equals("sounds.json"));

        dumpResources(soundConfigs);
    }

    private static void dumpLanguageFiles(CommandSourceStack source, ResourceManager resourceManager) {
        sendMessage(source, "Starting language files");

        Map<ResourceLocation, Resource> langFiles = resourceManager.listResources("lang",
                location -> location.getPath().endsWith(".json"));

        dumpResources(langFiles);
    }

    private static void dumpRecipes(CommandSourceStack source, ResourceManager resourceManager) {
        sendMessage(source, "Starting recipes");

        Map<ResourceLocation, Resource> recipes = resourceManager.listResources("recipes",
                location -> location.getPath().endsWith(".json"));

        dumpResources(recipes);
    }

    private static void dumpLootTables(CommandSourceStack source, ResourceManager resourceManager) {
        sendMessage(source, "Starting loot tables");

        Map<ResourceLocation, Resource> lootTables = resourceManager.listResources("loot_tables",
                location -> location.getPath().endsWith(".json"));

        dumpResources(lootTables);
    }

    private static void dumpAdvancements(CommandSourceStack source, ResourceManager resourceManager) {
        sendMessage(source, "Starting advancements");

        Map<ResourceLocation, Resource> advancements = resourceManager.listResources("advancements",
                location -> location.getPath().endsWith(".json"));

        dumpResources(advancements);
    }

    private static void dumpTags(CommandSourceStack source, ResourceManager resourceManager) {
        sendMessage(source, "Starting tags");

        Map<ResourceLocation, Resource> tags = resourceManager.listResources("tags",
                location -> location.getPath().endsWith(".json"));

        dumpResources(tags);
    }

    private static void dumpResources(Map<ResourceLocation, Resource> resources) {
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try {
                ResourceLocation location = entry.getKey();
                Resource resource = entry.getValue();
                Path outputPath = FileUtils.getOutputPath(location);

                FileUtils.writeResource(resource, outputPath);
            } catch (Exception e) {
                AssetsMod.getLogger().warn("Failed to dump resource: " + entry.getKey(), e);
                // Continue with other resources
            }
        }
    }

    private static void sendMessage(CommandSourceStack source, String message) {
        Component component = Component.literal(message);
        source.sendSuccess(() -> component, false);
    }
}
