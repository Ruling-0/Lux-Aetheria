package com.ruling_0.luxaetheria.common.tileentities;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import net.minecraftforge.common.util.ForgeDirection;

import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherRelay;
import com.ruling_0.luxaetheria.api.aether.IAetherReleaser;
import com.ruling_0.luxaetheria.api.aether.handlers.IRelayHandler;
import com.ruling_0.luxaetheria.api.aether.handlers.SimpleRelayHandler;
import com.ruling_0.luxaetheria.api.utils.IWDMLAProvider;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Base class for any {@link TileEntity} that can be linked into an Aether processing chain.
 */
public abstract class BaseAetherRelay extends TileEntity
                                      implements IAetherRelay, IAetherReleaser, IWDMLAProvider {

    protected final SimpleRelayHandler aetherHandler;
    protected boolean isEnabled = false;
    protected InterDimCoords coords;

    public BaseAetherRelay() {
        this(null, ForgeDirection.DOWN);
    }

    public BaseAetherRelay(World world, ForgeDirection dir) {
        this(world, dir, 1);
    }

    public BaseAetherRelay(World world, ForgeDirection dir, int maxAetherSinks) {
        super();
        this.coords = null;
        // TODO: the manipulator will need to get coords from the onBlockPlaced command, then lazy set the TE
        // maybe have a queue of packed coords, and every time it checks relays it checks for non-0 queue and resolves
        // it
        this.aetherHandler = new SimpleRelayHandler(maxAetherSinks, this);
    }

    @Override
    public IRelayHandler getAetherHandler() { return this.aetherHandler; }

    @Override
    public AethericEnergyUnit getAetherRelease() { return this.aetherHandler.getAetherRelease(); }

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

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() { return TileEntityCollectorPylon.INFINITE_EXTENT_AABB; }

    @SideOnly(Side.CLIENT)
    public double getMaxRenderDistanceSquared() {
        return Math.max(this.aetherHandler.getMaxSinkDistance() * this.aetherHandler.getMaxSinkDistance(),
            4096.0D);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }
}
