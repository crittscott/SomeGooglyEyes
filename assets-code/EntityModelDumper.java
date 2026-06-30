package com.github.crittscott.assets;

import net.minecraft.client.model.EntityModel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.nio.file.Path;

/** Iterates entity types and writes one file per living entity in the chosen {@link Mode}. */
public class EntityModelDumper {

    public static void dump(CommandSourceStack source, Mode mode) {
        try {
            FileUtils.createDirectories(FileUtils.getOutputDirectory());
        } catch (Exception e) {
            throw new RuntimeException("Could not create output directory", e);
        }

        int written = 0;
        int skipped = 0;
        int failed = 0;

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            try {
                EntityModel<?> model = EntityModelExtractor.resolveModel(type);
                if (model == null) {
                    skipped++; // not a living entity / no model
                    continue;
                }
                Object data = switch (mode) {
                    case SKELETON -> EntityModelExtractor.extractSkeleton(id, model);
                    case GEOMETRY -> EntityModelExtractor.extractGeometry(id, model);
                    case HEAD -> EntityModelExtractor.extractHead(id, model);
                    case BLOCKBENCH -> {
                        ResourceLocation texLoc = EntityModelExtractor.resolveTexture(type);
                        TextureUtils.LoadedTexture texture = texLoc != null ? TextureUtils.load(texLoc) : null;
                        int[] texSize = texture != null ? new int[]{texture.width(), texture.height()} : null;
                        yield BlockbenchExporter.export(
                                EntityModelExtractor.extractGeometry(id, model, texSize), texture);
                    }
                };
                Path output = FileUtils.getEntityOutputPath(id, mode);
                FileUtils.writeJson(data, output);
                written++;
            } catch (Exception e) {
                failed++;
                AssetsMod.getLogger().warn("Failed to extract {} for {}: {}", mode.folder, id, e.toString());
            }
        }

        send(source, String.format("%s: %d written, %d non-living skipped, %d failed",
                mode.folder, written, skipped, failed));
    }

    private static void send(CommandSourceStack source, String message) {
        Component component = Component.literal(message);
        source.sendSuccess(() -> component, false);
    }
}
