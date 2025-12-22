package com.ruling_0.luxaetheria.common.tileentities;

import com.ruling_0.luxaetheria.common.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.common.aether.IAetherCollector;
import com.ruling_0.luxaetheria.common.aether.IAetherReleaser;
import it.unimi.dsi.fastutil.Pair;

public class TileEntityCollectorPylon extends BaseAetherManipulator implements IAetherCollector {
    private final int range;
    private final long collection;
    private final AethericEnergyUnit ambientAether;

    private double collectorEfficiency = 1.0D;
    private int collectorsInRange = 0;
    private BaseAetherManipulator aetherSink = null;
    private boolean isEnabled = true; //TODO function for toggling on/off

    public TileEntityCollectorPylon(int range, long collection) {
        super();
        this.range = range;
        this.collection = collection;
        this.maxAetherSinks = 1;
        this.ambientAether = new AethericEnergyUnit(BASE_PRODUCTION);
    }

    public long getAetherCollectionAmount() {
        if (aetherSink == null) return 0;
        return (long) (this.collection * this.collectorEfficiency);
    }

    public int getCollectorRange() {
        return this.range;
    }

    public void addCollectorInRange(IAetherCollector collector) {
        this.collectorsInRange++;
        this.collectorEfficiency = Math.cbrt(1.0D / this.collectorsInRange);
        this.ambientAether.amount = this.ambientAether.amount - collector.getAetherCollectionAmount();
    }

    public void removeCollectorInRange(IAetherCollector collector) {
        this.collectorsInRange--;
        this.collectorEfficiency = Math.cbrt(1.0D / this.collectorsInRange);
        this.ambientAether.amount = this.ambientAether.amount + collector.getAetherCollectionAmount();
    }

    public void bulkUpdateCollectors(long collectionDelta, int countDelta) {
        this.ambientAether.amount = this.ambientAether.amount + collectionDelta;
        this.collectorsInRange = this.collectorsInRange + countDelta;
        this.collectorEfficiency = Math.cbrt(1.0D / this.collectorsInRange);
    }

    public void addReleaserInRange(IAetherReleaser releaser) {
        AethericEnergyUnit released = releaser.getAetherRelease();
        this.ambientAether.merge(released);
    }

    public void removeReleaserInRange(IAetherReleaser releaser) {
        AethericEnergyUnit released = releaser.getAetherRelease();
        this.ambientAether.split(released);
    }

    public boolean addAetherSink(BaseAetherManipulator sink) {
        if (this.aetherSinks.size() < this.maxAetherSinks) {
            this.aetherSinks.put(sink, new AethericEnergyUnit());
        }
        return false;
    }

    public boolean removeAetherSink(BaseAetherManipulator sink) {
        this.aetherSink = null;
        return true;
    }

    public void enable() {
        this.aetherOut.amount = this.getAetherCollectionAmount();
        this.isEnabled = true;
    }

    public void disable() {
        this.aetherOut.amount = 0;
        this.isEnabled = false;
    }

    @Override
    public void updateEntity() {
        if (this.isEnabled) {
            this.ambientAether.amount -= this.getAetherCollectionAmount();
            if (this.aetherSink != null) {
                this.aetherSink.addAetherIn(this.aetherOut);
            }
            else {
                this.ambientAether.amount += this.getAetherCollectionAmount();
            }
        }
    }
}
