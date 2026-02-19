package com.ruling_0.luxaetheria.api.aether;

/**
 * An interface for entities that receive and manipulate Aether provided by an {@link IAetherRelay}
 */
public interface IAetherManipulator {

    void bindRelay(IAetherRelay relay);

    void unbindRelay(IAetherRelay relay);
}
