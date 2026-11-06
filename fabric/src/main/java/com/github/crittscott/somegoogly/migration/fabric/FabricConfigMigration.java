package com.github.crittscott.somegoogly.migration.fabric;

import com.github.crittscott.somegoogly.SomeGooglyCommon;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Migration of the released 0.8.2 Fabric config files, whose first section header contains spaces unquoted. The
 * released text is kept in a sibling {@value #BACKUP_SUFFIX} file, which the later reformatting backup leaves alone.
 */
public final class FabricConfigMigration {

    public static final String BACKUP_SUFFIX = ".0.8.2.bak";

    private FabricConfigMigration() {
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
