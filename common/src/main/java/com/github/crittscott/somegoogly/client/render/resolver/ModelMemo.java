package com.github.crittscott.somegoogly.client.render.resolver;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Memoizes a value per (model instance, attach token), misses included. An entry stays valid for as long
 * as its model lives, so a datapack reload never clears anything here; {@link #clear()} drops every entry
 * when models are replaced, which is also the only way an entry goes. Models are matched by identity: the
 * cached values are parts of that very instance, and GeckoLib's baked models are records that compare equal
 * to any model of the same structure, including the one a resource reload replaces.
 *
 * <p>Client main thread only.
 *
 * @param <K> the model type keyed on ({@code EntityModel}, or GeckoLib's {@code BakedGeoModel})
 * @param <V> the resolved value ({@link Attachment}, or a GeckoLib bone)
 */
public final class ModelMemo<K, V> {

    /** Computes the value for a (model, token) pair. Called only on a miss. */
    @FunctionalInterface
    public interface Resolver<K, V> {
        /** @return the resolved value, or {@code null} when the token names nothing in the model. */
        @Nullable
        V resolve(K model, String token);
    }

    private final Map<K, Map<String, V>> byModel = new IdentityHashMap<>();

    /** Drop every entry. Called when the models being keyed on are replaced wholesale. */
    public void clear() {
        byModel.clear();
    }

    /**
     * The value for {@code token} in {@code model}, resolving and caching it on first ask.
     *
     * <p>Pass the resolver as an object rather than a bound method reference: a capturing lambda would
     * allocate on every call, and this is on the per-frame render path.
     */
    @Nullable
    public V get(K model, String token, Resolver<K, V> resolver) {
        Map<String, V> tokens = byModel.computeIfAbsent(model, key -> new HashMap<>());
        // A miss walks the whole part tree, so it is cached as null and containsKey decides.
        V cached = tokens.get(token);
        if (cached != null || tokens.containsKey(token)) {
            return cached;
        }
        V resolved = resolver.resolve(model, token);
        tokens.put(token, resolved);
        return resolved;
    }
}
