package com.ruling_0.luxaetheria.utils;

import net.minecraft.block.Block;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;

public class LAUtils {

    /**
     * Equality check between {@link Vec3} because it's not implemented in Mojang's code.
     *
     * @return True if v1's x, y, and z match those of v2, false otherwise.
     */
    public static boolean vec3Equals(Vec3 v1, Vec3 v2) {
        return v1.xCoord == v2.xCoord && v1.yCoord == v2.yCoord && v1.zCoord == v2.zCoord;
    }

    /**
     * Check that the path between two Aether Manipulators is clear of blocks using
     * {@link World#rayTraceBlocks(Vec3, Vec3, boolean)}.
     *
     * @return True if the path is clear, false otherwise.
     */
    public static boolean checkRayCollision(World world, IAetherManipulator start, IAetherManipulator end,
                                            boolean includeLiquid) {
        return checkRayCollision(world, start.getAetherHandler().getPosVec3(), end.getAetherHandler().getPosVec3(), includeLiquid);
    }

    /**
     * Check that the path between two positions is clear of blocks using
     * {@link World#rayTraceBlocks(Vec3, Vec3, boolean)}.
     *
     * @return True if the path is clear, false otherwise.
     */
    public static boolean checkRayCollision(World world, Vec3 u, Vec3 v, boolean includeLiquid) {
        return getRayCollision(world, u, v, includeLiquid) == null;
    }

    public static MovingObjectPosition getRayCollision(World world, Vec3 u, Vec3 v, boolean includeLiquid) {
        Vec3 uv = u.subtract(v);
        Vec3 unit = uv.normalize();
        int step = 1;
        Vec3 pos = u.addVector(unit.xCoord * step, unit.yCoord * step, unit.zCoord * step);
        boolean negX = uv.xCoord < 0;
        boolean negY = uv.yCoord < 0;
        boolean negZ = uv.zCoord < 0;
        int dx = negX ? 1 : -1;
        int dy = negY ? 1 : -1;
        int dz = negZ ? 1 : -1;

        // Iterate until pos is beyond the target (v)
        while ((negX ? pos.xCoord >= v.xCoord : pos.xCoord <= v.xCoord) &&
            (negY ? pos.yCoord >= v.yCoord : pos.yCoord <= v.yCoord) &&
            (negZ ? pos.zCoord >= v.zCoord : pos.zCoord <= v.zCoord)) {
            int x = MathHelper.floor_double(pos.xCoord);
            int y = MathHelper.floor_double(pos.yCoord);
            int z = MathHelper.floor_double(pos.zCoord);

            // Check these first since they are closer to u than pos is
            if (unit.xCoord != 0) {
                MovingObjectPosition mop = getBlockCollision(world, x + dx, y, z, u, v, includeLiquid);
                if (mop != null) return mop;
            }
            if (unit.yCoord != 0) {
                MovingObjectPosition mop = getBlockCollision(world, x, y + dy, z, u, v, includeLiquid);
                if (mop != null) return mop;
            }
            if (unit.zCoord != 0) {
                MovingObjectPosition mop = getBlockCollision(world, x, y, z + dz, u, v, includeLiquid);
                if (mop != null) return mop;
            }
            MovingObjectPosition mop = getBlockCollision(world, x, y, z, u, v, includeLiquid);
            if (mop != null) return mop;

            step++;
            pos = u.addVector(unit.xCoord * step, unit.yCoord * step, unit.zCoord * step);
        }
        return null;
    }

    /**
     * Checks if a block collides with the line between two points. The block is at the given x, y, z coordinates.
     * The line is between u and v. Excludes the destination point (v).
     *
     * @return True if the block collides with the line, false otherwise.
     */
    public static boolean checkBlockCollision(World world, int x, int y, int z, Vec3 u, Vec3 v, boolean includeLiquid) {
        MovingObjectPosition mop = getBlockCollision(world, x, y, z, u, v, includeLiquid);
        return mop != null;
    }

    /**
     * Checks if a block collides with the line between two points. The block is at the given x, y, z coordinates.
     * The line is between u and v. Excludes the destination point (v).
     *
     * @return {@link MovingObjectPosition} if the block collides with the line, null otherwise.
     */
    public static MovingObjectPosition getBlockCollision(World world, int x, int y, int z, Vec3 u, Vec3 v,
                                                         boolean includeLiquid) {
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        if (block.canCollideCheck(meta, includeLiquid) &&
            block.getCollisionBoundingBoxFromPool(world, x, y, z) != null) {
            MovingObjectPosition mop = block.collisionRayTrace(world, x, y, z, u, v);
            if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return null;
            if (mop.blockX == MathHelper.floor_double(u.xCoord) &&
                mop.blockY == MathHelper.floor_double(u.yCoord) && mop.blockZ == MathHelper.floor_double(u.zCoord))
                return null;
            if (mop.blockX == MathHelper.floor_double(v.xCoord) &&
                mop.blockY == MathHelper.floor_double(v.yCoord) && mop.blockZ == MathHelper.floor_double(v.zCoord))
                return null;
            return mop;
        }
        return null;
    }
}
