package com.ruling_0.luxaetheria.common.tileentities;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;

import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.api.aether.AetherAspects;
import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.api.aether.IAetherReleaser;
import com.ruling_0.luxaetheria.api.aether.handlers.IAetherHandler;
import com.ruling_0.luxaetheria.api.aether.handlers.IReleaserHandler;
import com.ruling_0.luxaetheria.api.aether.handlers.SimpleAetherHandler;

public class TileEntityAethericFurnace extends TileEntityFurnace implements IAetherManipulator, IAetherReleaser {

    protected SimpleAetherHandler aetherHandler;
    protected final int maxAetherSinks = 1;

    public TileEntityAethericFurnace() {
        super();
    }

    @Override
    public IAetherHandler getAetherHandler() {
        return this.aetherHandler;
    }

    @Override
    public IReleaserHandler getReleaserHandler() {
        return this.aetherHandler;
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        this.aetherHandler.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.aetherHandler.readFromNBT(compound);
    }

    @Override
    public void writeWAILAData(NBTTagCompound compound) {
        this.writeToNBT(compound);
        NBTTagCompound nbtAetherRelease = new NBTTagCompound();
        this.aetherHandler.aetherRelease.writeToNBT(nbtAetherRelease);
        compound.setTag("aetherRelease", nbtAetherRelease);
    }

    @Override
    public void enable() {
        if (this.worldObj.isRemote) return;
        this.aetherHandler = new SimpleAetherHandler(this.maxAetherSinks, this);
        LuxAetheria.proxy.aetherManager
            .enableReleaser(this, this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord);
    }

    @Override
    public void disable() {
        if (this.worldObj.isRemote) return;
        this.aetherHandler.disconnectFromSources();
        LuxAetheria.proxy.aetherManager.bulkOrphanSinks(this);
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
        if (!this.worldObj.isRemote) {
            if (this.aetherHandler.aetherIn.getAspectAmount(AetherAspects.RED.index) >= 1 && this.canSmelt()) {
                // This is decremented before checks for non-zero val
                this.furnaceBurnTime = Math.max(2, this.furnaceBurnTime + 1);
            }
        }
        super.updateEntity();
        if (!this.worldObj.isRemote) this.aetherHandler.doResetAether = true;
    }
}
