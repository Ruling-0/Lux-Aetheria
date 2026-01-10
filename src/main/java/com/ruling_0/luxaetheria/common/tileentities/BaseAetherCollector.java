package com.ruling_0.luxaetheria.common.tileentities;

import com.ruling_0.luxaetheria.api.utils.IWDMLAProvider;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.api.aether.IAetherCollector;
import com.ruling_0.luxaetheria.api.aether.handlers.SimpleCollectorHandler;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

public abstract class BaseAetherCollector extends TileEntity implements IAetherCollector, IWDMLAProvider {

    protected SimpleCollectorHandler collectorHandler;
    protected final int range;
    protected final long collection;
    protected boolean isEnabled = false;

    public BaseAetherCollector(int maxAetherSinks, int range, long collection) {
        super();
        this.collectorHandler = new SimpleCollectorHandler(maxAetherSinks, this, range, collection);
        this.range = range;
        this.collection = collection;
    }

    @Override
    public SimpleCollectorHandler getCollectorHandler() {
        return this.collectorHandler;
    }

    @Override
    public void enable() {
        if (this.isEnabled) return;
        this.isEnabled = true;
        this.collectorHandler.ambientAether.updateID(0, 0, this);
        if (this.worldObj.isRemote) return;
        LuxAetheria.proxy.aetherManager
            .enableCollector(this, this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord);
    }

    @Override
    public void disable() {
        if (!this.isEnabled) return;
        this.isEnabled = false;
        if (this.worldObj.isRemote) return;
        LuxAetheria.proxy.aetherManager
            .disableCollector(this, this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord);
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
        super.writeToNBT(compound);
        this.collectorHandler.writeToNBT(compound);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.collectorHandler.readFromNBT(compound);
    }
}
