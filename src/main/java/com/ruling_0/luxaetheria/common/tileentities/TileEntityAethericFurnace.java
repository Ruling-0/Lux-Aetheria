package com.ruling_0.luxaetheria.common.tileentities;

import java.util.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.utils.LAUtils;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.Vec3;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.ruling_0.luxaetheria.LAProxy;
import com.ruling_0.luxaetheria.api.AetherAspects;
import com.ruling_0.luxaetheria.common.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.common.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.common.aether.IAetherReleaser;

public class TileEntityAethericFurnace extends TileEntityFurnace implements IAetherManipulator, IAetherReleaser {

    public AethericEnergyUnit aetherIn;
    public AethericEnergyUnit aetherOut;
    public AethericEnergyUnit aetherRelease;

    protected int maxAetherSinks = 1;
    protected HashMap<IAetherManipulator, Double> aetherSources = new HashMap<>();
    protected HashMap<Long, Pair<IAetherManipulator, Integer>> aetherSinks;
    protected long[] aetherOutputs;
    protected HashSet<AethericEnergyUnit.AEUID> encounteredIDs = new HashSet<>();
    protected boolean doResetAether = false;

    public TileEntityAethericFurnace() {
        super();
        this.aetherIn = new AethericEnergyUnit();
        this.aetherOut = new AethericEnergyUnit();
        this.aetherRelease = new AethericEnergyUnit();
        this.aetherSinks = new HashMap<>(this.maxAetherSinks);
        this.aetherOutputs = new long[this.maxAetherSinks];
        for (int i = 0; i < this.maxAetherSinks; ++i) this.aetherOutputs[i] = -1L;
    }

    @Override
    public boolean addAetherSink(IAetherManipulator sink) {
        if (this.aetherSinks.size() < this.maxAetherSinks) {
            this.aetherSinks.put(sink.getPosBlockPos().asLong(), Pair.of(sink, sink.getDimension()));
        }
        return false;
    }

    @Override
    public boolean removeAetherSink(IAetherManipulator sink) {
        return this.aetherSinks.remove(sink.getPosBlockPos().asLong()) != null;
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
        if (this.doResetAether) {
            this.doResetAether = false;
            this.aetherIn.reset();
            this.aetherRelease.reset();
            this.encounteredIDs = new HashSet<>();
        }
        if (!this.aetherSources.containsKey(source)) {
            this.aetherSources.put(source, this.getPosBlockPos().distance(source.getPosBlockPos()));
        }
        double dist = this.aetherSources.get(source);
        AethericEnergyUnit incoming = source.getAetherOut(tick, this, dist);
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
    public void enable() {
        if (this.worldObj.isRemote) return;
        LuxAetheria.proxy.aetherManager
            .enableReleaser(this, this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord);
    }

    @Override
    public void disable() {
        if (this.worldObj.isRemote) return;
        LuxAetheria.proxy.aetherManager
            .disableReleaser(this, this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord);
    }

    @Override
    public void onChunkUnload() {
        this.disable();
    }

    @Override
    public void invalidate() {
        this.disable();
        super.invalidate();
    }

    @Override
    public void validate() {
        super.validate();
        this.enable();
    }

    protected boolean canSmelt() {
        if (this.getStackInSlot(0) == null) {
            return false;
        } else {
            ItemStack itemstack = FurnaceRecipes.smelting()
                .getSmeltingResult(this.getStackInSlot(0));
            if (itemstack == null) return false;
            if (this.getStackInSlot(2) == null) return true;
            if (!this.getStackInSlot(2)
                .isItemEqual(itemstack)) return false;
            int result = getStackInSlot(2).stackSize + itemstack.stackSize;
            return result <= getInventoryStackLimit() && result <= this.getStackInSlot(2)
                .getMaxStackSize(); // Forge BugFix: Make it respect stack sizes properly.
        }
    }

    @Override
    public void updateEntity() {
        if (this.aetherIn.getAspectAmount(AetherAspects.RED.index) >= 1 && this.canSmelt()) {
            // This is decremented before checks for non-zero val
            this.furnaceBurnTime = Math.max(2, this.furnaceBurnTime + 1);
        }
        super.updateEntity();
        // This is used so the aether values are available to WAILA
        this.doResetAether = true;
    }

    @Override
    public boolean isRemote() {
        return this.worldObj.isRemote;
    }
}
