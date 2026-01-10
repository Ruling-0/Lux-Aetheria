package com.ruling_0.luxaetheria.common.tileentities;

import com.ruling_0.luxaetheria.api.utils.IWDMLAProvider;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.api.aether.IAetherReleaser;
import com.ruling_0.luxaetheria.api.aether.handlers.IAetherHandler;
import com.ruling_0.luxaetheria.api.aether.handlers.SimpleAetherHandler;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import javax.annotation.Nonnull;

/**
 * Base class for any {@link TileEntity} that can be linked into an Aether processing chain.
 */
public abstract class BaseAetherManipulator extends TileEntity implements IAetherManipulator, IAetherReleaser, IWDMLAProvider {

    protected SimpleAetherHandler aetherHandler;
    protected boolean isEnabled = false;
    protected InterDimCoords coords;

    public BaseAetherManipulator() {
        this(1);
    }

    public BaseAetherManipulator(int maxAetherSinks) {
        super();
        this.coords = null;
        this.aetherHandler = new SimpleAetherHandler(maxAetherSinks, this);
    }

    @Override
    public IAetherHandler getAetherHandler() {
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
}
