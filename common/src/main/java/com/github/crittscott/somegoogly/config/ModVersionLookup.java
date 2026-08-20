package com.github.crittscott.somegoogly.config;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.util.Optional;

/** Resolves the loaded version string for a config namespace. */
public final class ModVersionLookup {

    private ModVersionLookup() {
    }

    @ExpectPlatform
    public static Optional<String> versionForNamespace(String namespace) {
        throw new AssertionError();
    }
}
