package com.github.crittscott.somegoogly.client.render.resolver;

import net.minecraft.client.model.EntityModel;

import java.util.List;

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
        for (EyeAttachmentResolver r : ALL) {
            r.clearModelCache();
        }
    }

    /** @return a resolver for the model, or {@code null} if none handles it. */
    public static EyeAttachmentResolver forModel(EntityModel<?> model) {
        for (EyeAttachmentResolver r : ALL) {
            if (r.handles(model)) {
                return r;
            }
        }
        return null;
    }
}
