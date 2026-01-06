package com.ruling_0.luxaetheria.common.tileentities;

import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.api.aether.handlers.IAetherHandler;
import com.ruling_0.luxaetheria.common.aether.IAetherManipulator;
import net.minecraft.nbt.NBTTagCompound;

public class TileEntityCollectorPylon extends BaseAetherCollector implements IAetherManipulator {


    public TileEntityCollectorPylon() {
        //TODO: getting these from the block
        this(1, 12, 3);
    }

    public TileEntityCollectorPylon(int maxAetherSinks, int range, long collection) {
        super(maxAetherSinks, range, collection);
    }

    public IAetherHandler getAetherHandler() {
        return this.collectorHandler;
    }

    @Override
    public void disable() {
        if (this.worldObj.isRemote) return;
        LuxAetheria.proxy.aetherManager.bulkOrphanSinks(this);
        LuxAetheria.proxy.aetherManager
            .disableCollector(this, this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord);
    }

    @Override
    public void writeWAILAData(NBTTagCompound compound) {
        this.writeToNBT(compound);
    }
}
