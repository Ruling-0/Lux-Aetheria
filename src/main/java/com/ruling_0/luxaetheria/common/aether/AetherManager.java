package com.ruling_0.luxaetheria.common.aether;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.gtnewhorizon.gtnhlib.datastructs.space.ArrayProximityMap4D;
import com.gtnewhorizon.gtnhlib.datastructs.space.VolumeShape;

import cpw.mods.fml.common.gameevent.TickEvent;

public class AetherManager {

    private static ArrayProximityMap4D<IAetherCollector> AetherCollectors;
    private static ArrayProximityMap4D<IAetherReleaser> AetherReleasers;
    private static HashSet<IAetherManipulator> AetherRootCollectors;
    private static Deque<IAetherManipulator> AetherSearchQueue;
    private static HashSet<IAetherManipulator> AetherUpdateQueue;
    private long serverTick = 0L;

    public void init() {
        AetherCollectors = new ArrayProximityMap4D<>(VolumeShape.SPHERE);
        AetherRootCollectors = new HashSet<>();
        AetherSearchQueue = new ArrayDeque<>();
        AetherUpdateQueue = new HashSet<>();
    }

    public void enableCollector(IAetherCollector collector, int dim, int x, int y, int z) {
        if (collector.isRemote()) return;
        // TODO: PR to GTNHLib that returns count from forEachInRange
        AtomicInteger count = new AtomicInteger();
        AtomicLong totalCollection = new AtomicLong();
        AetherCollectors.forEachInRange(dim, x, y, z, c -> {
            count.getAndIncrement();
            c.addCollectorInRange(collector);
            totalCollection.getAndAdd(c.getAetherCollectionAmount());
        });
        collector.bulkUpdateCollectors(-totalCollection.get(), count.get());
        AetherCollectors.put(collector, dim, x, y, z, collector.getCollectorRange());
        if (collector instanceof IAetherManipulator manipulator) AetherRootCollectors.add(manipulator);
    }

    public void disableCollector(IAetherCollector collector, int dim, int x, int y, int z) {
        if (collector.isRemote()) return;
        AetherCollectors.remove(dim, x, y, z);
        AetherCollectors.forEachInRange(dim, x, y, z, c -> c.removeCollectorInRange(collector));
        if (collector instanceof IAetherManipulator manipulator) AetherRootCollectors.remove(manipulator);
    }

    public void enableReleaser(IAetherReleaser releaser, int dim, int x, int y, int z) {
        if (releaser.isRemote()) return;
        AetherCollectors.forEachInRange(dim, x, y, z, c -> c.addReleaserInRange(releaser));
    }

    public void disableReleaser(IAetherReleaser releaser, int dim, int x, int y, int z) {
        if (releaser.isRemote()) return;
        AetherCollectors.forEachInRange(dim, x, y, z, c -> c.removeReleaserInRange(releaser));
    }

    public void onServerTick(TickEvent.ServerTickEvent event) {
        // TODO: Process aether chains through BFS starting with collectors. No loops!
        /*
         * Starting with known collectors, calculate Aether propagation using BFS.
         * Loops are handled in manipulators' getAetherFromSource
         */
        AetherSearchQueue.clear();
        AetherSearchQueue.addAll(AetherRootCollectors);
        this.serverTick++;
        while (!AetherSearchQueue.isEmpty()) {
            IAetherManipulator curr = AetherSearchQueue.pop();
            Iterator<IAetherManipulator> iterSinks = curr.getAetherSinksIter();
            while (iterSinks.hasNext()) {
                // TODO: listen for block updates and only check collision in range
                IAetherManipulator next = iterSinks.next();
                if (curr.validateSink(next)) {
                    iterSinks.remove();
                    AetherSearchQueue.push(next);
                }
            }
            iterSinks = curr.getAetherSinksIter();
            while (iterSinks.hasNext()) {
                IAetherManipulator next = iterSinks.next();
                boolean shouldPropagate = next.getAetherFromSource(curr, this.serverTick);
                if (shouldPropagate) AetherSearchQueue.push(next);
            }
            if (curr.isUpdateable()) AetherUpdateQueue.add(curr);
        }
        for (IAetherManipulator curr : AetherUpdateQueue) {
            curr.updateAether();
        }
        AetherUpdateQueue.clear();
    }
}
