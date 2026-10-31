package com.github.crittscott.somegoogly.config.fabric;

/** Migration of the immediately preceding Fabric release's unquoted section header containing spaces. */
final class FabricConfigMigration {

    private FabricConfigMigration() {
    }

    /** The released first table has one of these two headers, optionally after comments; other TOML is unchanged. */
    static String quoteReleasedHeader(String source) {
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
}
