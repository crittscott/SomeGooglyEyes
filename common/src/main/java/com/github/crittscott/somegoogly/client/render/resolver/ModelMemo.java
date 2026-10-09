package com.github.crittscott.somegoogly.client.render.resolver;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Memoizes a value per (model instance, attach token), misses included. An entry stays valid for as long
 * as its model lives, so a datapack reload never clears anything here; {@link #clear()} drops every entry
 * when models are replaced. Keys are weak, but an entry whose value refers back to its own model (Citadel
 * and LLibrary boxes) is reclaimed only by {@link #clear()}.
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

    // Weak keys reclaim an entry once its model dies, unless the cached value reaches back to the model:
    // vanilla ModelParts and GeckoLib GeoBones cannot, but Citadel's AdvancedModelBox and LLibrary's
    // AdvancedModelRenderer hold a field pointing at their model, which pins the key.
    private final Map<K, Map<String, V>> byModel = new WeakHashMap<>();

    /** Drop every entry. Called when the models being keyed on are replaced wholesale. */
    public void clear() {
        byModel.clear();
    }

    /**
     * The value for {@code token} in {@code model}, resolving and caching it on first ask. A {@code null}
     * model or token resolves to {@code null} without consulting {@code resolver}.
     *
     * <p>Pass the resolver as an object rather than a bound method reference: a capturing lambda would
     * allocate on every call, and this is on the per-frame render path.
     */
    @Nullable
    public V get(@Nullable K model, @Nullable String token, Resolver<K, V> resolver) {
        if (model == null || token == null) {
            return null;
        }
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
