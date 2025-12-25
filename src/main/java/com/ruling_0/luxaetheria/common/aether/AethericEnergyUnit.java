package com.ruling_0.luxaetheria.common.aether;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nonnull;
import java.util.Arrays;

public class AethericEnergyUnit {
    public long amount = 0L;
    public double[] aspects = {1.0D, 1.0D, 1.0D};
    public AEUID id;

    public AethericEnergyUnit() {}

    public AethericEnergyUnit(long amount) {
        this.init(amount, 0, 0, 0, 0);
    }

    public AethericEnergyUnit(long amount, long origin, long tick, int dim) {
        this.init(amount, origin, tick, 0, dim);
    }

    public AethericEnergyUnit(long amount, long origin, long tick, int dim, int output) {
        this.init(amount, origin, tick, output, dim);
    }

    public AethericEnergyUnit(long amount, TileEntity te) {
        this.initFromTE(amount, te, 0, 0);
    }

    public AethericEnergyUnit(long amount, TileEntity te, long tick) {
        this.initFromTE(amount, te, tick, 0);
    }

    public AethericEnergyUnit(long amount, TileEntity te, long tick, int output) {
        this.initFromTE(amount, te, tick, output);
    }

    public AethericEnergyUnit(AethericEnergyUnit otherAeU) {
        this.setToOther(otherAeU);
    }

    public static class AEUID {
        public long origin;
        public long tick;
        public int output;
        public int dim;

        public AEUID(TileEntity te, long tick, int output) {
            this.setVals(te, tick, output);
        }

        public AEUID(long origin, long tick, int output, int dim) {
            this.origin = origin;
            this.tick = tick;
            this.output = output;
            this.dim = dim;
        }

        public void setVals(TileEntity te, long tick, int output) {
            this.origin = CoordinatePacker.pack(te.xCoord, te.yCoord, te.zCoord);
            this.tick = tick;
            this.output = output;
            this.dim = te.getWorldObj().provider.dimensionId;
        }

        @Override
        public String toString() {
            return Long.toHexString(this.origin) + "-" + Long.toHexString(this.tick)
                + "-" + Integer.toHexString(this.output) + "-" + Integer.toHexString(this.dim);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            AEUID that = (AEUID) other;
            if (this.origin != that.origin) return false;
            if (this.tick != that.tick) return false;
            if (this.output != that.output) return false;
            return this.dim == that.dim;
        }

        public void writeToNBT(@Nonnull NBTTagCompound compound) {
            compound.setLong("origin", this.origin);
            compound.setLong("tick", this.tick);
            compound.setInteger("output", this.output);
            compound.setInteger("dim", this.dim);
        }

        public void readFromNBT(@Nonnull NBTTagCompound compound) {
            this.origin = compound.getLong("origin");
            this.tick = compound.getLong("tick");
            this.output = compound.getInteger("output");
            this.dim = compound.getInteger("dim");
        }
    }

    public void init(long amount, long origin, long tick, int output, int dim) {
        this.amount = amount;
        this.id = new AEUID(origin, tick, output, dim);
    }

    public void initFromTE(long amount, TileEntity te, long tick, int output) {
        this.amount = amount;
        this.id = new AEUID(te, tick, output);
    }

    public void setToOther(@Nonnull AethericEnergyUnit otherAeU) {
        this.amount = otherAeU.amount;
        this.aspects = otherAeU.aspects.clone();
        this.id = otherAeU.id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        AethericEnergyUnit that = (AethericEnergyUnit) other;
        if (this.amount != that.amount) return false;
        for (int i = 0; i < this.aspects.length; i++) {
            if (this.aspects[i] != that.aspects[i]) return false;
        }
        return this.id == that.id;
    }

    public void updateID(long tick, int output) {
        this.id.tick = tick;
        this.id.output = output;
    }

    public void updateID(long tick, int output, TileEntity te) {
        this.id.setVals(te, tick, output);
    }

    public long getAspectAmount(int index) {
        return (long) (this.amount * this.aspects[index]);
    }

    public void reset() {
        this.amount = 0L;
        Arrays.fill(this.aspects, 1.0D);
    }

    public void merge(AethericEnergyUnit incoming) {
        if (this.amount == 0L) {
            this.setToOther(incoming);
        }
        else {
            double propIncoming = (double) incoming.amount / this.amount;
            double propCurrent = 1.0D - propIncoming;
            for (int i = 0; i < this.aspects.length; i++) {
                this.aspects[i] = propIncoming * incoming.aspects[i]
                    + propCurrent * this.aspects[i];
            }
            this.amount += incoming.amount;
        }
    }

    public void split(@Nonnull AethericEnergyUnit outgoing) {
        this.amount -= outgoing.amount;
        if (this.amount == 0) {
            Arrays.fill(this.aspects, 1.0D);
        }
        else {
            double propOutgoing = (double) outgoing.amount / this.amount;
            double propCurrent = 1.0D - propOutgoing;
            for (int i = 0; i < this.aspects.length; i++) {
                this.aspects[i] = propOutgoing * outgoing.aspects[i]
                    / propCurrent;
            }
        }
    }

    public void writeToNBT(@Nonnull NBTTagCompound compound) {
        compound.setLong("amount", this.amount);
        NBTTagList nbtAspects = new NBTTagList();
        for (int i = 0; i < this.aspects.length; i++) {
            NBTTagCompound aspect = new NBTTagCompound();
            aspect.setByte("index", (byte)i);
            aspect.setDouble("amount", this.aspects[i]);
            nbtAspects.appendTag(aspect);
        }
        compound.setTag("aspects", nbtAspects);
        NBTTagCompound nbtID =  new NBTTagCompound();
        this.id.writeToNBT(nbtID);
        compound.setTag("id", nbtID);
    }

    public void readFromNBT(@Nonnull NBTTagCompound compound) {
        this.amount = compound.getLong("amount");
        NBTTagList nbtAspects = compound.getTagList("aspects", 10);
        for (int i = 0; i < nbtAspects.tagCount(); i++) {
            NBTTagCompound aspect = nbtAspects.getCompoundTagAt(i);
            int index = aspect.getByte("index") & 0xFF;
            this.aspects[index] = aspect.getDouble("amount");
        }
        NBTTagCompound nbtID = compound.getCompoundTag("id");
        this.id.readFromNBT(nbtID);
    }
}
