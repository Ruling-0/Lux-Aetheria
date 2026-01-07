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
import net.minecraft.util.Vec3;

import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;
import com.ruling_0.luxaetheria.utils.LAUtils;

public class SimpleAetherHandler implements IAetherHandler, IReleaserHandler {

    public final AethericEnergyUnit aetherIn = new AethericEnergyUnit();
    public final AethericEnergyUnit aetherOut = new AethericEnergyUnit();
    public final AethericEnergyUnit aetherRelease = new AethericEnergyUnit();
    /**
     * This is used so the aether values are available to WAILA.
     * Triggers an aether reset on the next {@link #getAetherFromSource(IAetherManipulator, long)} call.
     */
    public boolean doResetAether = false;

    protected final int maxAetherSinks;
    protected final HashSet<AethericEnergyUnit.AEUID> encounteredIDs = new HashSet<>();
    protected final HashMap<IAetherManipulator, Double> aetherSources = new HashMap<>();
    protected final HashMap<InterDimCoords, IAetherManipulator> aetherSinks;
    protected final HashMap<InterDimCoords, AethericEnergyUnit> sinkToAether;
    protected final InterDimCoords[] aetherOutputs;
    protected final InterDimCoords coords;

    private final IAetherManipulator owner;

    public SimpleAetherHandler(TileEntity te) {
        this(1, te);
    }

    public SimpleAetherHandler(int maxAetherSinks, TileEntity te) {
        this.maxAetherSinks = maxAetherSinks;
        this.coords = new InterDimCoords(te);
        this.owner = (IAetherManipulator) te;
        this.aetherSinks = new HashMap<>(this.maxAetherSinks);
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
        for (int i = 0; i < this.maxAetherSinks; ++i) {
            if (coords.equals(this.aetherOutputs[i])) {
                this.aetherOutputs[i] = null;
                this.aetherOut.split(this.sinkToAether.get(coords));
                this.sinkToAether.remove(coords);
                this.aetherSinks.remove(coords);
                LuxAetheria.proxy.aetherManager.addOrphanedManipulator(sink);
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<Map.Entry<InterDimCoords, IAetherManipulator>> getAetherSinksIter() {
        return this.aetherSinks.entrySet()
            .iterator();
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
        return this.coords.getVec3();
    }

    @Nonnull
    @Override
    public InterDimCoords getInterDimCoords() {
        return this.coords;
    }

    @Override
    public boolean isInvalidSink(@Nullable IAetherHandler sinkHandler) {
        if (sinkHandler == null) return true;
        return !LAUtils.checkRayCollision(this.coords.getWorld(), this.getPosVec3(), sinkHandler.getPosVec3(), true);
    }

    @Override
    public boolean getAetherFromSource(@Nonnull IAetherManipulator source, long tick) {
        if (this.doResetAether) {
            this.resetAether();
            this.doResetAether = false;
        }
        IAetherHandler sourceHandler = source.getAetherHandler();
        if (!this.aetherSources.containsKey(source)) {
            this.aetherSources.put(source, this.coords.distance(sourceHandler.getInterDimCoords()));
        }
        double dist = this.aetherSources.get(source);
        AethericEnergyUnit incoming = sourceHandler.getAetherOut(tick, this, dist);
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
    public AethericEnergyUnit getAetherOut(long tick, @Nonnull IAetherHandler sinkHandler, double dist) {
        AethericEnergyUnit prevAether = this.sinkToAether.get(sinkHandler.getInterDimCoords());
        if (prevAether != null) this.aetherOut.split(prevAether);
        AethericEnergyUnit returnedAether = new AethericEnergyUnit(this.aetherIn);
        if (this.isInvalidSink(sinkHandler)) {
            returnedAether.reset();
            // No need to merge since it'd merge 0
            this.sinkToAether.put(sinkHandler.getInterDimCoords(), returnedAether);
            return returnedAether;
        }
        returnedAether.setAmount(this.aetherIn.getAmount() / this.aetherSinks.size());
        this.aetherOut.merge(returnedAether);
        this.sinkToAether.put(sinkHandler.getInterDimCoords(), returnedAether);

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
    }

    @Override
    public void disconnectFromSources() {
        for (IAetherManipulator source : this.aetherSources.keySet()) {
            source.getAetherHandler()
                .removeAetherSink(this.owner);
        }
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
        for (int i = 0; i < nbtAetherSinks.tagCount(); ++i) {
            NBTTagCompound nbtAetherSink = nbtAetherSinks.getCompoundTagAt(i);
            int x = nbtAetherSink.getInteger("x");
            int y = nbtAetherSink.getInteger("y");
            int z = nbtAetherSink.getInteger("z");
            int dim = nbtAetherSink.getInteger("dim");
            InterDimCoords coords = new InterDimCoords(x, y, z, dim);
            this.aetherSinks.put(coords, null);
            this.aetherOutputs[nbtAetherSink.getByte("idx")] = coords;
        }
    }

}
