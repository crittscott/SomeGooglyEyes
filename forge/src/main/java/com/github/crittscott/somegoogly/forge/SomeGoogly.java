package com.github.crittscott.somegoogly.forge;

import com.github.crittscott.somegoogly.SomeGooglyCommon;
import com.github.crittscott.somegoogly.client.ClientNetworkHandler;
import com.github.crittscott.somegoogly.client.forge.EyeInspectIndicator;
import com.github.crittscott.somegoogly.client.forge.SlimyEyeColors;
import com.github.crittscott.somegoogly.client.picker.forge.ForgePickerClient;
import com.github.crittscott.somegoogly.command.GooglyClientCommands;
import com.github.crittscott.somegoogly.command.MaybeFloatArgumentType;
import com.github.crittscott.somegoogly.config.forge.ForgeClientConfig;
import com.github.crittscott.somegoogly.config.forge.ForgeServerConfig;
import com.github.crittscott.somegoogly.event.ClientEventHandler;
import com.github.crittscott.somegoogly.event.EyeItemInteractions;
import com.github.crittscott.somegoogly.event.EyeReactionHandler;
import com.github.crittscott.somegoogly.event.ServerEventHandler;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import dev.architectury.platform.forge.EventBuses;
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
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(MOD_ID, modEventBus);
        COMMAND_ARGUMENTS.register(modEventBus);
        SomeGooglyCommon.init();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientNetworkHandler.register();
            ClientCommandRegistrationEvent.EVENT.register(GooglyClientCommands::register);
            ForgeClientConfig.register();

            MinecraftForge.EVENT_BUS.register(clientEventHandler = new ClientEventHandler());

            // Sneak + held eye item → action-bar verdict on whether the aimed mob could wear eyes.
            MinecraftForge.EVENT_BUS.register(new EyeInspectIndicator());

            // The slimy eye's iris layer is tinted from the appearance its stack carries.
            FMLJavaModLoadingContext.get().getModEventBus().addListener(SlimyEyeColors::register);

            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::addLayers);

            // Part picker (authoring tool).
            FMLJavaModLoadingContext.get().getModEventBus().addListener(ForgePickerClient::registerHud);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(ForgePickerClient::registerKeys);
            MinecraftForge.EVENT_BUS.register(new ForgePickerClient());

        });

        ForgeServerConfig.register();

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
