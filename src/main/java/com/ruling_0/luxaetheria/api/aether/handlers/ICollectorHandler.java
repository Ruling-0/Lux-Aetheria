package com.ruling_0.luxaetheria.api.aether.handlers;

import javax.annotation.Nonnull;

import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherCollector;
import com.ruling_0.luxaetheria.api.aether.IAetherReleaser;

/**
 * Responsible for handling Aether for an {@link IAetherCollector}. This involves maintaining an
 * {@link AethericEnergyUnit} which reflects the ambient environment's Aether level.
 */
public interface ICollectorHandler {

    /**
     * Returns the (floored) collection of Aether, accounting for any efficiency changes.
     */
    long getAetherCollectionAmount();

    /**
     * Returns the range over which this collector is influenced by releasers and other collectors.
     */
    int getCollectorRange();

    /**
     * When an {@link IAetherCollector} is added to the world, this is called when that collector is within this
     * collector's range (from {@link #getCollectorRange}). This must reflect the collector's reduction in the ambient
     * Aether level, but can introduce other effects.
     * 
     * @param collector The newly added collector.
     */
    void addCollectorInRange(@Nonnull IAetherCollector collector);

    /**
     * When an {@link IAetherCollector} is removed from the world, this is called when that collector is within this
     * collector's range (from {@link #getCollectorRange}). This should reverse the effects of
     * {@link #addCollectorInRange}.
     * 
     * @param collector The collector being removed.
     */
    void removeCollectorInRange(@Nonnull IAetherCollector collector);

    /**
     * Called when this handler's owning {@link IAetherCollector} is added to the world, so that this can handle the
     * effects that would have been handled by {@link #addCollectorInRange}.
     * 
     * @param collectionDelta The total draw being updated, should be added directly to the ambient Aether level.
     * @param countDelta      The change in the number of collectors in range.
     */
    void bulkUpdateCollectors(long collectionDelta, int countDelta);

    /**
     * Called when an {@link IAetherReleaser} is added to the world within range of this handler.
     * This handler should keep track of releasers in range and manage their released Aether's contribution
     * to the ambient level.
     */
    void addReleaserInRange(@Nonnull IAetherReleaser releaser);

    /**
     * Called when an {@link IAetherReleaser} is removed from the world within range of this handler.
     * This should reverse the effects of {@link #addReleaserInRange}.
     */
    void removeReleaserInRange(@Nonnull IAetherReleaser releaser);

    /**
     * Should return a clone of the current ambient Aether level.
     */
    AethericEnergyUnit getAmbientAether();
}
