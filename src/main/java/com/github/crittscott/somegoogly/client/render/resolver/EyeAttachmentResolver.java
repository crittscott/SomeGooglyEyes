package com.github.crittscott.somegoogly.client.render.resolver;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Strategy for getting from an entity model's base render pose into the animated space of a named
 * attachment part, so eyes can be drawn relative to it.
 *
 * <p>Different model frameworks expose their part trees differently (see {@link HierarchicalResolver},
 * {@link AgeableListResolver}, {@link CitadelResolver}, and {@link ChildMapResolver}); each converges on
 * the same contract here. Implementations must use <b>obfuscation-safe</b> handles only — string part
 * names walked from a stable entry point / positional root indices, never obfuscated field names.
 *
 * <p>The work splits in two. {@link #resolve} searches the model's part tree by string name and is the
 * expensive half; {@link #toAttachmentSpace} replays the {@link Attachment} it produced onto the pose and
 * is the per-frame half. The split is what lets {@link Resolvers#ATTACHMENTS} memoize the search: a model
 * is a singleton and a token names the same part within it forever.
 */
public interface EyeAttachmentResolver extends ModelMemo.Resolver<EntityModel<?>, Attachment> {

    /** Segment normalization strips everything but letters and digits; compiled once, matched often. */
    Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]");

    /**
     * List selectable attachment tokens for this model, in a stable order (used by the part picker).
     * Default: none (the resolver doesn't support enumeration / picker authoring).
     */
    default List<String> enumerateParts(EntityModel<?> model) {
        return List.of();
    }

    /**
     * Translate a (possibly differently-spelled or partial) stored attach token into the <i>canonical</i>
     * path token this resolver would enumerate for the same part — the single vocabulary the picker, HUD,
     * and exported configs all speak. That is the full slash path ({@code root/body/head}), with a
     * positional segment ({@code #N}) only where a part has no obfuscation-stable name. Returns the token
     * unchanged when it can't be resolved to a part (so unknown tokens pass through untouched rather than
     * being silently remapped).
     *
     * <p>Default: identity (the resolver's enumeration vocabulary already matches stored tokens, e.g.
     * named bones).
     */
    default String canonicalToken(EntityModel<?> model, String storedToken) {
        return storedToken;
    }

    /** Whether this resolver knows how to walk the given model's part tree. */
    boolean handles(EntityModel<?> model);

    /**
     * Drop any per-model state this resolver caches. Called from {@link Resolvers#clearCaches()} when the
     * models themselves are replaced. Default: the resolver keeps none.
     */
    default void clearModelCache() {
    }

    /** Normalizes a single token/part-name segment so camelCase field names match snake_case child-map keys. */
    static String normalize(String s) {
        return s == null ? "" : NON_ALPHANUMERIC.matcher(s.toLowerCase(Locale.ROOT)).replaceAll("");
    }

    /**
     * Split a slash-delimited attach token into normalized, non-empty path segments. A bare name (no
     * slash) yields a one-element list; {@code null}/blank yields an empty list. Each segment is
     * {@link #normalize}d, so {@code "Body/leftHead"} and {@code "body/left_head"} produce the same list.
     */
    static List<String> segments(String token) {
        List<String> out = new ArrayList<>();
        if (token == null) {
            return out;
        }
        for (String part : token.split("/")) {
            String n = normalize(part);
            if (!n.isEmpty()) {
                out.add(n);
            }
        }
        return out;
    }

    /**
     * Whether {@code candidatePath} ends with {@code storedToken} (a normalized <b>suffix</b> match over
     * path segments). So a stored {@code "head"} matches a candidate {@code "root/body/head"}, and a stored
     * {@code "body/head"} matches it too while distinguishing two parts both named {@code head} under
     * different parents. An empty stored token never matches (callers must supply a part).
     */
    static boolean pathMatches(String storedToken, String candidatePath) {
        List<String> want = segments(storedToken);
        if (want.isEmpty()) {
            return false;
        }
        List<String> have = segments(candidatePath);
        if (have.size() < want.size()) {
            return false;
        }
        int offset = have.size() - want.size();
        for (int i = 0; i < want.size(); i++) {
            if (!have.get(offset + i).equals(want.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Find the part {@code partToken} names and return the transforms that reach it, or {@code null} when
     * the model has no such part (an unknown token, or a model whose worn variant lacks it).
     *
     * <p>The expensive half of the contract: a search of the model's part tree, matching each candidate
     * path against the token. Called once per (model, token) — see {@link Resolvers#ATTACHMENTS} — so it
     * may allocate and walk freely.
     *
     * @param partToken the configured attachment token (a string part name, possibly camelCase)
     */
    @Override
    @Nullable
    Attachment resolve(EntityModel<?> model, String partToken);

    /**
     * Move {@code poseStack} into the named part's current (this-frame, post-animation) space.
     * The caller is responsible for {@code pushPose()}/{@code popPose()} around this call.
     *
     * @param partToken the configured attachment token (a string part name, possibly camelCase)
     * @return {@code true} if the part was found and the pose moved; {@code false} otherwise (caller
     *         should skip drawing for this head)
     */
    default boolean toAttachmentSpace(PoseStack poseStack, EntityModel<?> model, String partToken) {
        Attachment attachment = Resolvers.ATTACHMENTS.get(model, partToken, this);
        return attachment != null && attachment.apply(poseStack);
    }
}
