package com.ruling_0.luxaetheria.common.tileentities;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.api.aether.IAetherCollector;
import com.ruling_0.luxaetheria.api.aether.handlers.SimpleCollectorHandler;

public abstract class BaseAetherCollector extends TileEntity implements IAetherCollector {

    protected SimpleCollectorHandler collectorHandler;
    protected final int maxAetherSinks;
    protected final int range;
    protected final long collection;

    public BaseAetherCollector() {
        this(1, 1, 0);
    }

    public BaseAetherCollector(int maxAetherSinks, int range, long collection) {
        super();
        this.maxAetherSinks = maxAetherSinks;
        this.range = range;
        this.collection = collection;
    }

    @Override
    public SimpleCollectorHandler getCollectorHandler() {
        return this.collectorHandler;
    }

    @Override
    public void enable() {
        if (this.worldObj.isRemote) return;
        this.collectorHandler = new SimpleCollectorHandler(maxAetherSinks, this, range, collection);
        this.collectorHandler.ambientAether.updateID(0, 0, this);
        LuxAetheria.proxy.aetherManager
            .enableCollector(this, this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord);
    }

    @Override
    public void disable() {
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

    @Override
    public boolean isRemote() {
        return this.worldObj.isRemote;
    }
}
