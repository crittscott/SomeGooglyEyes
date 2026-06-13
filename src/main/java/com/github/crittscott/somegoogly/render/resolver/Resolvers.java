package com.github.crittscott.somegoogly.render.resolver;

import net.minecraft.client.model.EntityModel;

import java.util.List;

/** Picks the first {@link EyeAttachmentResolver} that handles a given model. */
public final class Resolvers {

    private static final List<EyeAttachmentResolver> ALL = List.of(
            new HierarchicalResolver(),
            new ListModelResolver()
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
