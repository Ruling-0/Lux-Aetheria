package com.ruling_0.luxaetheria;

import com.ruling_0.luxaetheria.client.renderer.AetherBeamRenderer;
import com.ruling_0.luxaetheria.common.tileentities.TileEntityAetherRelay;
import com.ruling_0.luxaetheria.common.tileentities.TileEntityAethericFurnace;
import com.ruling_0.luxaetheria.common.tileentities.TileEntityCollectorPylon;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.event.FMLInitializationEvent;

public class LAClientProxy extends LAProxy {

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCollectorPylon.class, new AetherBeamRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityAetherRelay.class, new AetherBeamRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityAethericFurnace.class, new AetherBeamRenderer());
    }

}
