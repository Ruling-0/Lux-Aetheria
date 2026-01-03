package com.ruling_0.luxaetheria.common.aether;

import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nonnull;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.util.Vec3;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;

/**
 * An Interface for things which intake and/or output Aether.
 */
public interface IAetherManipulator {

    boolean addAetherSink(IAetherManipulator sink);

    boolean removeAetherSink(IAetherManipulator sink);

    Iterator<Map.Entry<Long, IAetherManipulator>> getAetherSinksIter();

    boolean hasOutput(long coords);

    IAetherManipulator getOutput(long coords);

    int getOutputIndex(long coords);

    @Nonnull
    Vec3 getPosVec3();

    @Nonnull
    BlockPos getPosBlockPos();

    boolean validateSink(IAetherManipulator sink);

    @Nonnull
    AethericEnergyUnit getAetherOut(long tick, IAetherManipulator sink, double dist);

    /**
     * For a given source, calculates the received aether, introducing loss.
     * If this is called more than once with the same source and tick, the later
     * amounts are released back to the environment.
     *
     * @param source The upstream manipulator
     * @param tick   The tick this is calculated on
     */
    boolean getAetherFromSource(IAetherManipulator source, long tick);

    void enable();

    /**
     * For disabling (making an invalid source/sink) an {@link IAetherManipulator}.
     * Should be called whenever the manipulator is destroyed or unloaded.
     * Removes the manipulator from source/sink lists of upstream/downstream manipulators.
     */
    void disable();

    boolean isUpdateable();

    void updateAether();
}
