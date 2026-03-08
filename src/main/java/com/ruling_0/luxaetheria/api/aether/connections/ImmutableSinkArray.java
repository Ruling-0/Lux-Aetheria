package com.ruling_0.luxaetheria.api.aether.connections;

import java.util.Iterator;

import javax.annotation.Nullable;

import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherRelay;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;

import org.joml.Vector3fc;

/**
 * An immutable view of an {@link AetherSinkArray}.
 */
public interface ImmutableSinkArray {

    int getOutputIndex(InterDimCoords sinkCoords);

    boolean hasOutput(int index);

    boolean hasSink(InterDimCoords sinkCoords);

    @Nullable
    ImmutableSinkConnection get(int index);

    boolean isEmpty();

    int size();

    /**
     * Returns a count of how many connections exist (are not null).
     */
    int numConnections();

    /**
     * Provides an iterator consisting of {@link ImmutableSinkConnection}.
     * This iterator is also immutable, and so does not support {@link Iterator#remove()}.
     */
    Iterator<ImmutableSinkConnection> immutableIter();

    @SuppressWarnings("unused")
    IAetherRelay getSink(InterDimCoords sinkCoords);

    @SuppressWarnings("unused")
    AethericEnergyUnit getAeu(InterDimCoords sinkCoords);

    Vector3fc getColCoords(InterDimCoords sinkCoords);
}
