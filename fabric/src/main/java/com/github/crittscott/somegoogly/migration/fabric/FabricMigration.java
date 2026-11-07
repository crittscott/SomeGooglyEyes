package com.github.crittscott.somegoogly.migration.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.platform.fabric.EntityPersistentDataImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Conversion of what the released Fabric builds saved.
 *
 * <p>The config files of the 1.20.1 build have their first section header unquoted though it contains spaces. The
 * released text is kept in a sibling {@value #BACKUP_SUFFIX} file, which the later reformatting backup leaves alone.
 *
 * <p>Entities saved by the released builds for Minecraft 1.20.1 and 1.21.1 hold the mod's persistent compound under
 * the {@value #RELEASED_KEY} entity-tag key. As an entity loads this moves it into the persistent-data attachment,
 * after which the key is never written again. A value under that key that is not a compound is kept in the attachment
 * under {@value #SET_ASIDE_KEY} and logged.
 */
public final class FabricMigration {

    public static final String BACKUP_SUFFIX = ".0.8.2.bak";
    public static final String RELEASED_KEY = "somegoogly:persistentData";
    public static final String SET_ASIDE_KEY = "somegoogly:unrecognizedReleasedData";

    private FabricMigration() {
    }

    /** Called at the end of {@code Entity.load} with the tag the entity was loaded from. */
    public static void migrateEntity(Entity entity, CompoundTag tag) {
        Tag released = tag.get(RELEASED_KEY);
        if (released == null) {
            return;
        }
        if (released instanceof CompoundTag compound) {
            entity.setAttached(EntityPersistentDataImpl.TYPE, compound.copy());
            return;
        }
        EntityPersistentDataImpl.get(entity).put(SET_ASIDE_KEY, released.copy());
        SomeGooglyCommon.LOGGER.warn("Set aside unrecognized released eye data on {} {} under {}: {}",
                EntityType.getKey(entity.getType()), entity.getUUID(), SET_ASIDE_KEY, released);
    }

    /** The released first table has one of these two headers, optionally after comments; other TOML is unchanged. */
    public static String quoteReleasedHeader(String source) {
        int start = source.length() - source.stripLeading().length();
        while (start < source.length() && source.charAt(start) == '#') {
            int end = source.indexOf('\n', start);
            if (end < 0) {
                return source;
            }
            start = end + 1;
            start += source.substring(start).length() - source.substring(start).stripLeading().length();
        }
        for (String section : new String[]{"Server Settings", "Client Settings"}) {
            String header = "[" + section + "]";
            if (source.startsWith(header, start)) {
                return source.substring(0, start) + "[\"" + section + "\"]"
                        + source.substring(start + header.length());
            }
        }
        return source;
    }

    /** Back up the released file at {@code path} and replace it with its {@code migrated} text. */
    public static void replaceFile(Path path, String migrated) throws IOException {
        Path backup = path.resolveSibling(path.getFileName() + BACKUP_SUFFIX);
        Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(path, migrated, StandardCharsets.UTF_8);
        SomeGooglyCommon.LOGGER.info("Migrated released config section header in {} (original retained in {})",
                path, backup.getFileName());
    }
}
