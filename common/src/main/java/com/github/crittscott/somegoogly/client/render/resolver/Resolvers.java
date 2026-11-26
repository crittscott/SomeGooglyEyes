package com.github.crittscott.somegoogly.client.render.resolver;

import com.github.crittscott.somegoogly.client.compat.ThirdPartyModelWraps;

import net.minecraft.client.model.EntityModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

/**
 * Picks the first {@link EyeAttachmentResolver} that handles a given model. Order matters: the
 * named-model resolvers come first (they give the cleanest, most stable tokens), and {@link
 * ChildMapResolver} is the catch-all last (it handles every model, falling back to positional root
 * names where no stable name exists).
 */
public final class Resolvers {

    /**
     * Attach points resolved once per (model instance, token) and replayed every frame, shared by
     * every resolver. A datapack reload must <b>not</b> clear this (new configs change which token
     * is asked for, not what a token names inside a model); {@link #clearCaches} clears it when the
     * models are replaced.
     */
    static final ModelMemo<EntityModel<?>, Attachment> ATTACHMENTS = new ModelMemo<>();

    // Which resolver handles a given model instance, and its whole-model wrap, never change for
    // that instance's life. The resolvers are stateless singletons and the wraps are plain
    // transforms, neither holding a reference back to any model, so weak keys alone are enough here
    // (nothing in the values pins a stale model alive).
    private static final Map<EntityModel<?>, Binding> BY_MODEL = new WeakHashMap<>();

    private static final EyeAttachmentResolver FALLBACK = new ChildMapResolver();
    private static final List<EyeAttachmentResolver> SPECIALIZED =
            List.of(
                    new HierarchicalResolver(),
                    new AgeableListResolver(),
                    AdvancedModelBoxResolver.citadel(),
                    AdvancedModelBoxResolver.uranus(),
                    new LLibraryResolver(),
                    new RabbitLlamaResolver());

    private Resolvers() {}

    /**
     * What is fixed about one model instance: its resolver and its whole-model wrap, if it has one.
     */
    private record Binding(
            EyeAttachmentResolver resolver, @Nullable ThirdPartyModelWraps.Entry wholeModel) {}

    /**
     * The log names of the reflected model families whose library this client found and could bind
     * to.
     */
    public static List<String> availableFamilies() {
        List<String> families = new ArrayList<>();
        for (EyeAttachmentResolver r : SPECIALIZED) {
            if (r instanceof ReflectedBoxResolver reflected && reflected.available()) {
                families.add(reflected.familyLabel());
            }
        }
        return families;
    }

    private static Binding bind(EntityModel<?> model) {
        EyeAttachmentResolver resolver = FALLBACK;
        for (EyeAttachmentResolver r : SPECIALIZED) {
            if (r.handles(model)) {
                resolver = r;
                break;
            }
        }
        return new Binding(resolver, ThirdPartyModelWraps.wholeModel(model));
    }

    /**
     * Drop every model-keyed cache. Call when the models themselves are replaced — a client
     * resource reload rebuilds each renderer and its model, so every cached part, box, and chain
     * then points into a dead model, and {@link ModelMemo} entries go only here.
     */
    public static void clearCaches() {
        ATTACHMENTS.clear();
        BY_MODEL.clear();
        FALLBACK.clearModelCache();
        for (EyeAttachmentResolver r : SPECIALIZED) {
            r.clearModelCache();
        }
    }

    /**
     * @return the specialized resolver for this model, or the child-map fallback.
     */
    public static EyeAttachmentResolver forModel(EntityModel<?> model) {
        return BY_MODEL.computeIfAbsent(model, Resolvers::bind).resolver();
    }

    /**
     * @return the whole-model transform this model applies outside its part tree, or {@code null}.
     */
    @Nullable
    public static ThirdPartyModelWraps.Entry wholeModelWrap(EntityModel<?> model) {
        return BY_MODEL.computeIfAbsent(model, Resolvers::bind).wholeModel();
    }
}
