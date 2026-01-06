package com.ruling_0.luxaetheria.common.aether;

import com.ruling_0.luxaetheria.api.aether.handlers.IReleaserHandler;
import net.minecraft.nbt.NBTTagCompound;

/**
 * An Interface for things which release Aether into the environment.
 */
public interface IAetherReleaser {
    IReleaserHandler getReleaserHandler();

    boolean isRemote();

    void writeWAILAData(NBTTagCompound compound);
}
