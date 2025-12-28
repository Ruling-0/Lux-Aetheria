package com.ruling_0.luxaetheria.utils;

import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class LAUtils {

    public static boolean checkRayCollision(World world, Vec3 startPos, Vec3 endPos, boolean includeWater) {
        MovingObjectPosition mop = world.rayTraceBlocks(startPos, endPos, includeWater);
        if (mop == null) return true;
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
            && (mop.blockX != startPos.xCoord && mop.blockY != startPos.yCoord && mop.blockZ != startPos.zCoord)
            && (mop.blockX != endPos.xCoord && mop.blockY != endPos.yCoord && mop.blockZ != endPos.zCoord)) {
            return false;
        }
        return true;
    }
}
