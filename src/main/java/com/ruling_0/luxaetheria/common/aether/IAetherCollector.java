package com.ruling_0.luxaetheria.common.aether;

/**
 * An Interface for things which collect ambient Aether.
 */
public interface IAetherCollector {

    long BASE_PRODUCTION = 30;

    long getAetherCollectionAmount();

    int getCollectorRange();

    void addCollectorInRange(IAetherCollector collector);

    void removeCollectorInRange(IAetherCollector collector);

    void bulkUpdateCollectors(long collectionDelta, int countDelta);

    void addReleaserInRange(IAetherReleaser releaser);

    void removeReleaserInRange(IAetherReleaser releaser);
}
