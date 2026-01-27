package com.ruling_0.luxaetheria.api.aether;

import static com.ruling_0.luxaetheria.api.aether.AetherConstants.BASE_AETHER_RECHARGE;

import java.text.DecimalFormat;
import java.util.Arrays;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;

public class AethericEnergyUnit {

    protected long amount = 0L;
    protected double[] aspectRatios = { 1.0D, 1.0D, 1.0D };
    protected AEUID id;

    public AethericEnergyUnit() {
        this(0);
    }

    public AethericEnergyUnit(long amount) {
        this(amount, 0, 0, 0, 0);
    }

    public AethericEnergyUnit(long amount, long origin, long tick, int dim) {
        this(amount, origin, tick, 0, dim);
    }

    public AethericEnergyUnit(long amount, long origin, long tick, int dim, int output) {
        this.amount = amount;
        this.id = new AEUID(origin, tick, output, dim);
    }

    public AethericEnergyUnit(long amount, @Nonnull double[] aspectRatios, long origin, long tick, int dim,
                              int output) {
        this(amount, origin, tick, 0, dim);
        if (aspectRatios.length == this.aspectRatios.length) this.aspectRatios = aspectRatios;
        else System.arraycopy(aspectRatios, 0, this.aspectRatios, 0, Math.min(aspectRatios.length,
            this.aspectRatios.length));
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

        public void setVals(@Nonnull TileEntity te, long tick, int output) {
            this.origin = CoordinatePacker.pack(te.xCoord, te.yCoord, te.zCoord);
            this.tick = tick;
            this.output = output;
            this.dim = te.getWorldObj().provider.dimensionId;
        }

        @Override
        public String toString() {
            return Long.toHexString(this.origin) + "-" + Long.toHexString(this.tick) + "-" +
                Integer.toHexString(this.output) + "-" + Integer.toHexString(this.dim);
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

    public void setToOther(@Nonnull AethericEnergyUnit otherAeU) {
        this.amount = otherAeU.amount;
        this.aspectRatios = otherAeU.aspectRatios.clone();
        this.id = otherAeU.id;
    }

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("0");
        return this.amount + " (" + EnumChatFormatting.RED +
            df.format(this.getAspectRatio(AetherAspects.RED.index) * 100) + EnumChatFormatting.RESET + "%, " +
            EnumChatFormatting.GREEN + df.format(this.getAspectRatio(AetherAspects.GREEN.index) * 100) +
            EnumChatFormatting.RESET + "%, " + EnumChatFormatting.BLUE +
            df.format(this.getAspectRatio(AetherAspects.BLUE.index) * 100) + EnumChatFormatting.RESET + "%)";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        AethericEnergyUnit that = (AethericEnergyUnit) other;
        if (this.amount != that.amount) return false;
        for (int i = 0; i < this.aspectRatios.length; i++) {
            if (this.aspectRatios[i] != that.aspectRatios[i]) return false;
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

    public AEUID getID() { return this.id; }

    public long getAmount() { return this.amount; }

    public void setAmount(long amount) { this.amount = amount; }

    public void addAmount(long amount) {
        this.amount += amount;
    }

    public long getAspectAmount(int index) {
        return (long) (this.amount * this.aspectRatios[index]);
    }

    public double getAspectRatio(int index) {
        return this.aspectRatios[index];
    }

    /**
     * Resets this AEU to 0 amount and all 1.0 aspect ratios.
     */
    public void reset() {
        this.amount = 0L;
        Arrays.fill(this.aspectRatios, 1.0D);
    }

    protected void recalculateRatios(long[] aspects) {
        if (this.amount == 0L) {
            Arrays.fill(this.aspectRatios, 1.0D);
            return;
        }
        for (int i = 0; i < this.aspectRatios.length; ++i) {
            this.aspectRatios[i] = (double) aspects[i] / this.amount;
        }
    }

    public void merge(AethericEnergyUnit incoming) {
        if (this.getAmount() == 0L) {
            this.setToOther(incoming);
            return;
        }
        long[] tempAspects = new long[AetherAspects.values().length];
        long tempAmount = this.getAmount();
        for (int i = 0; i < this.aspectRatios.length; ++i) {
            tempAspects[i] = this.getAspectAmount(i) + incoming.getAspectAmount(i);
            if (tempAspects[i] > tempAmount) tempAmount = tempAspects[i];
        }
        this.setAmount(tempAmount);
        this.recalculateRatios(tempAspects);
    }

    public void split(@Nonnull AethericEnergyUnit outgoing) {
        if (outgoing.getAmount() == 0L) return;
        long[] tempIAspects = new long[AetherAspects.values().length];
        long[] tempOAspects = new long[AetherAspects.values().length];
        long tempIAmount = 0L;
        long tempOAmount = 0L;
        boolean changedOut = false;
        for (int i = 0; i < this.aspectRatios.length; ++i) {
            if (this.getAspectAmount(i) < outgoing.getAspectAmount(i)) {
                changedOut = true;
                tempOAspects[i] = this.getAspectAmount(i);
                tempIAspects[i] = 0L;
                if (tempOAspects[i] > tempOAmount) tempOAmount = tempOAspects[i];
            }
            tempIAspects[i] = this.getAspectAmount(i) - outgoing.getAspectAmount(i);
            if (tempIAspects[i] > tempIAmount) tempIAmount = tempIAspects[i];
            if (tempOAspects[i] > tempOAmount) tempOAmount = tempOAspects[i];
        }
        this.setAmount(tempIAmount);
        outgoing.setAmount(tempOAmount);
        this.recalculateRatios(tempIAspects);
        if (changedOut) outgoing.recalculateRatios(tempOAspects);
    }

    public void moveToEquilibrium(long amount) {
        double[] aspects = new double[this.aspectRatios.length];
        Arrays.fill(aspects, 1.0D);
        this.moveToEquilibrium(amount, aspects);
    }

    /**
     * Decays this AEU towards the given equilibrium stats.
     *
     * @param amount  The equilibrium amount
     * @param aspects The aspect ratios of the equilibrium
     */
    public void moveToEquilibrium(long amount, double[] aspects) {
        long tempAmount = 0L;
        long[] tempAspects = new long[AetherAspects.values().length];
        for (int i = 0; i < this.aspectRatios.length; ++i) {
            double target = (amount * aspects[i]);
            double current = this.getAspectAmount(i);
            double delta = Math.cbrt(current) * (target - current) / BASE_AETHER_RECHARGE;
            if (delta < 0.0D) delta = Math.min(-1.0D, delta);
            else if (delta > 0.0D) delta = Math.max(1.0D, delta);
            tempAspects[i] = (long) (this.getAspectAmount(i) + delta);
            if (tempAspects[i] > tempAmount) tempAmount = tempAspects[i];
        }
        this.setAmount(tempAmount);
        this.recalculateRatios(tempAspects);
    }

    public long calculateLoss(double dist) {
        return (long) (this.amount * (1 - Math.exp(-0.009D * dist)));
    }

    public void writeToNBT(@Nonnull NBTTagCompound compound) {
        compound.setLong("amount", this.amount);
        NBTTagList nbtAspects = new NBTTagList();
        for (int i = 0; i < this.aspectRatios.length; i++) {
            NBTTagCompound aspect = new NBTTagCompound();
            aspect.setByte("index", (byte) i);
            aspect.setDouble("amount", this.aspectRatios[i]);
            nbtAspects.appendTag(aspect);
        }
        compound.setTag("aspects", nbtAspects);
        NBTTagCompound nbtID = new NBTTagCompound();
        this.id.writeToNBT(nbtID);
        compound.setTag("id", nbtID);
    }

    public void readFromNBT(@Nonnull NBTTagCompound compound) {
        this.amount = compound.getLong("amount");
        NBTTagList nbtAspects = compound.getTagList("aspects", 10);
        for (int i = 0; i < nbtAspects.tagCount(); i++) {
            NBTTagCompound aspect = nbtAspects.getCompoundTagAt(i);
            int index = aspect.getByte("index") & 0xFF;
            this.aspectRatios[index] = aspect.getDouble("amount");
        }
        NBTTagCompound nbtID = compound.getCompoundTag("id");
        this.id.readFromNBT(nbtID);
    }
}
