package com.ruling_0.luxaetheria.api.aether.handlers;

import java.util.Iterator;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherCollector;
import com.ruling_0.luxaetheria.api.aether.IAetherRelay;
import com.ruling_0.luxaetheria.api.aether.IAetherReleaser;
import com.ruling_0.luxaetheria.api.aether.connections.ImmutableSinkConnection;
import com.ruling_0.luxaetheria.api.aether.connections.SinkConnection;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;
import com.ruling_0.luxaetheria.common.aether.AetherManager;
import org.joml.Vector3fc;

/**
 * Responsible for handling Aether flow and processing for a {@link IAetherRelay} or other object that
 * participates in an Aether processing chain.
 * <p>
 * The processing chain occurs every tick, where it traverses BFS style from {@link IAetherCollector} root nodes.
 * At each step, the current node retrieves Aether from connected sources
 * ({@link #getAetherFromSource(IAetherRelay, long)}).
 * <p>
 * {@link IAetherCollector}s keep track of an ambient level, which can be impacted by {@link IAetherReleaser}s.
 * If the current node is an {@link IAetherReleaser}, it may change the amount of aether it releases into the
 * environment
 * during the BFS traversal. Thus, nodes (like {@link IAetherCollector}s) which need to do updates that can only occur
 * at
 * the very end of the BFS traversal (like calculate ambient levels from changed Aether release) return true in
 * {@link #isUpdatable()}.
 * <p>
 * Details of the BFS traversal can be seen in {@link AetherManager}.
 * <p>
 * This is only required for {@link IAetherRelay}s as they belong to the BFS traversal. Stand-alone devices
 * (like machines that are their own collector and do not link to sinks) should not implement this.
 */
public interface IRelayHandler {

    /**
     * Registers a downstream {@link IAetherRelay} to receive aether from this one.
     *
     * @param sink The downstream manipulator.
     * @return Whether the sink was successfully added.
     */
    boolean addAetherSink(IAetherRelay sink);

    /**
     * Removes a downstream {@link IAetherRelay}.
     *
     * @param sink The downstream manipulator.
     * @return Whether the sink was successfully removed.
     */
    boolean removeAetherSink(IAetherRelay sink);

    /**
     * Gets an immutable iterator (of immutable elements) over the connected sinks.
     */
    Iterator<ImmutableSinkConnection> getAetherSinksIter();

    /**
     * Gets a mutable iterator (of mutable elements) over the connected sinks.
     */
    Iterator<SinkConnection> getMutableSinksIter();

    /**
     * Gets the point on the source to sink ray where it first collides with a block or the sink itself.
     */
    Vector3fc getSinkCollisionCoords(InterDimCoords coords);

    /**
     * Checks whether the provided {@link InterDimCoords} represent a registered Aether sink.
     */
    boolean hasSink(InterDimCoords coords);

    /**
     * Checks the registered Aether sinks for the given coords and returns which output index they are bound to.
     *
     * @return The index of the output in the array of outputs, or -1 if not found.
     */
    int getOutputIndex(InterDimCoords coords);

    /**
     * Gets a {@link Vec3} representing the center of this.
     *
     * @return {@link Vec3} object of each coord + 0.5.
     */
    @Nonnull
    Vec3 getPosVec3();

    /**
     * Gets the {@link InterDimCoords} of this.
     */
    @Nonnull
    InterDimCoords getInterDimCoords();

    /**
     * Get a clone of the total, pre-loss Aether output.
     */
    AethericEnergyUnit getAetherOut();

    /**
     * Get the maximum sink distance, to set render distance and AABB.
     */
    double getMaxSinkDistance();

    /**
     * Called by the {@link AetherManager} during BFS traversal.
     * Should use {@link #getAetherForSink} to retrieve an {@link AethericEnergyUnit} to process.
     * If this receives an {@link AethericEnergyUnit} with an {@link AethericEnergyUnit.AEUID} already encountered
     * since the last call to {@link #resetAether()}, it should return false to prevent further processing and
     * handle the excess Aether (such as by releasing to the environment).
     *
     * @param source The upstream manipulator
     * @param tick   The tick this is calculated on
     * @return True if this should be added to the BFS queue for downstream processing, false otherwise.
     */
    boolean getAetherFromSource(@Nonnull IAetherRelay source, long tick);

    /**
     * Provides an {@link AethericEnergyUnit} to a connected sink. If this is a root node of the
     * {@link AetherManager}'s BFS traversal, it must set the {@link AethericEnergyUnit.AEUID} using the provided
     * tick. It is also the responsibility of this to handle loss calculation, and if there is a loss, to release it.
     * <p>
     * This is the per-sink output; so, given multiple sinks, this must handle splitting overall output between them.
     * <p>
     * For UI purposes (like WDMLA), it is recommended to have one unit for showing non-loss output, which is updated
     * in this method, as the return value is the post-loss value.
     *
     * @param tick        Processing tick, for building a unique ID.
     * @param sinkHandler Aether handler of the connected sink.
     * @param dist        The distance between this and the sink, for loss calculations.
     * @return A new {@link AethericEnergyUnit} representing the post-loss output.
     */
    @Nonnull
    AethericEnergyUnit getAetherForSink(long tick, @Nonnull IRelayHandler sinkHandler, double dist);

    /**
     * Whether this handler should be updated every tick after the {@link AetherManager} runs its BFS
     * traversal (see {@link IRelayHandler}).
     * In this case, it is guaranteed that all external Aether values are final (set in {@link #getAetherFromSource}).
     * Thus, this should not manipulate released or to-sink Aether values.
     * If True, {@link #updateAether} is called later for the actual update.
     *
     * @return True if this should be updated, false otherwise.
     */
    boolean isUpdatable();

    /**
     * Update Aether values that rely on previous processing during this tick being already done. This should not modify
     * values (such as released Aether) that other updatable handlers may rely on.
     */
    void updateAether();

    /**
     * Called to reset the dynamic Aether values for this handler. This is called by the {@link AetherManager} when an
     * upstream source is removed, since it's possible this handler's owner was orphaned and so the BFS traversal won't
     * call {@link #getAetherFromSource}.
     */
    void resetAether();

    /**
     * This must iterate through all immediate upstream sources which this handler is a sink of, and call
     * {@link #removeAetherSink} to remove itself.
     */
    void disconnectFromSources();

    /**
     * Store this handler's information in NBT. It should write any Aether information that should be saved and loaded
     * alongside the chunk/world, as well as the coordinates of any sinks.
     *
     * @param compound The {@link NBTTagCompound} to write to.
     */
    void writeToNBT(@Nonnull NBTTagCompound compound);

    /**
     * Load this handler's information from NBT.
     */
    void readFromNBT(@Nonnull NBTTagCompound compound);
}
