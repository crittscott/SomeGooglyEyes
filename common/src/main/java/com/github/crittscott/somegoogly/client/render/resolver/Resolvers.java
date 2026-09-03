package com.github.crittscott.somegoogly.client.render.resolver;

import net.minecraft.client.model.EntityModel;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Picks the first {@link EyeAttachmentResolver} that handles a given model. Order matters: the
 * named-model resolvers come first (they give the cleanest, most stable tokens), and
 * {@link ChildMapResolver} is the catch-all last (it handles every model, falling back to positional
 * root names where no stable name exists).
 */
public final class Resolvers {

    private static final List<EyeAttachmentResolver> ALL = List.of(
            new HierarchicalResolver(),
            new TwilightForestResolver(),
            new AgeableListResolver(),
            new CitadelResolver(),
            new LLibraryResolver(),
            new RabbitLlamaResolver(),
            new ChildMapResolver()
    );

    // Which resolver handles a given model instance never changes for that instance's life, and the
    // resolvers themselves are stateless singletons holding no reference back to any model, so weak keys
    // alone are enough here (unlike ModelMemo's cached values, nothing pins a stale model alive).
    private static final Map<EntityModel<?>, EyeAttachmentResolver> BY_MODEL = new WeakHashMap<>();

    private Resolvers() {
    }

    /**
     * Drop every model-keyed cache. Call when the models themselves are replaced — a client resource
     * reload rebuilds each renderer and its model, so every cached part, box, and chain then points into
     * a dead model. Weak keys alone are not enough: a Citadel/LLibrary box holds its model, so those
     * entries keep their own key alive (see {@link ModelMemo}).
     */
    public static void clearCaches() {
        AttachmentCache.ATTACHMENTS.clear();
        BY_MODEL.clear();
        for (EyeAttachmentResolver r : ALL) {
            r.clearModelCache();
        }
    }

    /** @return a resolver for the model, or {@code null} if none handles it. */
    public static EyeAttachmentResolver forModel(EntityModel<?> model) {
        return BY_MODEL.computeIfAbsent(model, Resolvers::findResolver);
    }

    private static EyeAttachmentResolver findResolver(EntityModel<?> model) {
        for (EyeAttachmentResolver r : ALL) {
            if (r.handles(model)) {
                return r;
            }
        }
        return null;
    }
}
