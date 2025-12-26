package com.ruling_0.luxaetheria.common.aether;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.util.Vec3;

import com.gtnewhorizon.gtnhlib.datastructs.space.ArrayProximityMap4D;
import com.gtnewhorizon.gtnhlib.datastructs.space.VolumeShape;

import cpw.mods.fml.common.gameevent.TickEvent;

public class AetherManager {

    private static ArrayProximityMap4D<IAetherCollector> AetherCollectors;
    private static HashSet<IAetherManipulator> AetherRootCollectors;
    private static Deque<IAetherManipulator> AetherSearchQueue;
    private static HashSet<IAetherManipulator> AetherUpdateQueue;

    public void init() {
        AetherCollectors = new ArrayProximityMap4D<>(VolumeShape.SPHERE);
        AetherRootCollectors = new HashSet<>();
        AetherSearchQueue = new ArrayDeque<>();
        AetherUpdateQueue = new HashSet<>();
    }

    public void enableCollector(IAetherCollector collector, int dim, int x, int y, int z) {
        // TODO: PR to GTNHLib that returns count from forEachInRange
        AtomicInteger count = new AtomicInteger();
        AtomicLong totalCollection = new AtomicLong();
        AetherCollectors.forEachInRange(dim, x, y, z, c -> {
            count.getAndIncrement();
            c.addCollectorInRange(collector);
            totalCollection.getAndAdd(c.getAetherCollectionAmount());
        });
        collector.bulkUpdateCollectors(-totalCollection.get(), -count.get());
        AetherCollectors.put(collector, dim, x, y, z, collector.getCollectorRange());
        if (collector instanceof IAetherManipulator manipulator) AetherRootCollectors.add(manipulator);
    }

    public void disableCollector(IAetherCollector collector, int dim, int x, int y, int z) {
        AetherCollectors.remove(dim, x, y, z);
        AetherCollectors.forEachInRange(dim, x, y, z, c -> c.removeCollectorInRange(collector));
    }

    public void enableReleaser(IAetherReleaser releaser, int dim, int x, int y, int z) {
        AetherCollectors.forEachInRange(dim, x, y, z, c -> c.addReleaserInRange(releaser));
    }

    public void disableReleaser(IAetherReleaser releaser, int dim, int x, int y, int z) {
        AetherCollectors.forEachInRange(dim, x, y, z, c -> c.removeReleaserInRange(releaser));
    }

    public void onWorldTick(TickEvent.WorldTickEvent event) {
        // TODO: Process aether chains through BFS starting with collectors. No loops!
        /*
         * Starting with known collectors, calculate Aether propagation using BFS.
         * Loops are handled in manipulators' getAetherFromSource
         */
        AetherSearchQueue.clear();
        AetherSearchQueue.addAll(AetherRootCollectors);
        long tick = event.world.getTotalWorldTime();
        while (!AetherSearchQueue.isEmpty()) {
            IAetherManipulator curr = AetherSearchQueue.pop();
            Vec3 currPos = curr.getPosVec3();
            Iterator<IAetherManipulator> iterSinks = curr.getAetherSinksIter();
            while (iterSinks.hasNext()) {
                // TODO: ensure removal/chunk unload handled
                IAetherManipulator next = iterSinks.next();
                Vec3 nextPos = next.getPosVec3();
                if (event.world.rayTraceBlocks(currPos, nextPos, true) != null) {
                    iterSinks.remove();
                    AetherSearchQueue.push(next);
                }
            }
            iterSinks = curr.getAetherSinksIter();
            while (iterSinks.hasNext()) {
                IAetherManipulator next = iterSinks.next();
                boolean shouldPropagate = next.getAetherFromSource(curr, tick);
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
