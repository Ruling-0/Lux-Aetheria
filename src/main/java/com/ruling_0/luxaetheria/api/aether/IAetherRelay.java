package com.ruling_0.luxaetheria.api.aether;

import javax.annotation.Nonnull;

import com.ruling_0.luxaetheria.api.aether.handlers.IRelayHandler;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;

/**
 * An Interface for things which move Aether via source/sink flows. Must possess an {@link IRelayHandler}.
 */
public interface IAetherRelay {

    /**
     * Returns the {@link IRelayHandler} for this manipulator.
     */
    IRelayHandler getAetherHandler();

    /**
     * For enabling (making a valid source/sink) an {@link IAetherRelay}.
     * Should be called whenever the manipulator is added to the world.
     */
    void enable();

    /**
     * For disabling (making an invalid source/sink) an {@link IAetherRelay}.
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
