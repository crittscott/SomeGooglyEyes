package com.github.crittscott.somegoogly.client.render.resolver;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Memoizes a value per (model instance, attach token). Both halves of the key are immutable for as long as
 * the model lives — a model is a singleton held by its renderer, and a token always names the same part
 * within it — so an entry can never go stale. In particular a datapack reload changes <i>which</i> token is
 * asked for, never what a token resolves to, and so must not clear anything here. The one thing that does
 * end an entry's life is the model being replaced, which is what {@link #clear()} is for.
 *
 * <p>Keys are weak, which reclaims an entry once its model dies — <i>provided</i> no cached value can
 * reach that model. A vanilla {@code ModelPart} chain cannot (parts hold children, never a parent or the
 * model), and neither can a GeckoLib {@code GeoBone} (its parent chain stops at a top-level bone). But
 * Citadel's {@code AdvancedModelBox} and LLibrary's {@code AdvancedModelRenderer} each hold a {@code model}
 * field pointing back at the very object keying the entry, so those entries pin themselves — the weak key
 * is strongly reachable from the map's own value — and would hold a whole model's box tree for the life of
 * the process. Weak keys therefore cover the safe families only; {@link #clear()} covers the rest.
 *
 * <p><b>Misses are cached.</b> A token naming no part is the expensive case, not the cheap one: a lookup
 * that fails has walked the model's whole part tree, having found no match to stop at. So a {@code null}
 * from the resolver is stored as a {@code null} value, and {@link Map#containsKey} — not the value —
 * decides whether the resolver runs.
 *
 * <p>Single-threaded: the client's main thread both ticks and renders, and every caller (render layers,
 * picker) lives on it. Hence the plain inner {@link HashMap}.
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
        V cached = tokens.get(token);
        if (cached != null || tokens.containsKey(token)) {
            return cached;
        }
        V resolved = resolver.resolve(model, token);
        tokens.put(token, resolved);
        return resolved;
    }
}
