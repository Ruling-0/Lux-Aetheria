package com.ruling_0.luxaetheria;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ruling_0.luxaetheria.common.tileentities.TileEntityAethericFurnace;
import com.ruling_0.luxaetheria.common.tileentities.TileEntityCollectorPylon;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(modid = LuxAetheria.MODID, version = Tags.VERSION, name = "Lux Aetheria", acceptedMinecraftVersions = "[1.7.10]")
public class LuxAetheria {

    public static final String MODID = "luxaetheria";
    public static final Logger LOG = LogManager.getLogger(MODID);

    public static CreativeTabs tabLuxAetheria = new CreativeTabs(MODID) {

        @Override
        public Item getTabIconItem() {
            return Items.quartz;
        }
    };

    @SidedProxy(clientSide = "com.ruling_0.luxaetheria.LAClientProxy", serverSide = "com.ruling_0.luxaetheria.LAProxy")
    public static LAProxy proxy;

    @Mod.EventHandler
    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        proxy.init(event);

        GameRegistry.registerTileEntity(TileEntityAethericFurnace.class, "LATileEntityAethericFurnace");
        GameRegistry.registerTileEntity(TileEntityCollectorPylon.class, "LATileEntityCollectorPylon");
    }

    @Mod.EventHandler
    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        proxy.serverStopped(event);
    }
}
