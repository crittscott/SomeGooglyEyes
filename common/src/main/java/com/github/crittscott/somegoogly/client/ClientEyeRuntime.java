package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.eye.HeadInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Loader-neutral owner of client eye trackers and their simulation lifecycle. */
public final class ClientEyeRuntime {

    private static final Map<LivingEntity, GooglyTracker> TRACKERS = new HashMap<>();
    private static int clientTicks;

    private ClientEyeRuntime() {
    }

    public static void clear() {
        TRACKERS.clear();
    }

    public static int clientTicks() {
        return clientTicks;
    }

    public static GooglyTracker get(LivingEntity living, HeadInfo helper) {
        GooglyTracker tracker = TRACKERS.get(living);
        if (tracker == null || !tracker.matches(helper)) {
            tracker = new GooglyTracker(living, helper);
            TRACKERS.put(living, tracker);
        }
        return tracker;
    }

    @Nullable
    public static GooglyTracker peek(LivingEntity living) {
        return TRACKERS.get(living);
    }

    public static void tick() {
        clientTicks++;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        Iterator<Map.Entry<LivingEntity, GooglyTracker>> iterator = TRACKERS.entrySet().iterator();
        while (iterator.hasNext()) {
            GooglyTracker tracker = iterator.next().getValue();
            int idle = clientTicks - tracker.lastRenderTick;
            if (idle > 10) {
                iterator.remove();
            } else if (idle <= 1) {
                tracker.update();
            }
        }
    }
}
