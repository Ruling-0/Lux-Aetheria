package com.ruling_0.luxaetheria.common.aether;

import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nonnull;

import com.ruling_0.luxaetheria.api.IAetherHandler;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;

/**
 * An Interface for things which intake and/or output Aether.
 */
public interface IAetherManipulator {
    IAetherHandler getAetherHandler();

    void enable();

    /**
     * For disabling (making an invalid source/sink) an {@link IAetherManipulator}.
     * Should be called whenever the manipulator is destroyed or unloaded.
     * Removes the manipulator from source/sink lists of upstream/downstream manipulators.
     */
    void disable();

    void writeWAILAData(NBTTagCompound compound);
}
