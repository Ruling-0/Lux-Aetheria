package com.ruling_0.luxaetheria.api.aether.handlers;

import static com.ruling_0.luxaetheria.api.aether.AetherConstants.BASE_AMBIENT_AETHER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;

import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherCollector;
import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.api.aether.IAetherReleaser;
import com.ruling_0.luxaetheria.utils.LAUtils;
import net.minecraft.util.Vec3;

public class SimpleCollectorHandler extends SimpleAetherHandler implements ICollectorHandler {

    public final AethericEnergyUnit ambientAether;

    private final int range;
    private final long collection;
    private long totalLoss = 0;

    private double collectorEfficiency = 1.0D;
    private int collectorsInRange = 1;
    private ArrayList<IAetherCollector> collectorsInRangeList = new ArrayList<>();
    private final HashMap<IAetherReleaser, AethericEnergyUnit> aetherReleasers = new HashMap<>();

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
    public int getCollectorRange() {
        return this.range;
    }

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
        AethericEnergyUnit release = releaser.getReleaserHandler()
            .getAetherRelease();
        this.aetherRelease.merge(release);
        this.aetherReleasers.put(releaser, release);
    }

    @Override
    public void removeReleaserInRange(@Nonnull IAetherReleaser releaser) {
        this.aetherRelease.split(this.aetherReleasers.get(releaser));
        this.aetherReleasers.remove(releaser);
    }

    @Override
    public AethericEnergyUnit getAmbientAether() {
        return new AethericEnergyUnit(this.ambientAether);
    }

    @Override
    public boolean getAetherFromSource(@Nonnull IAetherManipulator source, long tick) {
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

        // TODO: handle insufficient ambient aether
        returnedAether.setAmount(
            Math.min(this.getAetherCollectionAmount(), this.ambientAether.getAmount()) / this.aetherSinks.size());
        this.aetherOut.merge(returnedAether);
        this.sinkToAether.put(sinkHandler.getInterDimCoords(), returnedAether);

        MovingObjectPosition mop = LAUtils.getRayCollision(
            this.getInterDimCoords()
                .getWorld(),
            this.getPosVec3(),
            sinkHandler.getPosVec3(),
            true);
        if (mop != null) {
            Vec3 oldCoords = this.sinkCollisionCoords.get(sinkHandler.getInterDimCoords());
            if (!mop.hitVec.equals(oldCoords)) {
                if (!LAUtils.vec3Equals(oldCoords, sinkHandler.getInterDimCoords().getVec3())) this.validSinks--;
                this.sinkCollisionCoords.put(sinkHandler.getInterDimCoords(), mop.hitVec);
                this.markForUpdate();
            }
            this.totalLoss += returnedAether.getAmount();
            returnedAether.setAmount(0L);
            return returnedAether;
        }
        if (!LAUtils.vec3Equals(this.sinkCollisionCoords.get(sinkHandler.getInterDimCoords()),sinkHandler.getInterDimCoords().getVec3())) {
            this.sinkCollisionCoords.put(
                sinkHandler.getInterDimCoords(),
                sinkHandler.getInterDimCoords()
                    .getVec3());
            this.validSinks++;
            this.markForUpdate();
        }

        long loss = returnedAether.calculateLoss(dist);
        returnedAether.setAmount(returnedAether.getAmount() - loss);

        returnedAether.updateID(tick, this.getOutputIndex(sinkHandler.getInterDimCoords()));
        this.totalLoss += loss;
        return returnedAether;
    }

    @Override
    public boolean isUpdatable() {
        return true;
    }

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
        for (Map.Entry<IAetherReleaser, AethericEnergyUnit> entry : this.aetherReleasers.entrySet()) {
            AethericEnergyUnit newRelease = entry.getKey()
                .getReleaserHandler()
                .getAetherRelease();
            if (newRelease.equals(entry.getValue())) continue;
            this.aetherRelease.split(entry.getValue());
            this.aetherRelease.merge(newRelease);
            entry.getValue()
                .setToOther(newRelease);
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

    @Override
    public void writeWDMLAData(@Nonnull NBTTagCompound compound) {
        super.writeWDMLAData(compound);
        NBTTagCompound nbtAetherAmbient = new NBTTagCompound();
        this.ambientAether.writeToNBT(nbtAetherAmbient);
        compound.setTag("aetherAmbient", nbtAetherAmbient);
    }
}
