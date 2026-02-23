package com.ruling_0.luxaetheria.api.aether;

import com.ruling_0.luxaetheria.api.utils.InterDimCoords;

/**
 * An interface for entities that receive and manipulate Aether provided by an {@link IAetherRelay}
 */
public interface IAetherManipulator {

    void bindRelay(InterDimCoords relayCoords);

    void unbindRelay(IAetherRelay relay);

    boolean isActive(IAetherRelay relay);

    boolean manipulate(AethericEnergyUnit aetherIn);
}
