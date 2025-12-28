package com.ruling_0.luxaetheria.common.tileentities;

import java.util.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.ruling_0.luxaetheria.utils.LAUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;

import org.joml.Vector3i;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
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

    protected int maxAetherSources = 1;
    protected int maxAetherSinks = 1;
    protected HashMap<IAetherManipulator, Double> aetherSources;
    protected ArrayList<IAetherManipulator> aetherSinks;

    public BaseAetherManipulator() {
        this(1, 1);
    }

    public BaseAetherManipulator(int maxAetherSources, int maxAetherSinks) {
        super();
        this.maxAetherSources = maxAetherSources;
        this.maxAetherSinks = maxAetherSinks;
        this.aetherSources = new HashMap<>(maxAetherSources);
        this.aetherSinks = new ArrayList<>(maxAetherSinks);
    }

    @Override
    public boolean addAetherSource(IAetherManipulator source) {
        if (this.aetherSources.size() < this.maxAetherSources) {
            this.aetherSources.put(
                source,
                this.getPosVec3()
                    .distanceTo(source.getPosVec3()));
            return true;
        }
        return false;
    }

    @Override
    public boolean removeAetherSource(IAetherManipulator source) {
        return this.aetherSources.remove(source) != null;
    }

    @Override
    public Iterator<IAetherManipulator> getAetherSourcesIter() {
        return this.aetherSources.keySet()
            .iterator();
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
    public Iterator<IAetherManipulator> getAetherSinksIter() {
        return this.aetherSinks.iterator();
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
    public boolean validateSink(@Nullable IAetherManipulator sink) {
        if (sink == null) return false;
        return LAUtils.checkRayCollision(this.worldObj, this.getPosVec3(), sink.getPosVec3(), true);
    }

    @Override
    public boolean getAetherFromSource(IAetherManipulator source, long tick) {
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
        NBTTagCompound nbtAetherRelease = new NBTTagCompound();
        this.aetherRelease.writeToNBT(nbtAetherRelease);
        compound.setTag("aetherRelease", nbtAetherRelease);

        NBTTagList nbtAetherSources = new NBTTagList();
        for (Map.Entry<IAetherManipulator, Double> entry : this.aetherSources.entrySet()) {
            NBTTagCompound nbtAetherSource = new NBTTagCompound();
            nbtAetherSource.setLong(
                "coords",
                entry.getKey()
                    .getPosBlockPos()
                    .asLong());
            nbtAetherSource.setDouble("distance", entry.getValue());
            nbtAetherSources.appendTag(nbtAetherSource);
        }
        compound.setTag("aetherSources", nbtAetherSources);

        NBTTagList nbtAetherSinks = new NBTTagList();
        for (IAetherManipulator sink : this.aetherSinks) {
            NBTTagCompound nbtAetherSink = new NBTTagCompound();
            nbtAetherSink.setLong(
                "coords",
                sink.getPosBlockPos()
                    .asLong());
            nbtAetherSinks.appendTag(nbtAetherSink);
        }
        compound.setTag("aetherSinks", nbtAetherSinks);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        this.aetherIn.readFromNBT(compound.getCompoundTag("aetherIn"));
        this.aetherOut.readFromNBT(compound.getCompoundTag("aetherOut"));

        NBTTagList nbtAetherSources = compound.getTagList("aetherSources", 10);
        for (int i = 0; i < nbtAetherSources.tagCount(); i++) {
            NBTTagCompound nbtAetherSource = nbtAetherSources.getCompoundTagAt(i);
            long coords = nbtAetherSource.getLong("coords");
            Vector3i vec = new Vector3i();
            CoordinatePacker.unpack(coords, vec);
            double dist = nbtAetherSource.getDouble("distance");
            if (this.worldObj.getTileEntity(vec.x, vec.y, vec.z) instanceof IAetherManipulator source) {
                this.aetherSources.put(source, dist);
            }
        }

        NBTTagList nbtAetherSinks = compound.getTagList("aetherSinks", 10);
        for (int i = 0; i < nbtAetherSinks.tagCount(); i++) {
            NBTTagCompound nbtAetherSink = nbtAetherSinks.getCompoundTagAt(i);
            long coords = nbtAetherSink.getLong("coords");
            Vector3i vec = new Vector3i();
            CoordinatePacker.unpack(coords, vec);
            if (this.worldObj.getTileEntity(vec.x, vec.y, vec.z) instanceof IAetherManipulator sink) {
                this.aetherSinks.add(sink);
            }
        }
    }
}
