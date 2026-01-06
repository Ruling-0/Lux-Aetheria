package com.ruling_0.luxaetheria.api.aether;

import net.minecraft.nbt.NBTTagCompound;

import com.ruling_0.luxaetheria.api.aether.handlers.IReleaserHandler;

/**
 * An Interface for things which release Aether into the environment.
 */
public interface IAetherReleaser {

    IReleaserHandler getReleaserHandler();

    boolean isRemote();

    void writeWAILAData(NBTTagCompound compound);
}
