package com.ruling_0.luxaetheria.common.tileentities;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import com.ruling_0.luxaetheria.LAProxy;
import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.api.IAetherHandler;
import com.ruling_0.luxaetheria.common.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.common.aether.IAetherCollector;
import com.ruling_0.luxaetheria.common.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.common.aether.IAetherReleaser;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.NotNull;

import static com.ruling_0.luxaetheria.api.AetherConstants.BASE_AMBIENT_AETHER;

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
