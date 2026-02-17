package com.ruling_0.luxaetheria.api.aether.connections;

import net.minecraft.util.Vec3;

import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * An immutable view of a {@link SinkConnection}.
 */
public interface ImmutableSinkConnection {

    @SuppressWarnings("unused")
    InterDimCoords getSinkCoords();

    @SuppressWarnings("unused")
    IAetherManipulator getSink();

    @SuppressWarnings("unused")
    AethericEnergyUnit getAeu();

    @SuppressWarnings("unused")
    Vector3fc getColCoords();

    @SuppressWarnings("unused")
    double getDist();
}
