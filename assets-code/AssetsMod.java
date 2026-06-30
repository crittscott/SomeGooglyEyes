package com.github.crittscott.assets;

import com.mojang.logging.LogUtils;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod("assets")
public class AssetsMod {

    public static final String MOD_ID = "assets";
    private static final Logger LOGGER = LogUtils.getLogger();

    public AssetsMod() {
        // Register for client setup event
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Assets mod initialized");
    }

    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        AssetsCommand.register(event.getDispatcher());
        LOGGER.info("Assets command registered");
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}