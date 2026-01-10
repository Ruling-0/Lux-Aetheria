package com.ruling_0.luxaetheria.api.aether;

import com.ruling_0.luxaetheria.api.utils.InterDimCoords;
import net.minecraft.nbt.NBTTagCompound;

import com.ruling_0.luxaetheria.api.aether.handlers.IAetherHandler;

import javax.annotation.Nonnull;

/**
 * An Interface for things which intake and/or output Aether. Must possess an {@link IAetherHandler}.
 */
public interface IAetherManipulator {

    /**
     * Returns the {@link IAetherHandler} for this manipulator.
     */
    IAetherHandler getAetherHandler();

    /**
     * For enabling (making a valid source/sink) an {@link IAetherManipulator}.
     * Should be called whenever the manipulator is added to the world.
     */
    void enable();

    /**
     * For disabling (making an invalid source/sink) an {@link IAetherManipulator}.
     * Should be called whenever the manipulator is destroyed or unloaded.
     * Must remove the manipulator from source/sink lists of upstream/downstream manipulators.
     */
    void disable();

    /**
     * Gets the {@link InterDimCoords} of this.
     */
    @Nonnull
    InterDimCoords getInterDimCoords();
}
