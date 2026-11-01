package com.github.crittscott.somegoogly.client;

import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.config.ClientEyeConfigs;
import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.eye.BoatEyeLayout;
import com.github.crittscott.somegoogly.eye.HeadInfo;
import com.github.crittscott.somegoogly.eye.behavior.BehaviorSettings;
import com.github.crittscott.somegoogly.eye.state.EyeState;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.level.Level;

/** World-owned boat pupils, retained and simulated only while recently rendered. */
public final class BoatEyeAnimations {

    private static final int EVICT_IDLE_TICKS = 10;
    private static final int SIMULATE_IDLE_TICKS = 1;

    private final Map<AbstractBoat, Entry> entries = new HashMap<>();
    private long ticks;

    private static final class Entry {
        final String attachment;
        final RuntimeConfig config;
        final ItemEyes eyes;
        long lastShown;

        Entry(String attachment, RuntimeConfig config, ItemEyes eyes) {
            this.attachment = attachment;
            this.config = config;
            this.eyes = eyes;
        }

        boolean matches(AbstractBoat boat, RuntimeConfig current) {
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
    public ItemEyes get(AbstractBoat boat) {
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
                    ItemEyes.create(boat.getUUID(), type, snapshot, helper));
            entries.put(boat, entry);
        }
        entry.lastShown = ticks;
        return entry.eyes;
    }

    private static boolean mayShow(AbstractBoat boat, @Nullable RuntimeConfig config) {
        return ClientEyeConfigs.googlyEyesEnabled() && !ClientConfig.DISABLE_GOOGLY_EYES.get()
                && !ClientConfig.isEntityDisabled(EntityType.getKey(boat.getType()))
                && !boat.isInvisible() && !boat.isRemoved() && EyeState.hasEyes(boat)
                && BoatEyeLayout.isEligible(boat, config);
    }

    /** Entity unloading immediately releases the boat's local simulation. */
    public void remove(Entity entity) {
        entries.remove(entity);
    }

    /** World time selects expressions; pause stops physics and idle aging, and absent levels clear state. */
    public void tick(@Nullable Level level, long gameTime, boolean simulate, BehaviorSettings settings) {
        if (level == null) {
            clear();
            return;
        }
        if (!simulate) {
            return;
        }
        ticks++;
        Iterator<Map.Entry<AbstractBoat, Entry>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            var item = iterator.next();
            AbstractBoat boat = item.getKey();
            Entry entry = item.getValue();
            long idle = ticks - entry.lastShown;
            RuntimeConfig config = ClientEyeConfigs.get(EntityType.getKey(boat.getType()), false);
            if (idle > EVICT_IDLE_TICKS || boat.level() != level || !mayShow(boat, config)
                    || !entry.matches(boat, config)) {
                iterator.remove();
            } else if (idle <= SIMULATE_IDLE_TICKS) {
                entry.eyes.tick(gameTime, settings, boat);
            }
        }
    }
}
