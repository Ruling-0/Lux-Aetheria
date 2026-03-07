package com.ruling_0.luxaetheria;

import com.ruling_0.luxaetheria.common.aether.AetherEventHandler;
import com.ruling_0.luxaetheria.common.aether.AetherManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;

public class LAProxy {

    public final AetherManager aetherManager = new AetherManager();

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        LAConfig.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        LuxAetheria.LOG.info(LAConfig.greeting);
        LuxAetheria.LOG.info("I am Lux Aetheria at version " + Tags.VERSION);

        LABlocks.init();
        LAItems.init();
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {}

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(new AetherEventHandler());
    }

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {}

    public void serverStopped(FMLServerStoppedEvent event) {
        this.aetherManager.reset();
    }
}
