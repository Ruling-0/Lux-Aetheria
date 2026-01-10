package com.ruling_0.luxaetheria.common.tileentities;

import com.ruling_0.luxaetheria.api.utils.IWDMLAProvider;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;
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
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nonnull;

public class TileEntityAethericFurnace extends TileEntityFurnace implements IAetherManipulator, IAetherReleaser, IWDMLAProvider {

    protected SimpleAetherHandler aetherHandler;
    protected final int maxAetherSinks = 1;
    protected InterDimCoords coords;
    protected boolean isEnabled = false;

    public TileEntityAethericFurnace() {
        this(1);
    }

    public TileEntityAethericFurnace(int maxAetherSinks) {
        super();
        this.coords = null;
        this.aetherHandler = new SimpleAetherHandler(maxAetherSinks, this);
    }

    @Override
    public IAetherHandler getAetherHandler() {
        return this.aetherHandler;
    }

    @Override
    public IReleaserHandler getReleaserHandler() {
        return this.aetherHandler;
    }

    @Nonnull
    @Override
    public InterDimCoords getInterDimCoords() {
        if (this.coords == null) this.coords = new InterDimCoords(this);
        return this.coords;
    }

    @Override
    public void enable() {
        if (this.isEnabled) return;
        this.isEnabled = true;
        if (this.worldObj.isRemote) return;
        LuxAetheria.proxy.aetherManager
            .enableReleaser(this, this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord);
    }

    @Override
    public void disable() {
        if (!this.isEnabled) return;
        this.isEnabled = false;
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

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        compound.setInteger("dimension", this.worldObj.provider.dimensionId);
        super.writeToNBT(compound);
        this.aetherHandler.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        this.coords = new InterDimCoords(compound);
        super.readFromNBT(compound);
        this.aetherHandler.readFromNBT(compound);
    }

    @Override
    public void writeWDMLAData(@Nonnull NBTTagCompound compound) {
        this.aetherHandler.writeWDMLAData(compound);
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
    }
}
