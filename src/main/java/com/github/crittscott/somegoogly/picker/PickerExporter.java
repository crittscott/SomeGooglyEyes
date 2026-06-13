package com.github.crittscott.somegoogly.picker;

import com.github.crittscott.somegoogly.head.HeadInfo.EntityConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes the committed picker config to a datapack in the current single-player world
 * ({@code world/datapacks/somegoogly-picker/data/<ns>/eyes/<entity>.json}) and triggers a
 * {@code /reload} so it persists and re-syncs through the normal path.
 */
public final class PickerExporter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PACK_NAME = "somegoogly-picker";
    private static final String PACK_MCMETA =
            "{\n  \"pack\": {\n    \"pack_format\": 15,\n    \"description\": \"SomeGoogly picker output\"\n  }\n}\n";

    private PickerExporter() {
    }

    public static String export() {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) {
            return "Export needs single-player.";
        }
        ResourceLocation type = PickerState.targetType();
        if (type == null || PickerState.committedCount() == 0) {
            return "Nothing committed to export.";
        }

        EntityConfig config = PickerState.toConfig();
        Path packDir = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK_NAME);
        Path eyesDir = packDir.resolve("data").resolve(type.getNamespace()).resolve("eyes");
        Path file = eyesDir.resolve(type.getPath() + ".json");

        try {
            Files.createDirectories(eyesDir);
            Path meta = packDir.resolve("pack.mcmeta");
            if (!Files.exists(meta)) {
                Files.writeString(meta, PACK_MCMETA);
            }
            Files.writeString(file, GSON.toJson(config));
        } catch (IOException e) {
            return "Export failed: " + e.getMessage();
        }

        // Reload on the server thread so the datapack is re-read and re-synced to the client.
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload"));
        return "Exported " + type + " → " + PACK_NAME + ", reloading.";
    }
}
