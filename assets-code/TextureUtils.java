package com.github.crittscott.assets;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.Base64;
import java.util.Optional;

/**
 * Loads an entity texture from the client resource manager and packages it for embedding in a
 * Blockbench {@code .bbmodel} (base64 {@code data:} URL plus real pixel dimensions).
 *
 * <p>Dimensions are read straight from the PNG IHDR header, so no image decode / GL context is
 * needed. Every failure path returns {@code null}; the caller falls back to an untextured export.
 */
public class TextureUtils {

    /** A texture ready to drop into a bbmodel: file name, {@code data:} URL, and real pixel size. */
    public record LoadedTexture(String name, String source, int width, int height) {}

    public static LoadedTexture load(ResourceLocation location) {
        try {
            Optional<Resource> resource =
                    Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isEmpty()) {
                return null;
            }
            byte[] bytes;
            try (InputStream in = resource.get().open()) {
                bytes = in.readAllBytes();
            }
            int[] size = readPngSize(bytes);
            if (size == null) {
                return null;
            }
            String source = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
            return new LoadedTexture(fileName(location.getPath()), source, size[0], size[1]);
        } catch (Exception e) {
            AssetsMod.getLogger().warn("Failed to load texture {}: {}", location, e.toString());
            return null;
        }
    }

    /** Width/height from the PNG IHDR chunk (big-endian ints at byte offsets 16..23). */
    private static int[] readPngSize(byte[] b) {
        if (b.length < 24) {
            return null;
        }
        int width = ((b[16] & 0xFF) << 24) | ((b[17] & 0xFF) << 16) | ((b[18] & 0xFF) << 8) | (b[19] & 0xFF);
        int height = ((b[20] & 0xFF) << 24) | ((b[21] & 0xFF) << 16) | ((b[22] & 0xFF) << 8) | (b[23] & 0xFF);
        if (width <= 0 || height <= 0) {
            return null;
        }
        return new int[]{width, height};
    }

    private static String fileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }
}
