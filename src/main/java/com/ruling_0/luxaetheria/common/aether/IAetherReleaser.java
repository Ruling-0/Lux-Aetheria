package com.ruling_0.luxaetheria.common.aether;

import net.minecraft.nbt.NBTTagCompound;

/**
 * An Interface for things which release Aether into the environment.
 */
public interface IAetherReleaser {

    AethericEnergyUnit getAetherRelease();

    boolean isRemote();

    void writeWAILAData(NBTTagCompound compound);
}
