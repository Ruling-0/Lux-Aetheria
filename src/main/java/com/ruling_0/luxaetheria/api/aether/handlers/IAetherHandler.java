package com.ruling_0.luxaetheria.api.aether.handlers;

import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.utils.InterDimCoords;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;

public interface IAetherHandler {

    boolean addAetherSink(IAetherManipulator sink);

    boolean removeAetherSink(IAetherManipulator sink);

    Iterator<Map.Entry<InterDimCoords, IAetherManipulator>> getAetherSinksIter();

    boolean hasOutput(InterDimCoords coords);

    IAetherManipulator getOutput(InterDimCoords coords);

    int getOutputIndex(InterDimCoords coords);

    @Nonnull
    Vec3 getPosVec3();

    @Nonnull
    InterDimCoords getInterDimCoords();

    int getDimension();

    boolean validateSink(@Nullable IAetherHandler sinkHandler);

    /**
     * For a given source, calculates the received aether, introducing loss.
     * If this is called more than once with the same source and tick, the later
     * amounts are released back to the environment.
     *
     * @param source The upstream manipulator
     * @param tick   The tick this is calculated on
     */
    boolean getAetherFromSource(@Nonnull IAetherManipulator source, long tick);

    @Nonnull
    AethericEnergyUnit getAetherOut(long tick, @Nonnull IAetherHandler sinkHandler, double dist);

    boolean isUpdateable();

    void updateAether();

    void resetAether();

    void disconnectFromSources();

    void writeToNBT(@Nonnull NBTTagCompound compound);

    void readFromNBT(@Nonnull NBTTagCompound compound);

    boolean isRemote();
}
