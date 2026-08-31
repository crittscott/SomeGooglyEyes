package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import com.github.crittscott.somegoogly.registry.ContentRegistrar;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;

/** Data components shared by the Googly Eye and Slimy Eye item stacks. */
public final class ModDataComponents {

    public static final ContentRegistrar.Handle<DataComponentType<AppearanceOverride>> EYE_PROPERTIES =
            new ContentRegistrar.Handle<>();

    private ModDataComponents() {
    }

    public static void register(ContentRegistrar registrar) {
        EYE_PROPERTIES.bind(registrar.registerDataComponent(
                "eye_properties", () -> DataComponentType.<AppearanceOverride>builder()
                        .persistent(AppearanceOverride.CODEC)
                        .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(AppearanceOverride.CODEC))
                        .build()));
    }
}
