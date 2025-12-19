package com.ruling_0.luxaetheria.common.aether;

import com.gtnewhorizon.gtnhlib.datastructs.space.ArrayProximityMap4D;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AetherManager {
    private static ArrayProximityMap4D<IAetherCollector> AetherCollectors;

    public void enableCollector(IAetherCollector collector, int dim, int x, int y, int z) {
        //TODO: PR to GTNHLib that returns count from forEachInRange
        AtomicInteger count = new AtomicInteger();
        AtomicLong totalCollection = new AtomicLong();
        AetherCollectors.forEachInRange(dim, x, y, z, c -> {
            count.getAndIncrement(); c.addCollectorInRange(collector); totalCollection.getAndAdd(c.getAetherCollection());});
        collector.bulkUpdateCollectors(-totalCollection.get(), -count.get());
        AetherCollectors.put(collector, dim, x, y, z, collector.getCollectorRange());
    }

    public void disableCollector(IAetherCollector collector, int dim, int x, int y, int z) {
        AetherCollectors.forEachInRange(dim, x, y, z, c -> c.removeCollectorInRange(collector));
    }

    public void enableReleaser(IAetherReleaser releaser, int dim, int x, int y, int z) {
        AetherCollectors.forEachInRange(dim, x, y, z, c -> {c.addReleaserInRange(releaser);});
    }

    public void disableReleaser(IAetherReleaser releaser, int dim, int x, int y, int z) {
        AetherCollectors.forEachInRange(dim, x, y, z, c -> {c.removeReleaserInRange(releaser);});
    }
}
