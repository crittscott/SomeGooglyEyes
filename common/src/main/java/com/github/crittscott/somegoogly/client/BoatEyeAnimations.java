package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.BoatEyeLayout;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;

/** World-owned boat pupils, retained and simulated only while recently rendered. */
public final class BoatEyeAnimations {

    private final Map<Boat, Entry> entries = new HashMap<>();
    private long ticks;

    private static final class Entry {
        final String attachment;
        final RuntimeConfig config;
        final PresentationEyes eyes;
        long lastShown;

        Entry(String attachment, RuntimeConfig config, PresentationEyes eyes) {
            this.attachment = attachment;
            this.config = config;
            this.eyes = eyes;
        }

        boolean matches(Boat boat, RuntimeConfig current) {
            return config == current && attachment.equals(BoatEyeLayout.attachment(boat))
                    && eyes.snapshot.equals(EyeState.snapshot(boat));
        }
    }

    /** Forget disposable pupils on resource/config reload, disconnect, or level replacement. */
    public void clear() {
        entries.clear();
        ticks = 0;
    }

    /** Return the rendered boat's current layout, replacing simulations after state or geometry changes. */
    @Nullable
    public PresentationEyes get(Boat boat) {
        var type = EntityType.getKey(boat.getType());
        RuntimeConfig config = ClientEyeConfigs.get(type, false);
        if (!mayShow(boat, config)) {
            entries.remove(boat);
            return null;
        }
        Entry entry = entries.get(boat);
        if (entry == null || !entry.matches(boat, config)) {
            var snapshot = EyeState.snapshot(boat);
            HeadInfo helper = BoatEyeLayout.resolve(boat, config, snapshot.variantRoll());
            entry = new Entry(BoatEyeLayout.attachment(boat), config,
                    PresentationEyes.create(boat.getUUID(), type, snapshot, helper));
            entries.put(boat, entry);
        }
        entry.lastShown = ticks;
        return entry.eyes;
    }

    private static boolean mayShow(Boat boat, @Nullable RuntimeConfig config) {
        return ClientEyeConfigs.eyesVisible()
                && !ClientConfig.isEntityDisabled(EntityType.getKey(boat.getType()))
                && !boat.isInvisible() && !boat.isRemoved() && EyeState.hasEyes(boat)
                && BoatEyeLayout.isEligible(boat, config);
    }

    /** Entity unloading immediately releases the boat's local simulation. */
    public void remove(Entity entity) {
        entries.remove(entity);
    }

    /** Step one unpaused tick; world time selects expressions. */
    public void tick(Level level, long gameTime, BehaviorSettings settings) {
        ticks++;
        ClientEyeRuntime.sweep(entries, ticks, entry -> entry.lastShown, (boat, entry) -> {
            RuntimeConfig config = ClientEyeConfigs.get(EntityType.getKey(boat.getType()), false);
            return boat.level() != level || !mayShow(boat, config) || !entry.matches(boat, config);
        }, (boat, entry) -> entry.eyes.tick(gameTime, settings, boat));
    }
}
