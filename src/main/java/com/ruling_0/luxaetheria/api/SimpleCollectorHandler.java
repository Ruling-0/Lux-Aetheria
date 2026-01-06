package com.ruling_0.luxaetheria.api;

import com.ruling_0.luxaetheria.common.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.common.aether.IAetherCollector;
import com.ruling_0.luxaetheria.common.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.common.aether.IAetherReleaser;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

import static com.ruling_0.luxaetheria.api.AetherConstants.BASE_AMBIENT_AETHER;

public class SimpleCollectorHandler extends SimpleAetherHandler implements ICollectorHandler {

    public final AethericEnergyUnit ambientAether;

    private final int range;
    private final long collection;
    private long totalLoss = 0;

    private double collectorEfficiency = 1.0D;
    private int collectorsInRange = 1;
    private final HashMap<IAetherReleaser, AethericEnergyUnit> aetherReleasers = new HashMap<>();
    private boolean isEnabled = true; // TODO function for toggling on/off
    private final IAetherCollector owner;

    public SimpleCollectorHandler(TileEntity te, int range, long collection) {
        this(1, te, range, collection);
    }

    public SimpleCollectorHandler(int maxAetherSinks, TileEntity te, int range, long collection) {
        super(maxAetherSinks, te);
        this.owner = (IAetherCollector) te;
        this.range = range;
        this.collection = collection;
        this.ambientAether = new AethericEnergyUnit(BASE_AMBIENT_AETHER);
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
        ICollectorHandler collectorHandler = collector.getCollectorHandler();
        if (collectorHandler.equals(this)) return;
        this.collectorsInRange++;
        this.collectorEfficiency = Math.cbrt(1.0D / this.collectorsInRange);
        this.ambientAether.addAmount(-collectorHandler.getAetherCollectionAmount());
    }

    @Override
    public void removeCollectorInRange(@Nonnull IAetherCollector collector) {
        this.collectorsInRange--;
        this.collectorEfficiency = Math.cbrt(1.0D / this.collectorsInRange);
        this.ambientAether.addAmount(collector.getCollectorHandler().getAetherCollectionAmount());
    }

    @Override
    public void bulkUpdateCollectors(long collectionDelta, int countDelta) {
        this.ambientAether.addAmount(collectionDelta);
        this.collectorsInRange = this.collectorsInRange + countDelta;
        this.collectorEfficiency = Math.cbrt(1.0D / this.collectorsInRange);
    }

    @Override
    public void addReleaserInRange(@Nonnull IAetherReleaser releaser) {
        AethericEnergyUnit release = new AethericEnergyUnit(releaser.getReleaserHandler().getAetherRelease());
        this.aetherRelease.merge(release);
        this.aetherReleasers.put(releaser, release);
    }

    @Override
    public void removeReleaserInRange(IAetherReleaser releaser) {
        this.aetherRelease.split(this.aetherReleasers.get(releaser));
        this.aetherReleasers.remove(releaser);
    }

    @Override
    public AethericEnergyUnit getAmbientAether() {
        return this.ambientAether;
    }

    @Override
    public boolean getAetherFromSource(@Nonnull IAetherManipulator source, long tick) {
        return true;
    }

    @Nonnull
    @Override
    public AethericEnergyUnit getAetherOut(long tick, @Nonnull IAetherHandler sinkHandler, double dist) {
        AethericEnergyUnit prevAether = this.sinkToAether.get(sinkHandler.getInterDimCoords());
        if (prevAether != null) this.aetherOut.split(prevAether);
        AethericEnergyUnit returnedAether = new AethericEnergyUnit(this.aetherIn);
        if (!this.validateSink(sinkHandler)) {
            returnedAether.reset();
            // No need to merge since it'd merge 0
            this.sinkToAether.put(sinkHandler.getInterDimCoords(), returnedAether);
            return returnedAether;
        }
        // TODO: handle insufficient ambient aether
        returnedAether.setAmount(Math.min(this.getAetherCollectionAmount(), this.ambientAether.getAmount())
            / this.aetherSinks.size());
        this.aetherOut.merge(returnedAether);
        this.sinkToAether.put(sinkHandler.getInterDimCoords(), returnedAether);

        long loss = returnedAether.calculateLoss(dist);
        returnedAether.setAmount(Math.max(0L, returnedAether.getAmount() - loss));

        returnedAether.updateID(tick, this.getOutputIndex(sinkHandler.getInterDimCoords()));
        this.totalLoss += loss;
        return returnedAether;
    }

    @Override
    public boolean isUpdateable() {
        return true;
    }

    @Override
    public void updateAether() {
        if (this.aetherSinks.isEmpty()) {
            this.aetherOut.reset();
            this.ambientAether.moveToEquilibrium(BASE_AMBIENT_AETHER);
            return;
        }
        this.ambientAether.addAmount(-this.getAetherCollectionAmount());
        this.ambientAether.addAmount(this.totalLoss);
        for (Map.Entry<IAetherReleaser, AethericEnergyUnit> entry : this.aetherReleasers.entrySet()) {
            AethericEnergyUnit newRelease = entry.getKey()
                .getReleaserHandler().getAetherRelease();
            if (newRelease.equals(entry.getValue())) continue;
            this.aetherRelease.split(entry.getValue());
            this.aetherRelease.merge(newRelease);
            entry.getValue().setToOther(newRelease);
        }
        this.ambientAether.merge(this.aetherRelease);
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
}
