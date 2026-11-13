package com.github.crittscott.somegoogly.eye.behavior;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.config.ConfigValue;
import com.github.crittscott.somegoogly.config.ServerConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ProjectileWeaponItem;

import org.jspecify.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * The named eye behaviors that last as long as the mob stays in some state, as opposed to the one-shot
 * {@link EyeBehavior} expressions. Each client derives a state from what vanilla already syncs, so nothing about
 * one crosses the mod's own network except its server switch, sent with the eye definitions in
 * {@link BehaviorSettings}. A state never blocks an expression: the client folds it in on top of whatever plays.
 *
 * <p>Each entry owns its id, its server switch, and the vanilla state that drives it ({@link #level}); how the
 * eyes show it lives with the eye simulation in {@code GooglyTracker}. The one state with a server-reported half
 * is {@link #ANGER}: anger vanilla keeps on the server reaches clients through {@code EyeAngerPacket}, and the
 * tracker folds it into this level.
 *
 * <p>This type must stay free of client-only imports: the server class-loads it to build the synced settings.
 */
public enum OngoingBehavior {

    /**
     * A skeleton drawing a ranged weapon, or a player drawing a bow, closes the eye away from the arm holding
     * it. A player's crossbow doesn't count: charging it is item use, but aiming it once loaded is not.
     */
    AIM_WINK("aim_wink", ServerConfig.WINK_ON_AIM) {
        @Override
        public float level(LivingEntity entity) {
            if (!entity.isUsingItem()) {
                return 0F;
            }
            Item weapon = entity.getUseItem().getItem();
            return on(entity instanceof AbstractSkeleton && weapon instanceof ProjectileWeaponItem
                    || entity instanceof Player && weapon instanceof BowItem);
        }
    },

    /**
     * An attacking mob's pupils ease to red; so do those of a mob the server reports angry, which the tracker
     * adds to this level.
     */
    ANGER("anger", ServerConfig.ANGRY_EYES) {
        @Override
        public float level(LivingEntity entity) {
            return on(entity instanceof Mob mob && mob.isAggressive());
        }
    },

    /** A creepy enderman's pupils pulse at a rate of its own. */
    CREEPY_PULSE("creepy_pulse", ServerConfig.PULSE_WHEN_CREEPY) {
        @Override
        public float level(LivingEntity entity) {
            return on(entity instanceof Enderman enderman && enderman.isCreepy());
        }
    },

    /** A goat lowering its head to ram squints. */
    RAM_SQUINT("ram_squint", ServerConfig.SQUINT_ON_RAM) {
        @Override
        public float level(LivingEntity entity) {
            return on(entity instanceof Goat goat && goat.getRammingXHeadRot() > 0F);
        }
    },

    /** A sleeping mob squints, or, for a villager, lowers its lids to a slit. */
    SLEEP_SQUINT("sleep_squint", ServerConfig.SQUINT_ON_SLEEP) {
        @Override
        public float level(LivingEntity entity) {
            return on(entity.isSleeping());
        }
    },

    /** A swelling creeper's pupils rattle, harder the further the swell has gone. */
    SWELL_RATTLE("swell_rattle", ServerConfig.RATTLE_ON_SWELL) {
        @Override
        public float level(LivingEntity entity) {
            // getSwelling runs a little past 1 at the end of the fuse.
            return entity instanceof Creeper creeper ? Math.min(1F, creeper.getSwelling(1F)) : 0F;
        }
    };

    private static final Map<Identifier, OngoingBehavior> BY_ID = new HashMap<>();

    static {
        for (OngoingBehavior behavior : values()) {
            BY_ID.put(behavior.id, behavior);
        }
    }

    private final Identifier id;
    private final ConfigValue<Boolean> setting;

    OngoingBehavior(String name, ConfigValue<Boolean> setting) {
        this.id = SomeGooglyCommon.id(name);
        this.setting = setting;
    }

    /** Look up a behavior by id, or {@code null} for an unregistered identifier. */
    @Nullable
    public static OngoingBehavior byId(Identifier id) {
        return BY_ID.get(id);
    }

    /** The id (e.g. {@code somegoogly:sleep_squint}); used on the wire. */
    public Identifier id() {
        return id;
    }

    /**
     * How strongly this behavior applies to {@code entity} right now, from 0 (not at all) to 1, read from state
     * vanilla syncs to every viewer. Most are on or off; the swell rattle grows with the swell.
     */
    public abstract float level(LivingEntity entity);

    private static float on(boolean on) {
        return on ? 1F : 0F;
    }

    /**
     * Anger the client can't see for itself, kept by vanilla on the server: a neutral mob (iron golem, zombified
     * piglin, enderman, wolf, bee, ...) with anger time left, or a piglin with someone to be angry at. Always
     * {@code false} for any behavior but {@link #ANGER}, or while its switch is off.
     */
    public boolean serverAngry(LivingEntity entity) {
        if (this != ANGER || !setting.get()) {
            return false;
        }
        return entity instanceof NeutralMob neutral ? neutral.isAngry()
                : entity instanceof AbstractPiglin && entity.getBrain().hasMemoryValue(MemoryModuleType.ANGRY_AT);
    }

    /** The server switch that enables this behavior. */
    public ConfigValue<Boolean> setting() {
        return setting;
    }
}
