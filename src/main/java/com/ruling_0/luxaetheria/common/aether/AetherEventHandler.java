package com.ruling_0.luxaetheria.common.aether;

import javax.annotation.Nonnull;

import com.ruling_0.luxaetheria.LuxAetheria;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class AetherEventHandler {

    @SubscribeEvent
    public void onServerTick(@Nonnull TickEvent.ServerTickEvent event) {
        if (!event.side.isServer() || event.phase == TickEvent.Phase.END) return;
        LuxAetheria.proxy.aetherManager.onServerTick(event);
    }
}
