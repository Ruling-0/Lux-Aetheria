package com.ruling_0.luxaetheria.common.tileentities;

import java.util.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.ruling_0.luxaetheria.utils.LAUtils;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.ruling_0.luxaetheria.common.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.common.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.common.aether.IAetherReleaser;

/**
 * Base class for any {@link TileEntity} that can be linked into an Aether processing chain.
 */
public abstract class BaseAetherManipulator extends TileEntity implements IAetherManipulator, IAetherReleaser {

    protected AethericEnergyUnit aetherIn = new AethericEnergyUnit();
    protected AethericEnergyUnit aetherOut = new AethericEnergyUnit();
    protected AethericEnergyUnit aetherRelease = new AethericEnergyUnit();
    protected HashSet<AethericEnergyUnit.AEUID> encounteredIDs = new HashSet<>();

    protected int maxAetherSinks;
    protected HashMap<IAetherManipulator, Double> aetherSources = new HashMap<>();
    protected HashMap<Long, Pair<IAetherManipulator, Integer>> aetherSinks;
    protected long[] aetherOutputs;

    public BaseAetherManipulator() {
        this(1);
    }

    public BaseAetherManipulator(int maxAetherSinks) {
        super();
        this.maxAetherSinks = maxAetherSinks;
        this.aetherSinks = new HashMap<>(this.maxAetherSinks);
        this.aetherOutputs = new long[this.maxAetherSinks];
        for (int i = 0; i < this.maxAetherSinks; ++i) this.aetherOutputs[i] = -1L;
    }

    @Override
    public boolean addAetherSink(IAetherManipulator sink) {
        if (this.aetherSinks.size() < this.maxAetherSinks) {
            for (int i = 0; i < this.maxAetherSinks; ++i) {
                if (this.aetherOutputs[i] == -1L) {
                    this.aetherOutputs[i] = sink.getPosBlockPos().asLong();
                    this.aetherSinks.put(sink.getPosBlockPos().asLong(), Pair.of(sink, sink.getDimension()));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean removeAetherSink(IAetherManipulator sink) {
        long coords = sink.getPosBlockPos().asLong();
        for (int i = 0; i < this.maxAetherSinks; ++i) {
            if (this.aetherOutputs[i] == coords) {
                this.aetherOutputs[i] = -1L;
                this.aetherSinks.remove(coords);
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<Map.Entry<Long, Pair<IAetherManipulator, Integer>>> getAetherSinksIter() {
        return this.aetherSinks.entrySet().iterator();
    }

    @Override
    public boolean hasOutput(long coords) {
        for (int i = 0; i < this.maxAetherSinks; ++i) {
            if (this.aetherOutputs[i] == coords) return true;
        }
        return false;
    }

    @Nullable
    @Override
    public IAetherManipulator getOutput(long coords) {
        for (int i = 0; i < this.maxAetherSinks; ++i) {
            if (this.aetherOutputs[i] == coords) return this.aetherSinks.get(coords).left();
        }
        return null;
    }

    public int getOutputIndex(long coords) {
        for (int i = 0; i < this.maxAetherSinks; ++i) {
            if (this.aetherOutputs[i] == coords) return i;
        }
        return -1;
    }

    @Nonnull
    @Override
    public Vec3 getPosVec3() {
        return Vec3.createVectorHelper(this.xCoord + 0.5, this.yCoord + 0.5, this.zCoord + 0.5);
    }

    @Nonnull
    @Override
    public BlockPos getPosBlockPos() {
        return new BlockPos(this.xCoord, this.yCoord, this.zCoord);
    }

    @Override
    public int getDimension() {
        return this.worldObj.provider.dimensionId;
    }

    @Override
    public boolean validateSink(@Nullable IAetherManipulator sink) {
        if (sink == null) return false;
        return LAUtils.checkRayCollision(this.worldObj, this.getPosVec3(), sink.getPosVec3(), true);
    }

    @Override
    public boolean getAetherFromSource(IAetherManipulator source, long tick) {
        if (!this.aetherSources.containsKey(source)) {
            this.aetherSources.put(source, this.getPosBlockPos().distance(source.getPosBlockPos()));
        }
        double dist = this.aetherSources.get(source);
        AethericEnergyUnit incoming = source.getAetherOut(tick, this, dist);
        if (this.encounteredIDs.add(incoming.getID())) {
            this.aetherIn.merge(incoming);
            return true;
        } else {
            this.aetherRelease.merge(incoming);
        }
        return false;
    }

    @Nonnull
    @Override
    public AethericEnergyUnit getAetherOut(long tick, IAetherManipulator sink, double dist) {
        this.aetherOut.setToOther(this.aetherIn);
        this.aetherOut.setAmount(this.aetherIn.getAmount() / this.aetherSinks.size());
        long loss = (long) (this.aetherOut.getAmount() * Math.exp(-0.003D * dist));
        this.aetherOut.setAmount(Math.max(0L, this.aetherOut.getAmount() - loss));
        AethericEnergyUnit toRelease = new AethericEnergyUnit(this.aetherOut);
        toRelease.setAmount(loss);
        this.aetherRelease.merge(toRelease);
        return this.aetherOut;
    }

    @Override
    public AethericEnergyUnit getAetherRelease() {
        return this.aetherRelease;
    }

    @Override
    public boolean isUpdateable() {
        return false;
    }

    @Override
    public void updateAether() {}

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);

        NBTTagCompound nbtAetherIn = new NBTTagCompound();
        this.aetherIn.writeToNBT(nbtAetherIn);
        compound.setTag("aetherIn", nbtAetherIn);
        NBTTagCompound nbtAetherOut = new NBTTagCompound();
        this.aetherOut.writeToNBT(nbtAetherOut);
        compound.setTag("aetherOut", nbtAetherOut);

        NBTTagList nbtAetherSinks = new NBTTagList();
        for (int i = 0; i < this.maxAetherSinks; ++i) {
            if (this.aetherOutputs[i] == -1L) continue;
            NBTTagCompound nbtAetherSink = new NBTTagCompound();
            nbtAetherSink.setLong("coords", this.aetherOutputs[i]);
            nbtAetherSink.setInteger("dim", this.aetherSinks.get(this.aetherOutputs[i]).right());
            nbtAetherSink.setByte("idx", (byte) i);
            nbtAetherSinks.appendTag(nbtAetherSink);
        }
        compound.setTag("aetherSinks", nbtAetherSinks);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        this.aetherIn.readFromNBT(compound.getCompoundTag("aetherIn"));
        this.aetherOut.readFromNBT(compound.getCompoundTag("aetherOut"));

        NBTTagList nbtAetherSinks = compound.getTagList("aetherSinks", 10);
        for (int i = 0; i < nbtAetherSinks.tagCount(); ++i) {
            NBTTagCompound nbtAetherSink = nbtAetherSinks.getCompoundTagAt(i);
            long coords = nbtAetherSink.getLong("coords");
            int dim = nbtAetherSink.getInteger("dim");
            this.aetherSinks.put(coords, Pair.of(null, dim));
            this.aetherOutputs[nbtAetherSink.getByte("idx")] = coords;
        }
    }

    @Override
    public void writeWAILAData(NBTTagCompound compound) {
        super.writeToNBT(compound);
        NBTTagCompound nbtAetherRelease = new NBTTagCompound();
        this.aetherRelease.writeToNBT(nbtAetherRelease);
        compound.setTag("aetherRelease", nbtAetherRelease);
    }

    @Override
    public boolean isRemote() {
        return this.worldObj.isRemote;
    }
}
