package com.ruling_0.luxaetheria.api.utils;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.gtnewhorizon.gtnhlib.blockpos.IWorldReferent;

import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraftforge.common.DimensionManager;

/**
 * Extension of {@link BlockPos} which also stores a reference to the {@link World} it is in.
 */
public class InterDimCoords extends BlockPos implements IWorldReferent {

    public final World world;

    public InterDimCoords() {
        super();
        this.world = null;
    }

    public InterDimCoords(NBTTagCompound compound) {
        this(compound.getInteger("x"), compound.getInteger("y"), compound.getInteger("z"), compound.getInteger("dim"));
    }

    public InterDimCoords(int x, int y, int z, int dim) {
        this(
            x,
            y,
            z,
            DimensionManager.getWorld(dim));
    }

    public InterDimCoords(int x, int y, int z, World world) {
        super(x, y, z);
        this.world = world;
    }

    public InterDimCoords(@Nonnull TileEntity te) {
        super(te.xCoord, te.yCoord, te.zCoord);
        this.world = te.getWorldObj();
    }

    /**
     * Distance between two InterDimCoords, where if they are in
     * different dimensions, the distance is 0.
     *
     * @param coords The point to measure distance to
     * @return 0 if different dimensions, distance otherwise
     */
    public double distance(@Nonnull InterDimCoords coords) {
        if (this.getDimID() != coords.getDimID()) return 0.0D;
        return super.distance(coords);
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    public int getDimID() {
        return this.world.provider.dimensionId;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) return false;
        return ((InterDimCoords) obj).world == this.world;
    }

    @Override
    public InterDimCoords copy() {
        return new InterDimCoords(this.x, this.y, this.z, this.world);
    }

    @Override
    public String toString() {
        return "(" + this.x + ", " + this.y + ", " + this.z + ", " + this.world.provider.dimensionId + ")";
    }

    public Vec3 getVec3() {
        return Vec3.createVectorHelper(this.x + 0.5, this.y + 0.5, this.z + 0.5);
    }
}
