package com.github.crittscott.somegoogly.client.picker;

import com.github.crittscott.somegoogly.client.compat.GeckoCompat;
import com.github.crittscott.somegoogly.client.render.resolver.EyeAttachmentResolver;
import com.github.crittscott.somegoogly.client.render.resolver.Resolvers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The picker's single view of a living model's attachment vocabulary. It composes ordinary model
 * resolvers and the optional GeckoLib bridge so choosing, draft seeding, and bulk export enumerate
 * and canonicalize tokens by the same policy.
 */
public final class ModelPartVocabulary {

    private final Canonicalizer canonicalizer;
    private final List<String> tokens;

    private ModelPartVocabulary(List<String> tokens, Canonicalizer canonicalizer) {
        this.tokens = List.copyOf(tokens);
        this.canonicalizer = canonicalizer;
    }

    @Nullable
    public static ModelPartVocabulary forEntity(LivingEntity living) {
        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(living);
        if (renderer instanceof LivingEntityRenderer<?, ?> livingRenderer) {
            EntityModel<?> model = livingRenderer.getModel();
            EyeAttachmentResolver resolver = Resolvers.forModel(model);
            if (resolver != null) {
                List<String> tokens = resolver.enumerateParts(model);
                if (!tokens.isEmpty()) {
                    return new ModelPartVocabulary(tokens, token -> resolver.canonicalToken(model, token));
                }
            }
        }

        List<String> bones = List.copyOf(GeckoCompat.enumerate(renderer, living));
        return bones.isEmpty() ? null
                : new ModelPartVocabulary(bones, token -> canonicalizeEnumerated(bones, token));
    }

    /** Resolve a type through a throwaway client entity so export-all does not depend on nearby mobs. */
    @Nullable
    public static ModelPartVocabulary forType(ResourceLocation id) {
        LivingEntity sample = sampleFor(id);
        return sample == null ? null : forEntity(sample);
    }

    public String canonicalize(String token) {
        return canonicalizer.canonicalize(token);
    }

    public List<String> tokens() {
        return tokens;
    }

    private static String canonicalizeEnumerated(List<String> tokens, String storedToken) {
        for (String token : tokens) {
            if (token.equals(storedToken)) {
                return token;
            }
        }
        String match = null;
        for (String token : tokens) {
            if (!EyeAttachmentResolver.pathMatches(storedToken, token)) {
                continue;
            }
            if (match != null) {
                return storedToken;
            }
            match = token;
        }
        return match != null ? match : storedToken;
    }

    @Nullable
    private static LivingEntity sampleFor(ResourceLocation id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.PLAYER))) {
            return minecraft.player;
        }
        ClientLevel level = minecraft.level;
        EntityType<?> type = level == null ? null : BuiltInRegistries.ENTITY_TYPE.get(id);
        if (type == null) {
            return null;
        }
        try {
            return type.create(level) instanceof LivingEntity living ? living : null;
        } catch (Throwable constructionFailed) {
            return null;
        }
    }

    @FunctionalInterface
    private interface Canonicalizer {
        String canonicalize(String token);
    }
}
