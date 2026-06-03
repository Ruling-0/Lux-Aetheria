package com.ruling_0.luxaetheria.common.aether;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.annotation.Nonnull;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.datastructs.space.ArrayProximityMap4D;
import com.gtnewhorizon.gtnhlib.datastructs.space.VolumeShape;
import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.api.aether.AetherConstants;
import com.ruling_0.luxaetheria.api.aether.IAetherCollector;
import com.ruling_0.luxaetheria.api.aether.IAetherRelay;
import com.ruling_0.luxaetheria.api.aether.IAetherReleaser;
import com.ruling_0.luxaetheria.api.aether.connections.ImmutableSinkConnection;
import com.ruling_0.luxaetheria.api.aether.connections.SinkConnection;
import com.ruling_0.luxaetheria.api.aether.handlers.ICollectorHandler;
import com.ruling_0.luxaetheria.api.aether.handlers.IRelayHandler;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;

import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * The class responsible for managing Aether flow. Only one should exist at a time.
 */
public class AetherManager {

    private static ArrayProximityMap4D<IAetherCollector> aetherCollectors;
    private static ArrayProximityMap4D<IAetherReleaser> aetherReleasers;
    private static HashSet<IAetherRelay> aetherRootCollectors;
    private static Deque<IAetherRelay> aetherSearchQueue;
    private static HashSet<IAetherRelay> aetherUpdateQueue;
    private static Deque<IAetherRelay> orphanedManipulators;
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
        if (collector instanceof IAetherRelay manipulator) aetherRootCollectors.add(manipulator);
    }

    public void disableCollector(@Nonnull IAetherCollector collector, int dim, int x, int y, int z) {
        aetherCollectors.remove(dim, x, y, z);
        aetherCollectors.forEachInRange(dim, x, y, z,
            c -> c.getCollectorHandler().removeCollectorInRange(collector));
        if (collector instanceof IAetherRelay manipulator) aetherRootCollectors.remove(manipulator);
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

    public void addOrphanedManipulator(IAetherRelay manipulator) {
        orphanedManipulators.add(manipulator);
    }

    public void bulkOrphanSinks(IAetherRelay manipulator) {
        Iterator<SinkConnection> mutIterSinks = manipulator.getAetherHandler().getMutableSinksIter();
        while (mutIterSinks.hasNext()) {
            SinkConnection sinkConn = mutIterSinks.next();
            if (handleInvalidSink(sinkConn)) {
                mutIterSinks.remove();
                continue;
            }
            orphanedManipulators.add(sinkConn.getSink());
        }
    }

    /// Sinks in `SinkConnection`s are null on world load (stored via coords).
    /// This sets the reference for the actual sink object using the coords.
    /// Returns true if the sink is now invalid, false otherwise.
    /// Inverted return since generally an action is conditioned on an invalid sink.
    private static boolean handleInvalidSink(SinkConnection sinkConn) {
        if (sinkConn.sink == null) {
            InterDimCoords coords = sinkConn.sinkCoords;
            World world = coords.getWorld();
            // An unloaded chunk reports no tile entity; keep the connection and retry once it loads.
            if (!world.blockExists(coords.getX(), coords.getY(), coords.getZ())) return false;
            TileEntity te = world.getTileEntity(coords.getX(), coords.getY(), coords.getZ());
            if (te instanceof IAetherRelay sink) {
                sinkConn.sink = sink;
            }
            else {
                LuxAetheria.LOG.warn("Dropping Aether sink connection: no relay at ({}, {}, {}) in dim {}",
                    coords.getX(), coords.getY(), coords.getZ(), coords.getDimID());
                return true;
            }
        }
        return false;
    }

    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        aetherSearchQueue.clear();

        /*
         * Force-reset orphans, in case they are not updated in the final loop.
         * Ideally, this is more performant than enqueuing into the final
         * loop and having extra checks.
         * Does not call handleInvalidSink since that is handled in bulkOrphanSinks
         */
        aetherSearchQueue.addAll(orphanedManipulators);
        while (!aetherSearchQueue.isEmpty()) {
            IAetherRelay curr = aetherSearchQueue.pop();
            IRelayHandler handler = curr.getAetherHandler();
            handler.resetAether();
            Iterator<ImmutableSinkConnection> iterSinks = handler.getAetherSinksIter();
            while (iterSinks.hasNext()) {
                aetherSearchQueue.push(iterSinks.next().getSink());
            }
            InterDimCoords coords = curr.getInterDimCoords();
            coords.getWorld().markBlockForUpdate(coords.getX(), coords.getY(), coords.getZ());
            orphanedManipulators.remove(curr);
        }

        /*
         * Pre-pass: resolve/validate every sink once (so the edge set is stable for the rest of the tick),
         * discover the subgraph reachable from the root collectors, and count each node's in-degree
         * (how many reachable sources point at it).
         */
        Object2IntOpenHashMap<IAetherRelay> pendingSources = new Object2IntOpenHashMap<>();
        ObjectOpenHashSet<IAetherRelay> reachable = new ObjectOpenHashSet<>(aetherRootCollectors);
        aetherSearchQueue.addAll(aetherRootCollectors);
        while (!aetherSearchQueue.isEmpty()) {
            IAetherRelay curr = aetherSearchQueue.pop();
            Iterator<SinkConnection> mutIterSinks = curr.getAetherHandler().getMutableSinksIter();
            while (mutIterSinks.hasNext()) {
                SinkConnection sinkConn = mutIterSinks.next();
                if (handleInvalidSink(sinkConn)) {
                    mutIterSinks.remove();
                    continue;
                }
                IAetherRelay sink = sinkConn.getSink();
                if (sink == null) continue; // sink chunk not loaded yet; retry next tick
                pendingSources.merge(sink, 1, Integer::sum);
                if (reachable.add(sink)) aetherSearchQueue.push(sink);
            }
        }

        /*
         * Topological propagation: process each node exactly once, only after every one of its sources has
         * delivered. This guarantees a node accumulates its full input before it emits to each sink a single time.
         */
        this.serverTick++;
        ObjectOpenHashSet<IAetherRelay> processed = new ObjectOpenHashSet<>();
        aetherSearchQueue.addAll(aetherRootCollectors);
        while (!aetherSearchQueue.isEmpty()) {
            IAetherRelay curr = aetherSearchQueue.pop();
            if (!processed.add(curr)) continue;
            IRelayHandler currHandler = curr.getAetherHandler();
            currHandler.finalizeTick(this.serverTick);
            Iterator<ImmutableSinkConnection> iterSinks = currHandler.getAetherSinksIter();
            while (iterSinks.hasNext()) {
                IAetherRelay next = iterSinks.next().getSink();
                if (next == null) continue;
                next.getAetherHandler().getAetherFromSource(curr, this.serverTick);
                Integer left = pendingSources.computeIfPresent(next, (k, v) -> v - 1);
                if (left != null && left == 0) aetherSearchQueue.push(next);
            }
            if (currHandler.isUpdatable()) aetherUpdateQueue.add(curr);
        }

        /*
         * Cycle fallback: any reachable node never processed above is in (or downstream of) a cycle, so its
         * in-degree never reached zero. Process them now, they shall handle cycles in getAetherFromSource.
         */
        for (IAetherRelay node : reachable) {
            if (!processed.contains(node)) aetherSearchQueue.add(node);
        }
        while (!aetherSearchQueue.isEmpty()) {
            IAetherRelay curr = aetherSearchQueue.pop();
            IRelayHandler currHandler = curr.getAetherHandler();
            currHandler.finalizeTick(this.serverTick);
            Iterator<ImmutableSinkConnection> iterSinks = currHandler.getAetherSinksIter();
            while (iterSinks.hasNext()) {
                IAetherRelay next = iterSinks.next().getSink();
                if (next == null) continue;
                if (next.getAetherHandler().getAetherFromSource(curr, this.serverTick)) aetherSearchQueue.push(next);
            }
            if (currHandler.isUpdatable()) aetherUpdateQueue.add(curr);
        }

        for (IAetherRelay curr : aetherUpdateQueue) {
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
