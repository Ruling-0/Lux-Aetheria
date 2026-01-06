package com.ruling_0.luxaetheria.api.aether.handlers;

import com.ruling_0.luxaetheria.common.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.common.aether.IAetherCollector;
import com.ruling_0.luxaetheria.common.aether.IAetherReleaser;

public interface ICollectorHandler {

    long getAetherCollectionAmount();

    int getCollectorRange();

    void addCollectorInRange(IAetherCollector collector);

    void removeCollectorInRange(IAetherCollector collector);

    void bulkUpdateCollectors(long collectionDelta, int countDelta);

    void addReleaserInRange(IAetherReleaser releaser);

    void removeReleaserInRange(IAetherReleaser releaser);

    AethericEnergyUnit getAmbientAether();
}
