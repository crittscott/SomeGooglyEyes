package com.github.crittscott.somegoogly.eye;

import com.github.crittscott.somegoogly.config.EyeConfigModel.RuntimeConfig;
import com.github.crittscott.somegoogly.config.EyeConfigModel.Variant;
import com.github.crittscott.somegoogly.config.EyeConfigModel;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import org.jspecify.annotations.Nullable;

/** Vanilla boat eligibility and hull/raft attachment selection, shared by both sides. */
public final class BoatEyeLayout {

    public static final String HULL = "hull";
    public static final String RAFT = "raft";
    public static final Set<EntityType<? extends AbstractBoat>> TYPES = Set.of(
            EntityTypes.ACACIA_BOAT,
            EntityTypes.ACACIA_CHEST_BOAT,
            EntityTypes.BAMBOO_CHEST_RAFT,
            EntityTypes.BAMBOO_RAFT,
            EntityTypes.BIRCH_BOAT,
            EntityTypes.BIRCH_CHEST_BOAT,
            EntityTypes.CHERRY_BOAT,
            EntityTypes.CHERRY_CHEST_BOAT,
            EntityTypes.DARK_OAK_BOAT,
            EntityTypes.DARK_OAK_CHEST_BOAT,
            EntityTypes.JUNGLE_BOAT,
            EntityTypes.JUNGLE_CHEST_BOAT,
            EntityTypes.MANGROVE_BOAT,
            EntityTypes.MANGROVE_CHEST_BOAT,
            EntityTypes.OAK_BOAT,
            EntityTypes.OAK_CHEST_BOAT,
            EntityTypes.PALE_OAK_BOAT,
            EntityTypes.PALE_OAK_CHEST_BOAT,
            EntityTypes.SPRUCE_BOAT,
            EntityTypes.SPRUCE_CHEST_BOAT);

    private BoatEyeLayout() {
    }

    /** Both tokens use the hull model's composed space, not an animated paddle or cargo part. */
    public static String attachment(AbstractBoat boat) {
        return boat.getType() == EntityTypes.BAMBOO_RAFT || boat.getType() == EntityTypes.BAMBOO_CHEST_RAFT
                ? RAFT : HULL;
    }

    /** Whether {@code boat} is a supported hull and {@code config} has a usable variant with eyes on its attachment. */
    public static boolean isEligible(AbstractBoat boat, @Nullable RuntimeConfig config) {
        return supports(boat) && RuntimeConfig.isUsable(config)
                && config.variants.stream().anyMatch(variant -> matches(variant, attachment(boat)));
    }

    private static boolean matches(Variant variant, String token) {
        return variant.weight > 0 && variant.heads.stream()
                .anyMatch(head -> token.equals(head.attachPoint) && !head.eyes.isEmpty());
    }

    /** Weighted selection considers only variants containing the matching boat geometry. */
    public static HeadInfo resolve(AbstractBoat boat, @Nullable RuntimeConfig config, float roll) {
        String token = attachment(boat);
        return new HeadInfo(config, EyeConfigModel.chooseVariantIndex(config, roll,
                variant -> matches(variant, token)), token);
    }

    public static boolean supports(Entity entity) {
        return entity instanceof AbstractBoat && TYPES.contains(entity.getType());
    }
}
