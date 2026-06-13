package com.github.crittscott.somegoogly;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SomeGoogly.MOD_ID)
public class SomeGoogly {
    public static final String MOD_NAME = "Some Googly Eyes";
    public static final String MOD_ID = "somegoogly";

    public static final Logger LOGGER = LogManager.getLogger();

    public static final ModelLayerLocation GOOGLY_EYE_LAYER = new ModelLayerLocation(new ResourceLocation("somegoogly:googly_eye"), "main");

    public static ClientEventHandler clientEventHandler;

    public SomeGoogly() {
        // Eye configs are loaded from datapacks on the server (EyeConfigReloadListener) and synced
        // to clients (EyeConfigSyncPacket); nothing to load at construction time.
        NetworkHandler.register();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientConfig.register();

            MinecraftForge.EVENT_BUS.register(clientEventHandler = new ClientEventHandler());

            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::addLayers);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerLayerDefinitions);

            // Part picker (authoring tool).
            FMLJavaModLoadingContext.get().getModEventBus().addListener(PickerKeys::register);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(PickerHud::register);
            MinecraftForge.EVENT_BUS.register(new PickerInput());
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
