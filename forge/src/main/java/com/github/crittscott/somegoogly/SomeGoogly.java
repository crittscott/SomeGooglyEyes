package com.github.crittscott.somegoogly;

import com.github.crittscott.somegoogly.client.EyeInspectIndicator;
import com.github.crittscott.somegoogly.client.SlimyEyeColors;
import com.github.crittscott.somegoogly.client.picker.PickerHud;
import com.github.crittscott.somegoogly.client.picker.PickerInput;
import com.github.crittscott.somegoogly.client.picker.PickerKeys;
import com.github.crittscott.somegoogly.command.GooglyClientCommands;
import com.github.crittscott.somegoogly.command.MaybeFloatArgumentType;
import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.enchant.ModEnchantments;
import com.github.crittscott.somegoogly.event.ClientEventHandler;
import com.github.crittscott.somegoogly.event.EyeItemInteractions;
import com.github.crittscott.somegoogly.event.EyeReactionHandler;
import com.github.crittscott.somegoogly.event.ServerEventHandler;
import com.github.crittscott.somegoogly.item.ModCreativeTabs;
import com.github.crittscott.somegoogly.item.ModItems;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.recipe.ModRecipes;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Forge entry point for Some Googly Eyes. Registers shared content, configuration, networking, and
 * event handlers, while keeping client-only rendering, input, and picker setup behind a physical-side
 * guard. Datapack eye definitions are loaded later through the server reload lifecycle.
 */
@Mod(SomeGoogly.MOD_ID)
public class SomeGoogly {
    public static final String MOD_ID = SomeGooglyCommon.MOD_ID;
    public static final String MOD_NAME = SomeGooglyCommon.MOD_NAME;
    public static final Logger LOGGER = SomeGooglyCommon.LOGGER;

    // Out of alphabetical order by necessity: this initializer reads MOD_ID by simple name (so it must
    // follow MOD_ID) and MAYBE_FLOAT reads this one (so it must precede MAYBE_FLOAT). Either forward
    // reference would be a compile error (JLS 8.3.3), even though MOD_ID is a constant.
    private static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENTS =
            DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, MOD_ID);

    public static final RegistryObject<SingletonArgumentInfo<MaybeFloatArgumentType>> MAYBE_FLOAT =
            COMMAND_ARGUMENTS.register("maybe_float", () -> ArgumentTypeInfos.registerByClass(
                    MaybeFloatArgumentType.class,
                    SingletonArgumentInfo.contextFree(MaybeFloatArgumentType::maybeFloat)));

    public static ClientEventHandler clientEventHandler;

    public SomeGoogly() {
        // Eye configs are loaded from datapacks on the server (EyeConfigReloadListener) and synced
        // to clients (EyeConfigSyncPacket); nothing to load at construction time.
        NetworkHandler.register();
        COMMAND_ARGUMENTS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModEnchantments.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModItems.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModCreativeTabs.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModRecipes.register(FMLJavaModLoadingContext.get().getModEventBus());

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientConfig.register();

            MinecraftForge.EVENT_BUS.register(clientEventHandler = new ClientEventHandler());

            // Sneak + held eye item → action-bar verdict on whether the aimed mob could wear eyes.
            MinecraftForge.EVENT_BUS.register(new EyeInspectIndicator());

            // The slimy eye's iris layer is tinted from the appearance its stack carries.
            FMLJavaModLoadingContext.get().getModEventBus().addListener(SlimyEyeColors::register);

            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::addLayers);

            // Part picker (authoring tool).
            FMLJavaModLoadingContext.get().getModEventBus().addListener(PickerHud::register);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(PickerKeys::register);
            MinecraftForge.EVENT_BUS.register(new PickerInput());

            // Client commands (/sg spawnall, /sg set …) for the authoring workflow.
            MinecraftForge.EVENT_BUS.register(new GooglyClientCommands());
        });

        ServerConfig.register();

        MinecraftForge.EVENT_BUS.register(new EyeItemInteractions());
        MinecraftForge.EVENT_BUS.register(new EyeReactionHandler());
        MinecraftForge.EVENT_BUS.register(new ServerEventHandler());

        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> "1", (a, b) -> true));
    }

    private void addLayers(EntityRenderersEvent.AddLayers event) {
        clientEventHandler.addLayers();
    }
}
