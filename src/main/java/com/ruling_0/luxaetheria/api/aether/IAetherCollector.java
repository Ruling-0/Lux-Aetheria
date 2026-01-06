package com.ruling_0.luxaetheria.api.aether;

import net.minecraft.nbt.NBTTagCompound;

import com.ruling_0.luxaetheria.api.aether.handlers.ICollectorHandler;

/**
 * An Interface for things which collect ambient Aether.
 */
public interface IAetherCollector {

    ICollectorHandler getCollectorHandler();

    void enable();

    /**
     * For disabling an {@link IAetherCollector}.
     * Should be called whenever the collector is destroyed or unloaded.
     * Removes the collector's effects on nearby collectors.
     */
    void disable();

    boolean isRemote();

    void writeWAILAData(NBTTagCompound compound);
}
