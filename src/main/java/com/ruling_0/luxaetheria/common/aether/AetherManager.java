package com.ruling_0.luxaetheria.common.aether;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;

import com.gtnewhorizon.gtnhlib.datastructs.space.ArrayProximityMap4D;
import com.gtnewhorizon.gtnhlib.datastructs.space.VolumeShape;
import com.ruling_0.luxaetheria.api.aether.AetherConstants;
import com.ruling_0.luxaetheria.api.aether.IAetherCollector;
import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.api.aether.IAetherReleaser;
import com.ruling_0.luxaetheria.api.aether.connections.ImmutableSinkConnection;
import com.ruling_0.luxaetheria.api.aether.connections.SinkConnection;
import com.ruling_0.luxaetheria.api.aether.handlers.IAetherHandler;
import com.ruling_0.luxaetheria.api.aether.handlers.ICollectorHandler;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;

import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * The class responsible for managing Aether flow. Only one should exist at a time.
 */
public class AetherManager {

    private static ArrayProximityMap4D<IAetherCollector> aetherCollectors;
    private static ArrayProximityMap4D<IAetherReleaser> aetherReleasers;
    private static HashSet<IAetherManipulator> aetherRootCollectors;
    private static Deque<IAetherManipulator> aetherSearchQueue;
    private static HashSet<IAetherManipulator> aetherUpdateQueue;
    private static Deque<IAetherManipulator> orphanedManipulators;
    private long serverTick = 0L;

    public AetherManager() {
        aetherCollectors = new ArrayProximityMap4D<>(VolumeShape.SPHERE);
        aetherReleasers = new ArrayProximityMap4D<>(VolumeShape.SPHERE);
        aetherRootCollectors = new HashSet<>();
        aetherSearchQueue = new ArrayDeque<>();
        aetherUpdateQueue = new HashSet<>();
        orphanedManipulators = new ArrayDeque<>();
    }

    public void enableCollector(@Nonnull IAetherCollector collector, int dim, int x, int y, int z) {
        ICollectorHandler handler = collector.getCollectorHandler();
        // Effects for any collector that has this new one in range
        aetherCollectors.forEachInRange(dim, x, y, z,
            c -> c.getCollectorHandler().addCollectorInRange(collector));
        // Effects for this collector from any in its range
        aetherCollectors.forEachInRange(dim, x, y, z, handler.getCollectorRange(), handler::addCollectorInRange);

        aetherCollectors.put(collector, dim, x, y, z, handler.getCollectorRange());
        aetherReleasers.forEachInRange(dim, x, y, z, handler.getCollectorRange(), handler::addReleaserInRange);
        if (collector instanceof IAetherManipulator manipulator) aetherRootCollectors.add(manipulator);
    }

    public void disableCollector(@Nonnull IAetherCollector collector, int dim, int x, int y, int z) {
        aetherCollectors.remove(dim, x, y, z);
        aetherCollectors.forEachInRange(dim, x, y, z,
            c -> c.getCollectorHandler().removeCollectorInRange(collector));
        if (collector instanceof IAetherManipulator manipulator) aetherRootCollectors.remove(manipulator);
    }

    public void enableReleaser(@Nonnull IAetherReleaser releaser, int dim, int x, int y, int z) {
        aetherReleasers.put(releaser, dim, x, y, z, AetherConstants.MAX_COLLECTOR_RANGE);
        aetherCollectors.forEachInRange(dim, x, y, z,
            c -> c.getCollectorHandler().addReleaserInRange(releaser));
    }

    public void disableReleaser(@Nonnull IAetherReleaser releaser, int dim, int x, int y, int z) {
        aetherReleasers.remove(dim, x, y, z);
        aetherCollectors.forEachInRange(dim, x, y, z,
            c -> c.getCollectorHandler().removeReleaserInRange(releaser));
    }

    public void addOrphanedManipulator(IAetherManipulator manipulator) {
        orphanedManipulators.add(manipulator);
    }

    public void bulkOrphanSinks(IAetherManipulator manipulator) {
        Iterator<ImmutableSinkConnection> iterSinks = manipulator.getAetherHandler().getAetherSinksIter();
        while (iterSinks.hasNext()) {
            orphanedManipulators.add(iterSinks.next().getSink());
        }
    }

    public void onServerTick(TickEvent.ServerTickEvent event) {
        aetherSearchQueue.clear();

        /*
         * Force-reset orphans, in case they are not updated in the final loop.
         * Ideally, this is more performant than enqueuing into the final
         * loop and having extra checks.
         */
        aetherSearchQueue.addAll(orphanedManipulators);
        while (!aetherSearchQueue.isEmpty()) {
            IAetherManipulator curr = aetherSearchQueue.pop();
            IAetherHandler handler = curr.getAetherHandler();
            handler.resetAether();
            Iterator<ImmutableSinkConnection> iterSinks = handler.getAetherSinksIter();
            while (iterSinks.hasNext()) {
                IAetherManipulator next = iterSinks.next().getSink();
                if (next != null) aetherSearchQueue.push(next);
            }
            InterDimCoords coords = curr.getInterDimCoords();
            coords.getWorld().markBlockForUpdate(coords.getX(), coords.getY(), coords.getZ());
            orphanedManipulators.remove(curr);
        }

        /*
         * Starting with known collectors, calculate Aether propagation using BFS.
         * Loops are handled in manipulators' getAetherFromSource
         */
        aetherSearchQueue.addAll(aetherRootCollectors);
        this.serverTick++;
        while (!aetherSearchQueue.isEmpty()) {
            IAetherManipulator curr = aetherSearchQueue.pop();
            IAetherHandler currHandler = curr.getAetherHandler();
            Iterator<SinkConnection> mutIterSinks = currHandler.getMutableSinksIter();
            while (mutIterSinks.hasNext()) {
                SinkConnection sinkConn = mutIterSinks.next();
                if (sinkConn.sink == null) {
                    InterDimCoords coords = sinkConn.sinkCoords;
                    TileEntity te = coords.getWorld().getTileEntity(coords.getX(), coords.getY(), coords.getZ());
                    if (te instanceof IAetherManipulator sink) {
                        sinkConn.sink = sink;
                    }
                    else {
                        mutIterSinks.remove();
                    }
                }
            }
            Iterator<ImmutableSinkConnection> iterSinks = currHandler.getAetherSinksIter();
            while (iterSinks.hasNext()) {
                IAetherManipulator next = iterSinks.next().getSink();
                boolean shouldPropagate = next.getAetherHandler().getAetherFromSource(curr, this.serverTick);
                if (shouldPropagate) aetherSearchQueue.push(next);
            }
            if (currHandler.isUpdatable()) aetherUpdateQueue.add(curr);
        }
        for (IAetherManipulator curr : aetherUpdateQueue) {
            curr.getAetherHandler().updateAether();
        }
        aetherUpdateQueue.clear();
    }

    public void reset() {
        aetherCollectors = new ArrayProximityMap4D<>(VolumeShape.SPHERE);
        aetherReleasers = new ArrayProximityMap4D<>(VolumeShape.SPHERE);
        aetherRootCollectors = new HashSet<>();
        aetherSearchQueue = new ArrayDeque<>();
        aetherUpdateQueue = new HashSet<>();
    }
}
