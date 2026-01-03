package com.ruling_0.luxaetheria.utils;

import com.ruling_0.luxaetheria.common.aether.IAetherManipulator;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class LAUtils {

    public static boolean checkRayCollision(World world, IAetherManipulator start, IAetherManipulator end, boolean includeWater) {
        return checkRayCollision(world, start.getPosVec3(), end.getPosVec3(), includeWater);
    }

    public static boolean checkRayCollision(World world, Vec3 startPos, Vec3 endPos, boolean includeWater) {
        MovingObjectPosition mop = world.rayTraceBlocks(startPos, endPos, includeWater);
        if (mop == null) return true;
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
            && (mop.blockX != (startPos.xCoord-0.5) && mop.blockY != (startPos.yCoord-0.5) && mop.blockZ != (startPos.zCoord-0.5))
            && (mop.blockX != (endPos.xCoord-0.5) && mop.blockY != (endPos.yCoord-0.5) && mop.blockZ != (endPos.zCoord-0.5))) {
            return false;
        }
        return true;
    }
}
