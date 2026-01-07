package com.ruling_0.luxaetheria.api.aether;

import net.minecraft.nbt.NBTTagCompound;

import com.ruling_0.luxaetheria.api.aether.handlers.IReleaserHandler;

/**
 * An Interface for things which release Aether into the environment. Must possess a {@link IReleaserHandler}.
 */
public interface IAetherReleaser {

    /**
     * Returns the {@link IReleaserHandler} for this releaser.
     */
    IReleaserHandler getReleaserHandler();

    void writeWAILAData(NBTTagCompound compound);
}
