package com.github.crittscott.somegoogly.client.compat;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

/** Logs recoverable client integration failures once each. */
public final class ClientIntegrationFailures {

    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private ClientIntegrationFailures() {
    }

    /**
     * Log a renderer-integration failure, once per integration, operation, and subject; {@code failure} is
     * {@code null} when no exception lies behind it.
     */
    public static void warnOnce(String integration, String operation, String subject, @Nullable Throwable failure) {
        String key = integration + '\0' + operation + '\0' + subject;
        if (!WARNED.add(key)) {
            return;
        }
        SomeGooglyCommon.LOGGER.warn(
                "Googly-eye {} integration failed during {} for {}", integration, operation, subject, failure);
    }
}
