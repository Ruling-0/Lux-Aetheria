package com.ruling_0.luxaetheria.common.tileentities;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.ruling_0.luxaetheria.LAProxy;
import com.ruling_0.luxaetheria.common.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.common.aether.AetherAspects;
import com.ruling_0.luxaetheria.common.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.common.aether.IAetherReleaser;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.Vec3;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import java.util.*;

public class TileEntityAethericFurnace extends TileEntityFurnace implements IAetherManipulator, IAetherReleaser {
    public AethericEnergyUnit aetherIn;
    public AethericEnergyUnit aetherOut;
    public AethericEnergyUnit aetherRelease;

    protected int maxAetherSources = 1;
    protected int maxAetherSinks = 1;
    protected HashMap<IAetherManipulator, Double> aetherSources;
    protected ArrayList<IAetherManipulator> aetherSinks;
    protected HashSet<AethericEnergyUnit.AEUID> encounteredIDs;

    public TileEntityAethericFurnace() {
        super();
        this.aetherRelease = new AethericEnergyUnit(0);
    }

    @Override
    public boolean addAetherSource(IAetherManipulator source) {
        if (this.aetherSources.size() < this.maxAetherSources) {
            this.aetherSources.put(source, this.getPosVec3().distanceTo(source.getPosVec3()));
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
        return this.aetherSources.keySet().iterator();
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
        return Vec3.createVectorHelper(this.xCoord, this.yCoord, this.zCoord);
    }

    @Nonnull
    @Override
    public BlockPos getPosBlockPos() {
        return new BlockPos(this.xCoord, this.yCoord, this.zCoord);
    }

    @Override
    public boolean getAetherFromSource(IAetherManipulator source, long tick) {
        double dist = this.aetherSources.get(source);
        AethericEnergyUnit incoming = source.getAetherOut(tick, this, dist);
        if (this.encounteredIDs.add(incoming.id)) {
            this.aetherIn.merge(incoming);
            return true;
        }
        else {
            this.aetherRelease.merge(incoming);
        }
        return false;
    }

    @Nonnull
    @Override
    public AethericEnergyUnit getAetherOut(long tick, IAetherManipulator sink, double dist) {
        this.aetherOut.setToOther(this.aetherIn);
        this.aetherOut.amount = this.aetherIn.amount / this.aetherSinks.size();
        long loss = (long) (this.aetherOut.amount * Math.exp(-0.003D * dist));
        this.aetherOut.amount = Math.max(0L, this.aetherOut.amount - loss);
        AethericEnergyUnit toRelease = new AethericEnergyUnit(this.aetherOut);
        toRelease.amount = loss;
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
            nbtAetherSource.setLong("coords", entry.getKey().getPosBlockPos().asLong());
            nbtAetherSource.setDouble("distance", entry.getValue());
            nbtAetherSources.appendTag(nbtAetherSource);
        }
        compound.setTag("aetherSources", nbtAetherSources);

        NBTTagList nbtAetherSinks = new NBTTagList();
        for (IAetherManipulator sink : this.aetherSinks) {
            NBTTagCompound nbtAetherSink = new NBTTagCompound();
            nbtAetherSink.setLong("coords", sink.getPosBlockPos().asLong());
            nbtAetherSinks.appendTag(nbtAetherSink);
        }
        compound.setTag("aetherSinks", nbtAetherSinks);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        this.aetherIn.readFromNBT(compound.getCompoundTag("aetherIn"));
        this.aetherOut.readFromNBT(compound.getCompoundTag("aetherOut"));
        this.aetherRelease.readFromNBT(compound.getCompoundTag("aetherRelease"));

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

    @Override
    public void enable() {
        LAProxy.aetherManager.enableReleaser(this, this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord);
        for (IAetherManipulator sink : this.aetherSinks) {
            sink.addAetherSource(this);
        }
        for (IAetherManipulator source : this.aetherSources.keySet()) {
            source.addAetherSink(this);
        }
    }

    @Override
    public void disable() {
        LAProxy.aetherManager.disableReleaser(this, this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord);
        for (IAetherManipulator sink : this.aetherSinks) {
            sink.removeAetherSource(this);
        }
        for (IAetherManipulator source : this.aetherSources.keySet()) {
            source.removeAetherSink(this);
        }
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
        if (this.getStackInSlot(0) == null)
        {
            return false;
        }
        else
        {
            ItemStack itemstack = FurnaceRecipes.smelting().getSmeltingResult(this.getStackInSlot(0));
            if (itemstack == null) return false;
            if (this.getStackInSlot(2) == null) return true;
            if (!this.getStackInSlot(2).isItemEqual(itemstack)) return false;
            int result = getStackInSlot(2).stackSize + itemstack.stackSize;
            return result <= getInventoryStackLimit() && result <= this.getStackInSlot(2).getMaxStackSize(); //Forge BugFix: Make it respect stack sizes properly.
        }
    }

    @Override
    public void updateEntity() {
        if (this.aetherIn.getAspectAmount(AetherAspects.RED.index) >= 1
            && this.canSmelt()) {
            // This is decremented before checks for non-zero val
            this.furnaceBurnTime = Math.max(2, this.furnaceBurnTime + 1);
        }
        super.updateEntity();
        this.aetherIn.reset();
        this.aetherRelease.reset();
        this.encounteredIDs = new HashSet<>();
    }
}
