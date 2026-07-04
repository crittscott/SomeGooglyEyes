package com.github.crittscott.somegoogly.event;

import com.github.crittscott.somegoogly.client.GooglyTracker;
import com.github.crittscott.somegoogly.client.compat.GeckoCompat;
import com.github.crittscott.somegoogly.client.picker.PickerLayer;
import com.github.crittscott.somegoogly.client.picker.PickerState;
import com.github.crittscott.somegoogly.client.render.LayerGooglyEyes;
import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.eye.HeadInfo;
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

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class ClientEventHandler {
    private static final Marker MARK = MarkerManager.getMarker(ClientEventHandler.class.getSimpleName());
    public static int clientTicks = 0;
    // A plain map on purpose: entries are evicted by the 10-tick sweep in onWorldTick (plus the
    // clearTrackers calls on sync/disconnect). This was a WeakHashMap, but each GooglyTracker holds a
    // strong reference to its own key (the parent entity), so weakness never fired anyway.
    protected final Map<LivingEntity, GooglyTracker> trackers = new HashMap<>();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void addLayers() {
        HashSet<LivingEntityRenderer> addedRenderers = new HashSet<>();

        EntityRenderDispatcher renderManager = Minecraft.getInstance().getEntityRenderDispatcher();

        if (!ClientConfig.isEntityDisabled(new ResourceLocation("minecraft", "player"))) {
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
            if (ClientConfig.isEntityDisabled(rl)) {
                return;
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

    /** Drop all trackers; called when configs change (sync) or on disconnect. */
    public void clearTrackers() {
        trackers.clear();
    }

    public GooglyTracker getGooglyTracker(LivingEntity living, HeadInfo helper) {
        GooglyTracker tracker = trackers.get(living);
        if (tracker == null || !tracker.matches(helper)) {
            tracker = new GooglyTracker(living, helper);
            trackers.put(living, tracker);
        }
        return tracker;
    }

    @SubscribeEvent
    public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        // Forget the previous server's configs so they don't leak onto the next connection.
        ClientEyeConfigs.clear();
        clearTrackers();
        // Reset picker state without sending anything (the connection is going away); the server's
        // own logout handling (PickerFreezeService) releases any mob this player had frozen.
        PickerState.resetOnDisconnect();
    }

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

    /**
     * The mob's existing tracker, or {@code null} if it has none yet (i.e. it isn't currently being
     * rendered). Used by the behavior trigger packet, which drops triggers for mobs with no tracker —
     * an off-screen mob has nothing to animate.
     */
    @Nullable
    public GooglyTracker peekGooglyTracker(LivingEntity living) {
        return trackers.get(living);
    }
}
