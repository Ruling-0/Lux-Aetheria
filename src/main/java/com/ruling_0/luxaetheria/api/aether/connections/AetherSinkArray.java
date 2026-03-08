package com.ruling_0.luxaetheria.api.aether.connections;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherRelay;
import com.ruling_0.luxaetheria.api.aether.handlers.IRelayHandler;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

/**
 * Array-backed class for holding data about Aether sinks in an {@link IRelayHandler}.
 * Sink data is stored as a {@link SinkConnection}.
 * Each array index refers to a specific output of the owning {@link IAetherRelay}.
 * Allows for retrieving data by a sink's {@link InterDimCoords}.
 */
public final class AetherSinkArray implements ImmutableSinkArray {

    private double maxSinkDistance = 0.0D;
    private final @Nullable SinkConnection[] sinkConnections;
    private final IRelayHandler owner;

    public AetherSinkArray(int max, IRelayHandler owner) {
        this.sinkConnections = new SinkConnection[max];
        this.owner = owner;
    }

    @Override
    public int getOutputIndex(InterDimCoords sinkCoords) {
        for (int i = 0; i < this.sinkConnections.length; ++i) {
            SinkConnection sinkConn = this.sinkConnections[i];
            if (sinkConn != null && sinkConn.sinkCoords.equals(sinkCoords)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean hasOutput(int index) {
        return this.sinkConnections[index] != null;
    }

    @Override
    public boolean hasSink(InterDimCoords sinkCoords) {
        for (SinkConnection sinkConn : this.sinkConnections) {
            if (sinkConn != null && sinkConn.sinkCoords.equals(sinkCoords)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public SinkConnection get(int index) {
        return this.sinkConnections[index];
    }

    public boolean add(int index, InterDimCoords sinkCoords, @Nullable IAetherRelay sink, Vector3fc colCoords,
                       double dist) {
        if (this.sinkConnections[index] != null) return false;
        this.sinkConnections[index] = new SinkConnection(sinkCoords, sink, new AethericEnergyUnit(), colCoords, dist);
        return true;
    }

    public boolean add(IAetherRelay sink) {
        return this.add(sink, new AethericEnergyUnit(), sink.getInterDimCoords().getVec3fc(),
            this.owner.getInterDimCoords().distance(sink.getInterDimCoords()));
    }

    public boolean add(IAetherRelay sink, AethericEnergyUnit aeu, Vector3fc colCoords, double dist) {
        return this.add(new SinkConnection(sink.getInterDimCoords(), sink, aeu, colCoords, dist));
    }

    public boolean add(SinkConnection connection) {
        for (int i = 0; i < this.sinkConnections.length; ++i) {
            if (this.sinkConnections[i] == null) {
                if (connection.dist > this.maxSinkDistance) this.maxSinkDistance = connection.dist;
                this.sinkConnections[i] = connection;
                return true;
            }
        }
        return false;
    }

    public void removeNoRet(int index) {
        this.sinkConnections[index] = null;
    }

    public SinkConnection remove(InterDimCoords sinkCoords) {
        this.maxSinkDistance = 0.0D;
        SinkConnection ret = null;
        for (int i = 0; i < this.sinkConnections.length; ++i) {
            SinkConnection sinkConn = this.sinkConnections[i];
            if (sinkConn == null) continue;
            if (sinkConn.sinkCoords.equals(sinkCoords)) {
                ret = sinkConn;
                this.sinkConnections[i] = null;
                continue;
            }
            if (sinkConn.dist > this.maxSinkDistance) this.maxSinkDistance = sinkConn.dist;
        }
        return ret;
    }

    @Override
    public boolean isEmpty() {
        for (SinkConnection sinkConn : this.sinkConnections) {
            if (sinkConn != null) return false;
        }
        return true;
    }

    @Override
    public int size() {
        return this.sinkConnections.length;
    }

    @Override
    public int numConnections() {
        int ret = 0;
        for (SinkConnection sinkConn : this.sinkConnections) {
            if (sinkConn != null && sinkConn.sink != null) ret += 1;
        }
        return ret;
    }

    public void clear() {
        Arrays.fill(this.sinkConnections, null);
    }

    @Override
    public Iterator<ImmutableSinkConnection> immutableIter() {
        return new ImmutableSinkIter();
    }

    public Iterator<SinkConnection> iterator() {
        return new SinkIter();
    }

    @Nullable
    @Override
    public IAetherRelay getSink(InterDimCoords sinkCoords) {
        for (SinkConnection sinkConn : this.sinkConnections) {
            if (sinkConn != null && sinkConn.sinkCoords.equals(sinkCoords)) {
                return sinkConn.sink;
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    public boolean setSink(InterDimCoords sinkCoords, IAetherRelay sink) {
        for (SinkConnection sinkConn : this.sinkConnections) {
            if (sinkConn != null && sinkConn.sinkCoords.equals(sinkCoords)) {
                sinkConn.sink = sink;
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public AethericEnergyUnit getAeu(InterDimCoords sinkCoords) {
        for (SinkConnection sinkConn : this.sinkConnections) {
            if (sinkConn != null && sinkConn.sinkCoords.equals(sinkCoords)) {
                return sinkConn.aeu;
            }
        }
        return null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean setAeu(InterDimCoords sinkCoords, AethericEnergyUnit aeu) {
        for (SinkConnection sinkConn : this.sinkConnections) {
            if (sinkConn != null && sinkConn.sinkCoords.equals(sinkCoords)) {
                sinkConn.aeu = aeu;
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public Vector3fc getColCoords(InterDimCoords sinkCoords) {
        for (SinkConnection sinkConn : this.sinkConnections) {
            if (sinkConn != null && sinkConn.sinkCoords.equals(sinkCoords)) {
                return sinkConn.colCoords;
            }
        }
        return null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean setColCoords(InterDimCoords sinkCoords, Vector3fc colCoords) {
        for (SinkConnection sinkConn : this.sinkConnections) {
            if (sinkConn != null && sinkConn.sinkCoords.equals(sinkCoords)) {
                sinkConn.colCoords = colCoords;
                return true;
            }
        }
        return false;
    }

    public double getMaxSinkDistance() { return this.maxSinkDistance; }

    private final class ImmutableSinkIter implements Iterator<ImmutableSinkConnection> {

        private int index = 0;
        private int nextKnown = 0;

        public ImmutableSinkIter() {}

        @Override
        public boolean hasNext() {
            for (int i = index; i < AetherSinkArray.this.size(); ++i) {
                if (AetherSinkArray.this.get(i) != null) {
                    this.nextKnown = i;
                    return true;
                }
            }
            return false;
        }

        @Override
        public ImmutableSinkConnection next() {
            if (!(this.nextKnown > this.index) && !this.hasNext()) {
                throw new NoSuchElementException();
            }
            this.index = this.nextKnown + 1;
            return AetherSinkArray.this.get(this.nextKnown);
        }
    }

    private final class SinkIter implements Iterator<SinkConnection> {

        private int index = -1;
        private int nextKnown = 0;

        public SinkIter() {}

        @Override
        public boolean hasNext() {
            for (int i = index + 1; i < AetherSinkArray.this.size(); ++i) {
                if (AetherSinkArray.this.get(i) != null) {
                    this.nextKnown = i;
                    return true;
                }
            }
            return false;
        }

        @Override
        public SinkConnection next() {
            if (!(this.nextKnown > this.index) && !this.hasNext()) {
                throw new NoSuchElementException();
            }
            this.index = this.nextKnown;
            return AetherSinkArray.this.get(this.index);
        }

        @Override
        public void remove() {
            AetherSinkArray.this.removeNoRet(this.index);
        }
    }
}
