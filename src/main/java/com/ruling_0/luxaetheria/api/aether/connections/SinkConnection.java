package com.ruling_0.luxaetheria.api.aether.connections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.util.Vec3;

import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Holds data about a connection from an {@link IAetherManipulator} to another.
 */
public class SinkConnection implements ImmutableSinkConnection {

    /**
     * Coords of the sink, used for indexing an {@link AetherSinkArray}.
     * These are final, new coords should be a new {@link SinkConnection}.
     */
    public final InterDimCoords sinkCoords;
    public IAetherManipulator sink;
    /**
     * The Aether being sent along this connection
     */
    public AethericEnergyUnit aeu;
    /**
     * The first point of collision along the connection.
     * If there are no collisions, this equals sinkCoords.{@link InterDimCoords#getVec3()}
     */
    public Vector3fc colCoords;
    public final double dist;

    public SinkConnection(InterDimCoords sinkCoords, @Nullable IAetherManipulator sink, @Nonnull AethericEnergyUnit aeu,
                          Vector3fc colCoords, double dist) {
        this.sinkCoords = sinkCoords;
        this.sink = sink;
        this.aeu = aeu;
        this.colCoords = colCoords;
        this.dist = dist;
    }

    @Override
    public InterDimCoords getSinkCoords() { return sinkCoords; }

    @Override
    public IAetherManipulator getSink() { return sink; }

    @Override
    public AethericEnergyUnit getAeu() { return aeu; }

    @Override
    public Vector3fc getColCoords() { return colCoords; }

    @Override
    public double getDist() { return dist; }
}
