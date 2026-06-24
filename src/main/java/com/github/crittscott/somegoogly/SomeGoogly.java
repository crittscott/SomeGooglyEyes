package com.github.crittscott.somegoogly;

import com.github.crittscott.somegoogly.command.GooglyClientCommands;
import com.github.crittscott.somegoogly.command.MaybeFloatArgumentType;
import com.github.crittscott.somegoogly.config.ClientConfig;
import com.github.crittscott.somegoogly.config.ServerConfig;
import com.github.crittscott.somegoogly.event.ClientEventHandler;
import com.github.crittscott.somegoogly.event.EyeItemInteractions;
import com.github.crittscott.somegoogly.event.EyePotionInteractions;
import com.github.crittscott.somegoogly.event.EyeReactionHandler;
import com.github.crittscott.somegoogly.event.ServerEventHandler;
import com.github.crittscott.somegoogly.enchant.ModEnchantments;
import com.github.crittscott.somegoogly.item.ModItems;
import com.github.crittscott.somegoogly.network.NetworkHandler;
import com.github.crittscott.somegoogly.potion.ModPotions;
import com.github.crittscott.somegoogly.recipe.ModRecipes;
import com.github.crittscott.somegoogly.client.picker.PickerHud;
import com.github.crittscott.somegoogly.client.picker.PickerInput;
import com.github.crittscott.somegoogly.client.picker.PickerKeys;
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
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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
        ModItems.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModEnchantments.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModRecipes.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModPotions.register(FMLJavaModLoadingContext.get().getModEventBus());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientConfig.register();

            MinecraftForge.EVENT_BUS.register(clientEventHandler = new ClientEventHandler());

            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::addLayers);

            // Part picker (authoring tool).
            FMLJavaModLoadingContext.get().getModEventBus().addListener(PickerKeys::register);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(PickerHud::register);
            MinecraftForge.EVENT_BUS.register(new PickerInput());

            // Client commands (/sg spawnall, /sg set …) for the authoring workflow.
            MinecraftForge.EVENT_BUS.register(new GooglyClientCommands());
        });

        ServerConfig.register();
        MinecraftForge.EVENT_BUS.register(new ServerEventHandler());
        MinecraftForge.EVENT_BUS.register(new EyeItemInteractions());
        MinecraftForge.EVENT_BUS.register(new EyePotionInteractions());
        MinecraftForge.EVENT_BUS.register(new EyeReactionHandler());

        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "1", (a, b) -> true));
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Brewing must be registered on the main thread (the registry isn't thread-safe).
        event.enqueueWork(ModPotions::registerBrewing);
    }

    private void addLayers(EntityRenderersEvent.AddLayers event) {
        if (clientEventHandler != null) {
            clientEventHandler.addLayers();
        }
    }
}
