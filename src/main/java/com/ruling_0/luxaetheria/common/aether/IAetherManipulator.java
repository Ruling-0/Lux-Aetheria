package com.ruling_0.luxaetheria.common.aether;


import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import net.minecraft.util.Vec3;

/**
 * An Interface for things which intake and/or output Aether.
 */
public interface IAetherManipulator {
    boolean addAetherSource(IAetherManipulator source);

    boolean removeAetherSource(IAetherManipulator source);

    IAetherManipulator getAetherSource();

    boolean addAetherSink(IAetherManipulator sink);

    boolean removeAetherSink(IAetherManipulator sink);

    IAetherManipulator getAetherSink();

    Vec3 getPosVec3();
}
