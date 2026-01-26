package com.ruling_0.luxaetheria.common.tileentities;

import javax.annotation.Nonnull;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.AxisAlignedBB;

import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.api.aether.AetherAspects;
import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.api.aether.IAetherReleaser;
import com.ruling_0.luxaetheria.api.aether.handlers.IAetherHandler;
import com.ruling_0.luxaetheria.api.aether.handlers.IReleaserHandler;
import com.ruling_0.luxaetheria.api.aether.handlers.SimpleAetherHandler;
import com.ruling_0.luxaetheria.api.utils.IWDMLAProvider;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityAethericFurnace extends TileEntityFurnace
    implements IAetherManipulator, IAetherReleaser, IWDMLAProvider {

    protected final SimpleAetherHandler aetherHandler;
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

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound compound = new NBTTagCompound();
        this.writeToNBT(compound);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, compound);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.func_148857_g());
        worldObj.markBlockRangeForRenderUpdate(
            this.xCoord,
            this.yCoord,
            this.zCoord,
            this.xCoord,
            this.yCoord,
            this.zCoord);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        double d0 = this.aetherHandler.getMaxSinkDistance();
        return AxisAlignedBB
            .getBoundingBox(this.xCoord, this.yCoord, this.zCoord, this.xCoord + 1, this.yCoord + 1, this.zCoord + 1)
            .expand(d0, d0, d0);
    }

    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return Math.max(this.aetherHandler.getMaxSinkDistance() * this.aetherHandler.getMaxSinkDistance(), 4096.0D);
    }
}
