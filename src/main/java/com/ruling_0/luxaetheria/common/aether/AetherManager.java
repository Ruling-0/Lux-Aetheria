package com.ruling_0.luxaetheria.common.aether;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.gtnewhorizon.gtnhlib.datastructs.space.ArrayProximityMap4D;
import com.gtnewhorizon.gtnhlib.datastructs.space.VolumeShape;

import com.ruling_0.luxaetheria.api.aether.AetherConstants;
import com.ruling_0.luxaetheria.api.aether.IAetherCollector;
import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.api.aether.IAetherReleaser;
import com.ruling_0.luxaetheria.api.aether.handlers.IAetherHandler;
import com.ruling_0.luxaetheria.api.aether.handlers.ICollectorHandler;
import com.ruling_0.luxaetheria.utils.InterDimCoords;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nonnull;

public class AetherManager {

    private static ArrayProximityMap4D<IAetherCollector> AetherCollectors;
    private static ArrayProximityMap4D<IAetherReleaser> AetherReleasers;
    private static HashSet<IAetherManipulator> AetherRootCollectors;
    private static Deque<IAetherManipulator> AetherSearchQueue;
    private static HashSet<IAetherManipulator> AetherUpdateQueue;
    private static Deque<IAetherManipulator> orphanedManipulators;
    private long serverTick = 0L;

    public AetherManager() {
        AetherCollectors = new ArrayProximityMap4D<>(VolumeShape.SPHERE);
        AetherReleasers = new ArrayProximityMap4D<>(VolumeShape.SPHERE);
        AetherRootCollectors = new HashSet<>();
        AetherSearchQueue = new ArrayDeque<>();
        AetherUpdateQueue = new HashSet<>();
        orphanedManipulators = new ArrayDeque<>();
    }

    public void enableCollector(@Nonnull IAetherCollector collector, int dim, int x, int y, int z) {
        if (collector.isRemote()) return;
        // TODO: PR to GTNHLib that returns count from forEachInRange
        AtomicInteger count = new AtomicInteger();
        AtomicLong totalCollection = new AtomicLong();
        ICollectorHandler handler = collector.getCollectorHandler();
        AetherCollectors.forEachInRange(dim, x, y, z, c -> {
            count.getAndIncrement();
            c.getCollectorHandler().addCollectorInRange(collector);
            totalCollection.getAndAdd(c.getCollectorHandler().getAetherCollectionAmount());
        });
        handler.bulkUpdateCollectors(-totalCollection.get(), count.get());
        AetherCollectors.put(collector, dim, x, y, z, handler.getCollectorRange());
        AetherReleasers.forEachInRange(dim, x, y, z, handler::addReleaserInRange);
        if (collector instanceof IAetherManipulator manipulator) AetherRootCollectors.add(manipulator);
    }

    public void disableCollector(@Nonnull IAetherCollector collector, int dim, int x, int y, int z) {
        if (collector.isRemote()) return;
        AetherCollectors.remove(dim, x, y, z);
        AetherCollectors.forEachInRange(dim, x, y, z, c -> c.getCollectorHandler().removeCollectorInRange(collector));
        if (collector instanceof IAetherManipulator manipulator) AetherRootCollectors.remove(manipulator);
    }

    public void enableReleaser(@Nonnull IAetherReleaser releaser, int dim, int x, int y, int z) {
        if (releaser.isRemote()) return;
        AetherReleasers.put(releaser, dim, x, y, z, AetherConstants.MAX_COLLECTOR_RANGE);
        AetherCollectors.forEachInRange(dim, x, y, z, c -> c.getCollectorHandler().addReleaserInRange(releaser));
    }

    public void disableReleaser(@Nonnull IAetherReleaser releaser, int dim, int x, int y, int z) {
        if (releaser.isRemote()) return;
        AetherReleasers.remove(dim, x, y, z);
        AetherCollectors.forEachInRange(dim, x, y, z, c -> c.getCollectorHandler().removeReleaserInRange(releaser));
    }

    public void addOrphanedManipulator(IAetherManipulator manipulator) {
        orphanedManipulators.add(manipulator);
    }

    public void bulkOrphanSinks(IAetherManipulator manipulator) {
        Iterator<Map.Entry<InterDimCoords, IAetherManipulator>> iterSinks = manipulator.getAetherHandler().getAetherSinksIter();
        while (iterSinks.hasNext()) {
            orphanedManipulators.add(iterSinks.next().getValue());
        }
    }

    public void onServerTick(TickEvent.ServerTickEvent event) {
        AetherSearchQueue.clear();

        /*
         * Force-reset orphans, in case they are not updated in the final loop.
         * Ideally, this is more performant than enqueuing into the final
         * loop and having extra checks.
         */
        AetherSearchQueue.addAll(orphanedManipulators);
        while (!AetherSearchQueue.isEmpty()) {
            IAetherManipulator curr = AetherSearchQueue.pop();
            IAetherHandler handler = curr.getAetherHandler();
            handler.resetAether();
            Iterator<Map.Entry<InterDimCoords, IAetherManipulator>> iterSinks = handler.getAetherSinksIter();
            while (iterSinks.hasNext()) {
                IAetherManipulator next = iterSinks.next().getValue();
                if (next != null) AetherSearchQueue.push(next);
            }
            orphanedManipulators.remove(curr);
        }

        /*
         * Starting with known collectors, calculate Aether propagation using BFS.
         * Loops are handled in manipulators' getAetherFromSource
         */
        AetherSearchQueue.addAll(AetherRootCollectors);
        this.serverTick++;
        while (!AetherSearchQueue.isEmpty()) {
            IAetherManipulator curr = AetherSearchQueue.pop();
            IAetherHandler currHandler = curr.getAetherHandler();
            Iterator<Map.Entry<InterDimCoords, IAetherManipulator>> iterSinks = currHandler.getAetherSinksIter();
            while (iterSinks.hasNext()) {
                // TODO: listen for block updates and only check collision in range
                Map.Entry<InterDimCoords, IAetherManipulator> entry = iterSinks.next();
                if (entry.getValue() == null) {
                    InterDimCoords coords = entry.getKey();
                    TileEntity te = coords.getWorld().getTileEntity(coords.getX(), coords.getY(), coords.getZ());
                    if (te instanceof IAetherManipulator sink) {
                        entry.setValue(sink);
                    } else {
                        iterSinks.remove();
                        continue;
                    }
                }
                AetherSearchQueue.push(entry.getValue());
            }
            iterSinks = currHandler.getAetherSinksIter();
            while (iterSinks.hasNext()) {
                IAetherManipulator next = iterSinks.next().getValue();
                boolean shouldPropagate = next.getAetherHandler().getAetherFromSource(curr, this.serverTick);
                if (shouldPropagate) AetherSearchQueue.push(next);
            }
            if (currHandler.isUpdateable()) AetherUpdateQueue.add(curr);
        }
        for (IAetherManipulator curr : AetherUpdateQueue) {
            curr.getAetherHandler().updateAether();
        }
        AetherUpdateQueue.clear();
    }

    public void reset() {
        AetherCollectors = new ArrayProximityMap4D<>(VolumeShape.SPHERE);
        AetherReleasers = new ArrayProximityMap4D<>(VolumeShape.SPHERE);
        AetherRootCollectors = new HashSet<>();
        AetherSearchQueue = new ArrayDeque<>();
        AetherUpdateQueue = new HashSet<>();
    }
}
