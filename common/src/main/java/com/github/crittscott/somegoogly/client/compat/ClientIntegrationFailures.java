package com.github.crittscott.somegoogly.client.compat;

import com.github.crittscott.somegoogly.SomeGooglyCommon;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Logs recoverable client integration failures once while allowing affected eyes to be skipped. */
public final class ClientIntegrationFailures {

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private ClientIntegrationFailures() {
    }

    public static void warnOnce(String integration, String operation, String subject, Throwable failure) {
        String key = integration + '\0' + operation + '\0' + subject;
        if (!WARNED.add(key)) {
            return;
        }
        SomeGooglyCommon.LOGGER.warn(
                "Googly-eye {} integration failed during {} for {}; affected eyes will be skipped",
                integration, operation, subject, failure);
    }
}
