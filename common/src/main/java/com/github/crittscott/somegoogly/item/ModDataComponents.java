package com.github.crittscott.somegoogly.item;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.eye.state.AppearanceOverride;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

/** Data components shared by the Googly Eye and Slimy Eye item stacks. */
public final class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(SomeGooglyCommon.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<AppearanceOverride>> EYE_PROPERTIES =
            COMPONENTS.register("eye_properties", () -> DataComponentType.<AppearanceOverride>builder()
                    .persistent(AppearanceOverride.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodecWithRegistries(AppearanceOverride.CODEC))
                    .build());

    private ModDataComponents() {
    }

    public static void register() {
        COMPONENTS.register();
    }
}
