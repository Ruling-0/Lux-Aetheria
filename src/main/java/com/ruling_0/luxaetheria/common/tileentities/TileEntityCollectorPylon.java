package com.ruling_0.luxaetheria.common.tileentities;

import com.ruling_0.luxaetheria.common.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.common.aether.IAetherCollector;
import com.ruling_0.luxaetheria.common.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.common.aether.IAetherReleaser;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class TileEntityCollectorPylon extends BaseAetherManipulator implements IAetherCollector {
    private final int range;
    private final long collection;
    private long totalLoss = 0;
    private final AethericEnergyUnit ambientAether;

    private double collectorEfficiency = 1.0D;
    private int collectorsInRange = 0;
    private final HashMap<IAetherReleaser, AethericEnergyUnit> aetherReleasers = new HashMap<>();
    private boolean isEnabled = true; //TODO function for toggling on/off

    public TileEntityCollectorPylon(int range, long collection) {
        super(0, 1);
        this.range = range;
        this.collection = collection;
        this.ambientAether = new AethericEnergyUnit(BASE_PRODUCTION, this);
    }

    @Override
    public long getAetherCollectionAmount() {
        if (this.aetherSinks.isEmpty()) return 0;
        return (long) (this.collection * this.collectorEfficiency);
    }

    @Override
    public int getCollectorRange() {
        return this.range;
    }

    @Override
    public void addCollectorInRange(IAetherCollector collector) {
        this.collectorsInRange++;
        this.collectorEfficiency = Math.cbrt(1.0D / this.collectorsInRange);
        this.ambientAether.amount = this.ambientAether.amount - collector.getAetherCollectionAmount();
    }

    @Override
    public void removeCollectorInRange(IAetherCollector collector) {
        this.collectorsInRange--;
        this.collectorEfficiency = Math.cbrt(1.0D / this.collectorsInRange);
        this.ambientAether.amount = this.ambientAether.amount + collector.getAetherCollectionAmount();
    }

    @Override
    public void bulkUpdateCollectors(long collectionDelta, int countDelta) {
        this.ambientAether.amount = this.ambientAether.amount + collectionDelta;
        this.collectorsInRange = this.collectorsInRange + countDelta;
        this.collectorEfficiency = Math.cbrt(1.0D / this.collectorsInRange);
    }

    @Override
    public void addReleaserInRange(IAetherReleaser releaser) {
        this.aetherReleasers.put(releaser, releaser.getAetherRelease());
    }

    @Override
    public void removeReleaserInRange(IAetherReleaser releaser) {
        this.aetherReleasers.remove(releaser);
    }

    @Override
    public boolean addAetherSink(IAetherManipulator sink) {
        if (this.aetherSinks.size() < this.maxAetherSinks) {
            return this.aetherSinks.add(sink);
        }
        return false;
    }

    @Override
    public boolean removeAetherSink(IAetherManipulator sink) {
        return this.aetherSinks.remove(sink);
    }

    @Override
    public boolean getAetherFromSource(IAetherManipulator source, long tick) {
        return true;
    }

    @Nonnull
    @Override
    public AethericEnergyUnit getAetherOut(long tick, IAetherManipulator sink, double dist) {
        this.aetherOut.setToOther(this.ambientAether);
        //TODO: handle insufficient ambient aether
        this.aetherOut.amount = Math.min(this.getAetherCollectionAmount(), this.ambientAether.amount) / this.aetherSinks.size();
        long loss = (long) (this.aetherOut.amount * Math.exp(-0.003D * dist));
        this.aetherOut.amount = Math.max(0L, this.aetherOut.amount - loss);
        this.aetherOut.updateID(tick, this.aetherSinks.indexOf(sink));
        this.totalLoss += loss;
        return this.aetherOut;
    }

    @Override
    public boolean isUpdateable() {
        return true;
    }

    @Override
    public void updateAether() {
        this.ambientAether.amount -= this.getAetherCollectionAmount();
        this.ambientAether.amount += this.totalLoss;
        for (Map.Entry<IAetherReleaser, AethericEnergyUnit> entry : this.aetherReleasers.entrySet()) {
            AethericEnergyUnit newRelease = entry.getKey().getAetherRelease();
            if (newRelease.equals(entry.getValue())) continue;
            this.ambientAether.split(entry.getValue());
            this.ambientAether.merge(newRelease);
        }
    }

    public void enable() {
        this.aetherOut.amount = this.getAetherCollectionAmount();
        this.isEnabled = true;
    }

    public void disable() {
        this.aetherOut.amount = 0;
        this.isEnabled = false;
    }
}
