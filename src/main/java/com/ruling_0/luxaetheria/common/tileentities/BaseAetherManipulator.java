package com.ruling_0.luxaetheria.common.tileentities;

import java.util.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.api.IAetherHandler;
import com.ruling_0.luxaetheria.api.SimpleAetherHandler;
import com.ruling_0.luxaetheria.utils.LAUtils;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.ruling_0.luxaetheria.common.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.common.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.common.aether.IAetherReleaser;

/**
 * Base class for any {@link TileEntity} that can be linked into an Aether processing chain.
 */
public abstract class BaseAetherManipulator extends TileEntity implements IAetherManipulator, IAetherReleaser {

    protected SimpleAetherHandler aetherHandler;
    protected final int maxAetherSinks;

    public BaseAetherManipulator() {
        this(1);
    }

    public BaseAetherManipulator(int maxAetherSinks) {
        super();
        this.maxAetherSinks = maxAetherSinks;
    }

    @Override
    public IAetherHandler getAetherHandler() {
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

    @Override
    public boolean isRemote() {
        return this.worldObj.isRemote;
    }
}
