package com.ruling_0.luxaetheria.api.aether.handlers;

import static com.ruling_0.luxaetheria.api.aether.AetherConstants.BASE_AMBIENT_AETHER;

import java.util.ArrayList;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherCollector;
import com.ruling_0.luxaetheria.api.aether.IAetherRelay;
import com.ruling_0.luxaetheria.api.aether.IAetherReleaser;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;

public class SimpleCollectorHandler extends SimpleAetherHandler implements ICollectorHandler {

    public final AethericEnergyUnit ambientAether;

    private final int range;
    private final long collection;
    private long totalLoss = 0;

    private double collectorEfficiency = 1.0D;
    private int collectorsInRange = 1;
    private final ArrayList<IAetherCollector> collectorsInRangeList = new ArrayList<>();
    private final ArrayList<IAetherReleaser> aetherReleasers = new ArrayList<>();

    public SimpleCollectorHandler(TileEntity te, int range, long collection) {
        this(1, te, range, collection);
    }

    public SimpleCollectorHandler(int maxAetherSinks, TileEntity te, int range, long collection) {
        super(maxAetherSinks, te);
        this.range = range;
        this.collection = collection;
        this.ambientAether = new AethericEnergyUnit(BASE_AMBIENT_AETHER);
    }

    @Override
    public long getAetherCollectionAmount() {
        if (this.validSinks == 0) return 0;
        return (long) (this.collection * this.collectorEfficiency);
    }

    @Override
    public int getCollectorRange() { return this.range; }

    @Override
    public void addCollectorInRange(@Nonnull IAetherCollector collector) {
        ICollectorHandler collectorHandler = collector.getCollectorHandler();
        if (collectorHandler.equals(this)) return;
        this.collectorsInRange++;
        this.collectorEfficiency = Math.cbrt(1.0D / this.collectorsInRange);
        this.collectorsInRangeList.add(collector);
    }

    @Override
    public void removeCollectorInRange(@Nonnull IAetherCollector collector) {
        this.collectorsInRange--;
        this.collectorEfficiency = Math.cbrt(1.0D / this.collectorsInRange);
        this.collectorsInRangeList.remove(collector);
    }

    @Override
    public void addReleaserInRange(@Nonnull IAetherReleaser releaser) {
        this.aetherReleasers.add(releaser);
    }

    @Override
    public void removeReleaserInRange(@Nonnull IAetherReleaser releaser) {
        this.aetherReleasers.remove(releaser);
    }

    @Override
    public AethericEnergyUnit getAmbientAether() { return new AethericEnergyUnit(this.ambientAether); }

    @Override
    public boolean getAetherFromSource(@Nonnull IAetherRelay source, long tick) {
        return true;
    }

    @Nonnull
    @Override
    public AethericEnergyUnit getAetherForSink(long tick, @Nonnull IAetherHandler sinkHandler, double dist) {
        if (tick != this.lastSinkTick) {
            this.lastSinkTick = tick;
            this.aetherOut.reset();
        }
        AethericEnergyUnit returnedAether = new AethericEnergyUnit(this.aetherIn);
        InterDimCoords sinkCoords = sinkHandler.getInterDimCoords();

        // TODO: handle insufficient ambient aether
        returnedAether.setAmount(
            Math.min(this.getAetherCollectionAmount(), this.ambientAether.getAmount()) / this.aetherSinks.size());
        if (this.handleSinkCollision(returnedAether, sinkCoords)) return returnedAether;

        long loss = returnedAether.calculateLoss(dist);
        returnedAether.setAmount(returnedAether.getAmount() - loss);

        returnedAether.updateID(tick, this.getOutputIndex(sinkCoords));
        this.totalLoss += loss;
        return returnedAether;
    }

    @Override
    public boolean isUpdatable() { return true; }

    @Override
    public void updateAether() {
        if (this.aetherSinks.isEmpty()) {
            this.aetherOut.reset();
        }

        for (IAetherCollector collector : this.collectorsInRangeList) {
            this.ambientAether.addAmount(-collector.getCollectorHandler().getAetherCollectionAmount());
        }
        this.ambientAether.addAmount(-this.getAetherCollectionAmount());

        this.ambientAether.addAmount(this.totalLoss);
        for (IAetherReleaser releaser : this.aetherReleasers) {
            this.ambientAether.merge(releaser.getReleaserHandler().getAetherRelease());
        }

        this.ambientAether.moveToEquilibrium(BASE_AMBIENT_AETHER);
        this.totalLoss = 0;
    }

    @Override
    public void writeToNBT(@Nonnull NBTTagCompound compound) {
        super.writeToNBT(compound);
        NBTTagCompound nbtAetherAmbient = new NBTTagCompound();
        this.ambientAether.writeToNBT(nbtAetherAmbient);
        compound.setTag("aetherAmbient", nbtAetherAmbient);
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound compound) {
        super.readFromNBT(compound);
        NBTTagCompound nbtAetherAmbient = compound.getCompoundTag("aetherAmbient");
        this.ambientAether.readFromNBT(nbtAetherAmbient);
    }

    @Override
    public void writeWDMLAData(@Nonnull NBTTagCompound compound) {
        super.writeWDMLAData(compound);
        NBTTagCompound nbtAetherAmbient = new NBTTagCompound();
        this.ambientAether.writeToNBT(nbtAetherAmbient);
        compound.setTag("aetherAmbient", nbtAetherAmbient);
    }
}
