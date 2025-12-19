package com.ruling_0.luxaetheria.common.tileentities;

import com.ruling_0.luxaetheria.common.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.common.aether.IAetherCollector;
import com.ruling_0.luxaetheria.common.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.common.aether.IAetherReleaser;
import net.minecraft.tileentity.TileEntity;

public class TileEntityCollectorPylon extends TileEntity implements IAetherManipulator, IAetherCollector {
    private final int range;
    private final long collection;
    private final AethericEnergyUnit ambient;

    private double collectorEfficiency = 1.0D;
    private int collectorsInRange = 0;
    private IAetherManipulator sink = null;

    public TileEntityCollectorPylon(int range, long collection) {
        super();
        this.range = range;
        this.collection = collection;
        this.ambient = new AethericEnergyUnit(BASE_PRODUCTION);
    }

    public long getAetherCollection() {
        if (sink == null) return 0;
        return (long) (this.collection * this.collectorEfficiency);
    }

    public int getCollectorRange() {
        return this.range;
    }

    public void addCollectorInRange(IAetherCollector collector) {
        this.collectorsInRange++;
        this.collectorEfficiency = Math.cbrt(1.0D / this.collectorsInRange);
        this.ambient.amount = this.ambient.amount - collector.getAetherCollection();
    }

    public void removeCollectorInRange(IAetherCollector collector) {
        this.collectorsInRange--;
        this.collectorEfficiency = Math.cbrt(1.0D / this.collectorsInRange);
        this.ambient.amount = this.ambient.amount + collector.getAetherCollection();
    }

    public void bulkUpdateCollectors(long collectionDelta, int countDelta) {
        this.ambient.amount = this.ambient.amount + collectionDelta;
        this.collectorsInRange = this.collectorsInRange + countDelta;
        this.collectorEfficiency = Math.cbrt(1.0D / this.collectorsInRange);
    }

    public void addReleaserInRange(IAetherReleaser releaser) {
        AethericEnergyUnit released = releaser.getAetherRelease();
        this.ambient.merge(released);
    }

    public void removeReleaserInRange(IAetherReleaser releaser) {
        AethericEnergyUnit released = releaser.getAetherRelease();
        this.ambient.split(released);
    }

    public boolean addSource(IAetherManipulator source) { return false; }

    public boolean removeSource(IAetherManipulator source) { return false; }

    public IAetherManipulator getSource() { return null; }

    public boolean addSink(IAetherManipulator sink) {
        if (this.sink == null) this.ambient.amount -= (long) (this.collection * this.collectorEfficiency);
        this.sink = sink;
        return true;
    }

    public boolean removeSink(IAetherManipulator sink) {
        this.ambient.amount += (long) (this.collection * this.collectorEfficiency);
        this.sink = null;
        return true;
    }

    public IAetherManipulator getSink() { return this.sink; }
}
