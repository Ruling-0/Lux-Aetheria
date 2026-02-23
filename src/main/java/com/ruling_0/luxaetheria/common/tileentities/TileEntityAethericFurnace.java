package com.ruling_0.luxaetheria.common.tileentities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.api.aether.handlers.IRelayHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.AxisAlignedBB;

import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.api.aether.AetherAspect;
import com.ruling_0.luxaetheria.api.aether.IAetherRelay;
import com.ruling_0.luxaetheria.api.aether.IAetherReleaser;
import com.ruling_0.luxaetheria.api.aether.handlers.IReleaserHandler;
import com.ruling_0.luxaetheria.api.aether.handlers.SimpleRelayHandler;
import com.ruling_0.luxaetheria.api.utils.IWDMLAProvider;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class TileEntityAethericFurnace extends TileEntityFurnace
                                       implements IAetherManipulator, IAetherReleaser {

    protected final ArrayList<IAetherRelay> relays = new ArrayList<>();
    protected final ArrayDeque<InterDimCoords> relayCoords = new ArrayDeque<>();
    protected final AethericEnergyUnit aetherRelease = new AethericEnergyUnit();
    protected final AethericEnergyUnit consumption = new AethericEnergyUnit(1L, new double[]{1.0D, 0.0D, 0.0D});
    protected boolean isEnabled = false;
    protected @Nullable IAetherRelay activeRelay = null;

    public TileEntityAethericFurnace() {
        super();
    }

    @Override
    public void bindRelay(InterDimCoords relayCoords) {
        this.relayCoords.add(relayCoords);
    }

    @Override
    public void unbindRelay(@Nonnull IAetherRelay relay) {
        this.relays.remove(relay);
        if (relay.equals(this.activeRelay)) this.activeRelay = null;
    }

    @Override
    public boolean isActive(@Nonnull IAetherRelay relay) {
        return relay.equals(this.activeRelay);
    }

    @Override
    public boolean manipulate(@Nonnull AethericEnergyUnit aetherIn) {
        if (aetherIn.canSplit(this.consumption)) {
            aetherIn.split(this.consumption);
            return true;
        }
        return false;
    }

    @Override
    public AethericEnergyUnit getAetherRelease() { return new AethericEnergyUnit(this.aetherRelease); }

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
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
    }

    protected boolean canSmelt() {
        if (this.getStackInSlot(0) == null) {
            return false;
        }
        else {
            ItemStack itemstack = FurnaceRecipes.smelting().getSmeltingResult(this.getStackInSlot(0));
            if (itemstack == null) return false;
            if (this.getStackInSlot(2) == null) return true;
            if (!this.getStackInSlot(2).isItemEqual(itemstack)) return false;
            int result = getStackInSlot(2).stackSize + itemstack.stackSize;
            return result <= getInventoryStackLimit() && result <= this.getStackInSlot(2).getMaxStackSize();
        }
    }

    @Override
    public void updateEntity() {
        if (!this.worldObj.isRemote) {
            while (!this.relayCoords.isEmpty()) {
                InterDimCoords relayCoords = this.relayCoords.pop();
                TileEntity te = this.worldObj.getTileEntity(relayCoords.x, relayCoords.y, relayCoords.z);
                if (te instanceof IAetherRelay relay) {
                    this.relays.add(relay);
                    relay.getAetherHandler().setManipulator(this);
                }
            }
            for (IAetherRelay relay : this.relays) {
                final IRelayHandler handler = relay.getAetherHandler();
                AethericEnergyUnit aether = handler.getAetherIn();
                if (aether.canSplit(consumption) && this.canSmelt()) {
                    this.activeRelay = relay;
                    // This is decremented before checks for non-zero val
                    this.furnaceBurnTime = Math.max(2, this.furnaceBurnTime + 1);
                    break;
                }
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
        worldObj.markBlockRangeForRenderUpdate(this.xCoord, this.yCoord, this.zCoord, this.xCoord, this.yCoord,
            this.zCoord);
    }
}
