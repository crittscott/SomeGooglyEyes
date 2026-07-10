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

    /**
     * Attach points resolved once per (model instance, token) and replayed every frame, shared by all four
     * families. Entries live as long as their model does: a datapack reload must <b>not</b> clear this
     * (new configs change which token is asked for, not what a token names inside a model), while a
     * resource reload must — see {@link #clearCaches()}.
     */
    static final ModelMemo<EntityModel<?>, Attachment> ATTACHMENTS = new ModelMemo<>();

    private static final List<EyeAttachmentResolver> ALL = List.of(
            new HierarchicalResolver(),
            new AgeableListResolver(),
            new CitadelResolver(),
            new LLibraryResolver(),
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
        ATTACHMENTS.clear();
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
