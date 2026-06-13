package com.github.crittscott.somegoogly.event;

import com.github.crittscott.somegoogly.compat.GeckoCompat;
import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.head.HeadInfo;
import com.github.crittscott.somegoogly.picker.PickerLayer;
import com.github.crittscott.somegoogly.picker.PickerState;
import com.github.crittscott.somegoogly.render.LayerGooglyEyes;
import com.github.crittscott.somegoogly.tracker.GooglyTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public class ClientEventHandler {
    public static int clientTicks = 0;
    protected WeakHashMap<LivingEntity, GooglyTracker> trackers = new WeakHashMap<>();
    private static final Marker MARK = MarkerManager.getMarker(ClientEventHandler.class.getSimpleName());

    @SubscribeEvent
    public void onWorldTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            clientTicks++;
            if (Minecraft.getInstance().level != null && !Minecraft.getInstance().isPaused()) {
                Iterator<Map.Entry<LivingEntity, GooglyTracker>> ite = trackers.entrySet().iterator();
                while (ite.hasNext()) {
                    Map.Entry<LivingEntity, GooglyTracker> e = ite.next();
                    GooglyTracker tracker = e.getValue();
                    if (clientTicks - tracker.lastUpdateRequest > 10) {
                        ite.remove();
                    } else {
                        tracker.update();
                    }
                }
            }
        }
    }

    public GooglyTracker getGooglyTracker(LivingEntity living, HeadInfo helper) {
        GooglyTracker tracker = trackers.get(living);
        if (tracker == null) {
            tracker = new GooglyTracker(living, helper);
            trackers.put(living, tracker);
        }
        return tracker;
    }

    /** Drop all trackers; called when configs change (sync) or on disconnect. */
    public void clearTrackers() {
        trackers.clear();
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        // Forget the previous server's configs so they don't leak onto the next connection.
        ClientEyeConfigs.clear();
        clearTrackers();
        // Release any picker-frozen mob and leave picker mode so NoAi can't persist.
        PickerState.active = false;
        PickerState.unlock();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void addLayers() {
        HashSet<LivingEntityRenderer> addedRenderers = new HashSet<>();

        EntityRenderDispatcher renderManager = Minecraft.getInstance().getEntityRenderDispatcher();

        if (!(ClientConfig.DISABLED_ENTITIES.get().contains("minecraft:player") || ClientConfig.DISABLED_ENTITIES.get().contains("player"))) {
            Map<String, EntityRenderer<? extends Player>> skinMap = renderManager.getSkinMap();

            for (Map.Entry<String, EntityRenderer<? extends Player>> e : skinMap.entrySet()) {
                if (e.getValue() instanceof PlayerRenderer) {
                    PlayerRenderer playerRenderer = (PlayerRenderer) e.getValue();
                    playerRenderer.addLayer(new LayerGooglyEyes<>(playerRenderer));
                    playerRenderer.addLayer(new PickerLayer<>(playerRenderer));
                    addedRenderers.add(playerRenderer);
                }
            }
        }

        renderManager.renderers.forEach((entityType, entityRenderer) -> {
            if (addedRenderers.contains(entityRenderer)) {
                return;
            }

            ResourceLocation rl = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            for (String s : ClientConfig.DISABLED_ENTITIES.get()) {
                ResourceLocation disabled = new ResourceLocation(s);
                if (disabled.equals(rl)) {
                    return;
                }
            }

            if (entityRenderer instanceof LivingEntityRenderer) {
                LivingEntityRenderer renderer = (LivingEntityRenderer) entityRenderer;
                renderer.addLayer(new LayerGooglyEyes<>(renderer));
                renderer.addLayer(new PickerLayer<>(renderer));
            } else {
                // GeckoLib (GeoEntityRenderer) isn't a LivingEntityRenderer; attach via soft-dep compat.
                GeckoCompat.tryAddLayer(entityRenderer);
            }
        });
    }
}