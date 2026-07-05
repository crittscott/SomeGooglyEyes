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
            new AgeableListResolver(),
            new CitadelResolver(),
            new LLibraryResolver(),
            new ChildMapResolver()
    );

    private Resolvers() {
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
