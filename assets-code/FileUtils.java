package com.github.crittscott.assets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtils {

    private static Path outputDirectory;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static Path getOutputDirectory() {
        if (outputDirectory == null) {
            outputDirectory = FMLPaths.GAMEDIR.get().resolve("myassets");
        }
        return outputDirectory;
    }

    public static void writeResource(Resource resource, Path outputPath) throws IOException {
        createDirectories(outputPath.getParent());
        try (InputStream in = resource.open();
             OutputStream out = Files.newOutputStream(outputPath)) {
            in.transferTo(out);
        }
    }

    public static void createDirectories(Path path) throws IOException {
        if (path != null && !Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    public static Path getOutputPath(ResourceLocation location) {
        return getOutputDirectory()
                .resolve(location.getNamespace())
                .resolve(sanitizePath(location.getPath()));
    }

    /** {@code myassets/<namespace>/entity/<mode>/<name>.json} */
    public static Path getEntityOutputPath(ResourceLocation id, Mode mode) {
        return getOutputDirectory()
                .resolve(id.getNamespace())
                .resolve("entity")
                .resolve(mode.folder)
                .resolve(sanitizePath(id.getPath()) + "." + mode.extension);
    }

    public static void writeJson(Object data, Path outputPath) throws IOException {
        createDirectories(outputPath.getParent());
        Files.writeString(outputPath, gson.toJson(data));
    }

    public static String sanitizePath(String path) {
        return path.replace(':', '_')
                .replace('<', '_')
                .replace('>', '_')
                .replace('"', '_')
                .replace('|', '_')
                .replace('?', '_')
                .replace('*', '_');
    }
}
