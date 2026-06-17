package com.github.crittscott.somegoogly;

import com.github.crittscott.somegoogly.command.GooglyClientCommands;
import com.github.crittscott.somegoogly.command.MaybeFloatArgumentType;
import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.event.ClientEventHandler;
import com.github.crittscott.somegoogly.event.ServerEventHandler;
import com.github.crittscott.somegoogly.model.ModelGooglyEye;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.picker.PickerHud;
import com.github.crittscott.somegoogly.picker.PickerInput;
import com.github.crittscott.somegoogly.picker.PickerKeys;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SomeGoogly.MOD_ID)
public class SomeGoogly {
    public static final String MOD_NAME = "Some Googly Eyes";
    public static final String MOD_ID = "somegoogly";

    public static final Logger LOGGER = LogManager.getLogger();

    public static final ModelLayerLocation GOOGLY_EYE_LAYER = new ModelLayerLocation(new ResourceLocation("somegoogly:googly_eye"), "main");

    // Custom command argument type for the /sg CLI (the "float or ~" no-op used by move/rot).
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

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientConfig.register();

            MinecraftForge.EVENT_BUS.register(clientEventHandler = new ClientEventHandler());

            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::addLayers);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerLayerDefinitions);

            // Part picker (authoring tool).
            FMLJavaModLoadingContext.get().getModEventBus().addListener(PickerKeys::register);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(PickerHud::register);
            MinecraftForge.EVENT_BUS.register(new PickerInput());

            // Client commands (/sg spawnall, /sg set …) for the authoring workflow.
            MinecraftForge.EVENT_BUS.register(new GooglyClientCommands());
        });

        ServerConfig.register();
        MinecraftForge.EVENT_BUS.register(new ServerEventHandler());

        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "1", (a, b) -> true));
    }

    @SubscribeEvent
    public void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GOOGLY_EYE_LAYER, ModelGooglyEye::createBodyLayer);
    }

    private void addLayers(EntityRenderersEvent.AddLayers event) {
        if (clientEventHandler != null) {
            clientEventHandler.addLayers();
        }
    }
}
