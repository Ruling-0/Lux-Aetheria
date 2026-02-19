package com.ruling_0.luxaetheria.api.aether.connections;

import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherRelay;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;
import org.joml.Vector3fc;

/**
 * An immutable view of a {@link SinkConnection}.
 */
public interface ImmutableSinkConnection {

    @SuppressWarnings("unused")
    InterDimCoords getSinkCoords();

    @SuppressWarnings("unused")
    IAetherRelay getSink();

    @SuppressWarnings("unused")
    AethericEnergyUnit getAeu();

    @SuppressWarnings("unused")
    Vector3fc getColCoords();

    @SuppressWarnings("unused")
    double getDist();
}
