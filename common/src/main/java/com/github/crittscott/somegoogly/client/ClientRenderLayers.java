package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.client.compat.GeckoCompat;
import com.github.crittscott.somegoogly.client.picker.PickerLayer;
import com.github.crittscott.somegoogly.client.render.LayerGooglyEyes;
import com.github.crittscott.somegoogly.client.render.resolver.Resolvers;
import com.github.crittscott.somegoogly.config.ClientConfig;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Installs vanilla-model, picker, and optional GeckoLib eye layers after renderer rebuilds. */
public final class ClientRenderLayers {

    private static final Set<EntityRenderer<?>> INSTALLED =
            Collections.newSetFromMap(new WeakHashMap<>());

    private ClientRenderLayers() {
    }

    /**
     * Reinstall every eye layer across the whole dispatcher: clear the attachment caches, then walk the
     * player skin map and the per-type renderer map, adding the vanilla-model layers to living renderers
     * and the optional GeckoLib layer to the rest. Duplicate-safe through the weak {@code INSTALLED} set,
     * and skips any entity hidden by {@link ClientConfig}. Call after a renderer rebuild.
     */
    @SuppressWarnings("rawtypes")
    public static void install(EntityRenderDispatcher dispatcher) {
        Resolvers.clearCaches();
        HashSet<LivingEntityRenderer> playerRenderers = new HashSet<>();

        if (!ClientConfig.isEntityDisabled(ResourceLocation.fromNamespaceAndPath("minecraft", "player"))) {
            Map<?, EntityRenderer<? extends Player>> skinMap = dispatcher.playerRenderers;
            for (EntityRenderer<? extends Player> renderer : skinMap.values()) {
                if (renderer instanceof PlayerRenderer playerRenderer) {
                    addLiving(playerRenderer);
                    playerRenderers.add(playerRenderer);
                }
            }
        }

        dispatcher.renderers.forEach((entityType, renderer) -> {
            if (playerRenderers.contains(renderer)) {
                return;
            }
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            if (ClientConfig.isEntityDisabled(id)) {
                return;
            }
            if (renderer instanceof LivingEntityRenderer livingRenderer) {
                addLiving(livingRenderer);
            } else if (INSTALLED.add(renderer)) {
                GeckoCompat.tryAddLayer(renderer);
            }
        });
    }

    /** Add the shared vanilla-model layers through a loader's native renderer-registration event. */
    @SuppressWarnings("rawtypes")
    public static boolean installLiving(EntityType<? extends LivingEntity> entityType,
                                        LivingEntityRenderer<?, ?> renderer) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        if (ClientConfig.isEntityDisabled(id)) {
            return false;
        }
        return addLiving(renderer);
    }

    /** Refresh attachment caches and install optional layers on non-vanilla renderer families. */
    public static int refreshNonLiving(EntityRenderDispatcher dispatcher) {
        Resolvers.clearCaches();
        int[] installed = {0};
        dispatcher.renderers.forEach((entityType, renderer) -> {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            if (!(renderer instanceof LivingEntityRenderer)
                    && !ClientConfig.isEntityDisabled(id)
                    && INSTALLED.add(renderer)) {
                GeckoCompat.tryAddLayer(renderer);
                installed[0]++;
            }
        });
        return installed[0];
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean addLiving(LivingEntityRenderer renderer) {
        if (!INSTALLED.add(renderer)) {
            return false;
        }
        LayerGooglyEyes eyes = new LayerGooglyEyes<>(renderer);
        PickerLayer picker = new PickerLayer<>(renderer);
        List<RenderLayer> layers = renderer.layers;
        for (int i = 0; i < layers.size(); i++) {
            if (layers.get(i) instanceof SlimeOuterLayer) {
                layers.add(i, eyes);
                layers.add(i + 1, picker);
                return true;
            }
        }
        renderer.addLayer(eyes);
        renderer.addLayer(picker);
        return true;
    }
}
