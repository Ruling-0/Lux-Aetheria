package com.ruling_0.luxaetheria.api.aether;

import com.ruling_0.luxaetheria.api.aether.handlers.ICollectorHandler;
import com.ruling_0.luxaetheria.common.aether.AetherManager;

/**
 * An Interface for things which collect ambient Aether. Must possess an {@link ICollectorHandler}.
 */
public interface IAetherCollector {

    /**
     * Returns the {@link ICollectorHandler} for this collector.
     */
    ICollectorHandler getCollectorHandler();

    /**
     * For disabling an {@link IAetherCollector}.
     * Should be called whenever the collector is added to the world.
     * Must call {@link AetherManager#enableCollector} server-side.
     */
    void enable();

    /**
     * For disabling an {@link IAetherCollector}.
     * Should be called whenever the collector is destroyed or unloaded.
     * Must call {@link AetherManager#disableCollector} server-side.
     */
    void disable();
}
