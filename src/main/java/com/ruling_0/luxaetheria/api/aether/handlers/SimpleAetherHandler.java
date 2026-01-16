package com.ruling_0.luxaetheria.api.aether.handlers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import org.jetbrains.annotations.NotNull;

import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.api.utils.IWDMLAProvider;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;
import com.ruling_0.luxaetheria.utils.LAUtils;

public class SimpleAetherHandler implements IAetherHandler, IReleaserHandler, IWDMLAProvider {

    public final AethericEnergyUnit aetherIn = new AethericEnergyUnit();
    public final AethericEnergyUnit aetherOut = new AethericEnergyUnit();
    public final AethericEnergyUnit aetherRelease = new AethericEnergyUnit();
    /**
     * This is used so the aether values are available to WDMLA.
     * Triggers an aether reset on the next {@link #getAetherFromSource(IAetherManipulator, long)} call.
     */
    public boolean doResetAether = false;
    public double maxSinkDistance = 0.0D;

    protected final int maxAetherSinks;
    protected final HashSet<AethericEnergyUnit.AEUID> encounteredIDs = new HashSet<>();
    protected final HashMap<IAetherManipulator, Double> aetherSources = new HashMap<>();
    protected final HashMap<InterDimCoords, IAetherManipulator> aetherSinks;
    protected final HashMap<InterDimCoords, Vec3> sinkCollisionCoords;
    protected final HashMap<InterDimCoords, AethericEnergyUnit> sinkToAether;
    protected final InterDimCoords[] aetherOutputs;
    protected long lastSourceTick = -1L;
    protected long lastSinkTick = -1L;
    protected int validSinks = 0;

    private final IAetherManipulator owner;

    public SimpleAetherHandler(int maxAetherSinks, TileEntity te) {
        this.maxAetherSinks = maxAetherSinks;
        this.owner = (IAetherManipulator) te;
        this.aetherSinks = new HashMap<>(this.maxAetherSinks);
        this.sinkCollisionCoords = new HashMap<>(this.maxAetherSinks);
        this.sinkToAether = new HashMap<>(this.maxAetherSinks);
        this.aetherOutputs = new InterDimCoords[this.maxAetherSinks];
        for (int i = 0; i < this.maxAetherSinks; ++i) this.aetherOutputs[i] = null;
    }

    @Override
    public boolean addAetherSink(@Nonnull IAetherManipulator sink) {
        IAetherHandler sinkHandler = sink.getAetherHandler();
        if (this.aetherSinks.size() < this.maxAetherSinks) {
            for (int i = 0; i < this.maxAetherSinks; ++i) {
                if (this.aetherOutputs[i] == null) {
                    InterDimCoords coords = sinkHandler.getInterDimCoords();
                    this.aetherOutputs[i] = coords;
                    this.sinkToAether.put(coords, new AethericEnergyUnit());
                    this.aetherSinks.put(coords, sink);
                    this.sinkCollisionCoords.put(coords, coords.getVec3());
                    double dist = this.getInterDimCoords()
                        .distance(coords);
                    if (dist > this.maxSinkDistance) this.maxSinkDistance = dist;
                    this.validSinks++;
                    this.markForUpdate();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean removeAetherSink(@Nonnull IAetherManipulator sink) {
        InterDimCoords coords = sink.getAetherHandler()
            .getInterDimCoords();
        boolean removed = false;
        this.maxSinkDistance = 0.0D;
        for (int i = 0; i < this.maxAetherSinks; ++i) {
            if (!removed && coords.equals(this.aetherOutputs[i])) {
                this.aetherOutputs[i] = null;
                AethericEnergyUnit prevOut = this.sinkToAether.remove(coords);
                if (prevOut != null) this.aetherOut.split(prevOut);
                this.aetherSinks.remove(coords);
                this.sinkCollisionCoords.remove(coords);
                LuxAetheria.proxy.aetherManager.addOrphanedManipulator(sink);
                this.markForUpdate();
                this.validSinks--;
                removed = true;
                continue;
            }
            double dist = this.getInterDimCoords()
                .distance(coords);
            if (dist > this.maxSinkDistance) this.maxSinkDistance = dist;
        }
        return removed;
    }

    @Override
    public Iterator<Map.Entry<InterDimCoords, IAetherManipulator>> getAetherSinksIter() {
        return this.aetherSinks.entrySet()
            .iterator();
    }

    @Override
    public Vec3 getSinkCollisionCoords(InterDimCoords coords) {
        return this.sinkCollisionCoords.get(coords);
    }

    @Override
    public boolean hasOutput(InterDimCoords coords) {
        for (int i = 0; i < this.maxAetherSinks; ++i) {
            if (coords.equals(this.aetherOutputs[i])) return true;
        }
        return false;
    }

    @Nullable
    @Override
    public IAetherManipulator getOutput(InterDimCoords coords) {
        for (int i = 0; i < this.maxAetherSinks; ++i) {
            if (coords.equals(this.aetherOutputs[i])) return this.aetherSinks.get(coords);
        }
        return null;
    }

    @Override
    public int getOutputIndex(InterDimCoords coords) {
        for (int i = 0; i < this.maxAetherSinks; ++i) {
            if (coords.equals(this.aetherOutputs[i])) return i;
        }
        return -1;
    }

    @Nonnull
    @Override
    public Vec3 getPosVec3() {
        return this.getInterDimCoords()
            .getVec3();
    }

    @Nonnull
    @Override
    public InterDimCoords getInterDimCoords() {
        return this.owner.getInterDimCoords();
    }

    @Override
    public boolean isInvalidSink(@Nullable IAetherHandler sinkHandler) {
        if (sinkHandler == null) return true;
        return !LAUtils.checkRayCollision(
            this.getInterDimCoords()
                .getWorld(),
            this.getPosVec3(),
            sinkHandler.getPosVec3(),
            true);
    }

    @Override
    public AethericEnergyUnit getAetherOut() {
        return new AethericEnergyUnit(this.aetherOut);
    }

    @Override
    public double getMaxSinkDistance() {
        return this.maxSinkDistance;
    }

    @Override
    public boolean getAetherFromSource(@Nonnull IAetherManipulator source, long tick) {
        if (tick != this.lastSourceTick) {
            this.lastSourceTick = tick;
            this.aetherIn.reset();
            this.aetherRelease.reset();
            this.encounteredIDs.clear();
        }
        IAetherHandler sourceHandler = source.getAetherHandler();
        if (!this.aetherSources.containsKey(source)) {
            this.aetherSources.put(
                source,
                this.getInterDimCoords()
                    .distance(sourceHandler.getInterDimCoords()));
        }
        double dist = this.aetherSources.get(source);
        AethericEnergyUnit incoming = sourceHandler.getAetherForSink(tick, this, dist);
        if (this.encounteredIDs.add(incoming.getID())) {
            this.aetherIn.merge(incoming);
            if (this.aetherSinks.isEmpty()) this.aetherRelease.merge(incoming);
            return true;
        } else {
            this.aetherRelease.merge(incoming);
        }
        return false;
    }

    @Nonnull
    @Override
    public AethericEnergyUnit getAetherForSink(long tick, @Nonnull IAetherHandler sinkHandler, double dist) {
        if (tick != this.lastSinkTick) {
            this.lastSinkTick = tick;
            this.aetherOut.reset();
        }
        AethericEnergyUnit returnedAether = new AethericEnergyUnit(this.aetherIn);

        returnedAether.setAmount(this.aetherIn.getAmount() / this.aetherSinks.size());
        this.aetherOut.merge(returnedAether);
        AethericEnergyUnit oldAether = this.sinkToAether.put(sinkHandler.getInterDimCoords(), returnedAether);
        if (!returnedAether.equals(oldAether)) this.markForUpdate();

        MovingObjectPosition mop = LAUtils.getRayCollision(
            this.getInterDimCoords()
                .getWorld(),
            this.getPosVec3(),
            sinkHandler.getPosVec3(),
            true);
        if (mop != null) {
            Vec3 oldCoords = this.sinkCollisionCoords.get(sinkHandler.getInterDimCoords());
            if (!mop.hitVec.equals(oldCoords)) {
                if (!LAUtils.vec3Equals(
                    oldCoords,
                    sinkHandler.getInterDimCoords()
                        .getVec3()))
                    this.validSinks--;
                this.sinkCollisionCoords.put(sinkHandler.getInterDimCoords(), mop.hitVec);
                this.markForUpdate();
            }
            this.aetherRelease.merge(returnedAether);
            returnedAether.setAmount(0L);
            return returnedAether;
        }
        if (!LAUtils.vec3Equals(
            this.sinkCollisionCoords.get(sinkHandler.getInterDimCoords()),
            sinkHandler.getInterDimCoords()
                .getVec3())) {
            this.sinkCollisionCoords.put(
                sinkHandler.getInterDimCoords(),
                sinkHandler.getInterDimCoords()
                    .getVec3());
            this.validSinks++;
            this.markForUpdate();
        }

        long loss = returnedAether.calculateLoss(dist);
        returnedAether.setAmount(Math.max(0L, returnedAether.getAmount() - loss));

        AethericEnergyUnit toRelease = new AethericEnergyUnit(returnedAether);
        toRelease.setAmount(loss);
        this.aetherRelease.merge(toRelease);

        return returnedAether;
    }

    @Override
    public AethericEnergyUnit getAetherRelease() {
        return new AethericEnergyUnit(this.aetherRelease);
    }

    @Override
    public boolean isUpdatable() {
        return false;
    }

    @Override
    public void updateAether() {}

    @Override
    public void resetAether() {
        this.aetherIn.reset();
        this.aetherOut.reset();
        this.aetherRelease.reset();
        this.encounteredIDs.clear();
        this.sinkToAether.clear();
    }

    @Override
    public void disconnectFromSources() {
        for (IAetherManipulator source : this.aetherSources.keySet()) {
            source.getAetherHandler()
                .removeAetherSink(this.owner);
        }
    }

    protected void clearSinks() {
        this.aetherSinks.clear();
        this.sinkToAether.clear();
        for (int i = 0; i < this.maxAetherSinks; ++i) this.aetherOutputs[i] = null;
    }

    protected void markForUpdate() {
        this.getInterDimCoords()
            .getWorld()
            .markBlockForUpdate(
                this.getInterDimCoords()
                    .getX(),
                this.getInterDimCoords()
                    .getY(),
                this.getInterDimCoords()
                    .getZ());
    }

    @Override
    public void writeToNBT(@Nonnull NBTTagCompound compound) {
        NBTTagCompound nbtAetherIn = new NBTTagCompound();
        this.aetherIn.writeToNBT(nbtAetherIn);
        compound.setTag("aetherIn", nbtAetherIn);
        NBTTagCompound nbtAetherOut = new NBTTagCompound();
        this.aetherOut.writeToNBT(nbtAetherOut);
        compound.setTag("aetherOut", nbtAetherOut);

        NBTTagList nbtAetherSinks = new NBTTagList();
        for (int i = 0; i < this.maxAetherSinks; ++i) {
            if (this.aetherOutputs[i] == null) continue;
            NBTTagCompound nbtAetherSink = new NBTTagCompound();
            InterDimCoords coords = this.aetherOutputs[i];
            nbtAetherSink.setInteger("x", coords.getX());
            nbtAetherSink.setInteger("y", coords.getY());
            nbtAetherSink.setInteger("z", coords.getZ());
            nbtAetherSink.setInteger("dim", coords.getDimID());
            Vec3 colCoords = this.sinkCollisionCoords.get(coords);
            nbtAetherSink.setDouble("cx", colCoords.xCoord);
            nbtAetherSink.setDouble("cy", colCoords.yCoord);
            nbtAetherSink.setDouble("cz", colCoords.zCoord);
            nbtAetherSink.setByte("idx", (byte) i);
            nbtAetherSinks.appendTag(nbtAetherSink);
        }
        compound.setTag("aetherSinks", nbtAetherSinks);
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound compound) {
        this.aetherIn.readFromNBT(compound.getCompoundTag("aetherIn"));
        this.aetherOut.readFromNBT(compound.getCompoundTag("aetherOut"));

        NBTTagList nbtAetherSinks = compound.getTagList("aetherSinks", 10);
        this.clearSinks();
        for (int i = 0; i < nbtAetherSinks.tagCount(); ++i) {
            NBTTagCompound nbtAetherSink = nbtAetherSinks.getCompoundTagAt(i);
            int x = nbtAetherSink.getInteger("x");
            int y = nbtAetherSink.getInteger("y");
            int z = nbtAetherSink.getInteger("z");
            int dim = nbtAetherSink.getInteger("dim");
            double cx = nbtAetherSink.getDouble("cx");
            double cy = nbtAetherSink.getDouble("cy");
            double cz = nbtAetherSink.getDouble("cz");
            InterDimCoords coords = new InterDimCoords(x, y, z, dim);
            double dist = this.getInterDimCoords()
                .distance(coords);
            if (dist > this.maxSinkDistance) this.maxSinkDistance = dist;
            this.aetherSinks.put(coords, null);
            Vec3 colCoords = Vec3.createVectorHelper(cx, cy, cz);
            this.sinkCollisionCoords.put(coords, colCoords);
            if (coords.getVec3()
                .equals(colCoords)) validSinks++;
            else validSinks--;
            this.aetherOutputs[nbtAetherSink.getByte("idx")] = coords;
        }
    }

    @Override
    public void writeWDMLAData(@NotNull NBTTagCompound compound) {
        NBTTagCompound nbtAetherIn = new NBTTagCompound();
        this.aetherIn.writeToNBT(nbtAetherIn);
        compound.setTag("aetherIn", nbtAetherIn);
        NBTTagCompound nbtAetherOut = new NBTTagCompound();
        this.aetherOut.writeToNBT(nbtAetherOut);
        compound.setTag("aetherOut", nbtAetherOut);
        NBTTagCompound nbtAetherRelease = new NBTTagCompound();
        this.aetherRelease.writeToNBT(nbtAetherRelease);
        compound.setTag("aetherRelease", nbtAetherRelease);
    }

}
