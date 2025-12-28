package com.ruling_0.luxaetheria.common.aether;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import org.jetbrains.annotations.NotNull;

import static com.ruling_0.luxaetheria.LAProxy.aetherManager;

public class AetherEventHandler {

    @SubscribeEvent
    public void onServerTick(TickEvent.@NotNull ServerTickEvent event) {
        if (!event.side.isServer() || event.phase == TickEvent.Phase.END) return;
        aetherManager.onServerTick(event);
    }
}
