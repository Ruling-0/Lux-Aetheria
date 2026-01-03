package com.ruling_0.luxaetheria.common.aether;

import com.ruling_0.luxaetheria.LAProxy;
import com.ruling_0.luxaetheria.LuxAetheria;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import org.jetbrains.annotations.NotNull;


public class AetherEventHandler {

    @SubscribeEvent
    public void onServerTick(TickEvent.@NotNull ServerTickEvent event) {
        if (!event.side.isServer() || event.phase == TickEvent.Phase.END) return;
        LuxAetheria.proxy.aetherManager.onServerTick(event);
    }
}
