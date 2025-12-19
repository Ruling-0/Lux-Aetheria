package com.ruling_0.luxaetheria.common.tileentities;

import com.ruling_0.luxaetheria.common.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.common.aether.IAetherCollector;
import com.ruling_0.luxaetheria.common.aether.IAetherReleaser;
import net.minecraft.tileentity.TileEntity;

public class TileEntityCollectorPylon extends TileEntity implements IAetherCollector {
    public int range;
    public long collection;
    public double collectorEfficiency = 1.0D;
    public int collectorsInRange = 0;
    protected AethericEnergyUnit ambient;

    public TileEntityCollectorPylon(int range, long collection) {
        super();
        this.range = range;
        this.collection = collection;
        this.ambient = new AethericEnergyUnit(BASE_PRODUCTION - this.collection);
    }

    public long getAetherCollection() {
        return this.collection;
    }

    public int getCollectorRange() {
        return this.range;
    }

    public void addCollectorInRange(IAetherCollector collector) {
        this.collectorsInRange++;
        this.ambient.amount = this.ambient.amount - collector.getAetherCollection();
        this.collectorEfficiency = Math.cbrt(1.0D / this.collectorsInRange);
    }

    public void removeCollectorInRange(IAetherCollector collector) {
        this.collectorsInRange--;
        this.ambient.amount = this.ambient.amount + collector.getAetherCollection();
        this.collectorEfficiency = Math.cbrt(1.0D / this.collectorsInRange);
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
}
