package com.ruling_0.luxaetheria.api.aether;

import com.ruling_0.luxaetheria.api.aether.handlers.IAetherHandler;
import net.minecraft.nbt.NBTTagCompound;

/**
 * An Interface for things which intake and/or output Aether.
 */
public interface IAetherManipulator {
    IAetherHandler getAetherHandler();

    void enable();

    /**
     * For disabling (making an invalid source/sink) an {@link IAetherManipulator}.
     * Should be called whenever the manipulator is destroyed or unloaded.
     * Removes the manipulator from source/sink lists of upstream/downstream manipulators.
     */
    void disable();

    void writeWAILAData(NBTTagCompound compound);
}
