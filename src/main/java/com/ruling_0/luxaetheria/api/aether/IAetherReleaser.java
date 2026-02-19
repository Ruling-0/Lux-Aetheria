package com.ruling_0.luxaetheria.api.aether;

import com.ruling_0.luxaetheria.api.aether.handlers.IReleaserHandler;
import com.ruling_0.luxaetheria.common.aether.AetherManager;

/**
 * An Interface for things which release Aether into the environment. Must possess a {@link IReleaserHandler}.
 */
public interface IAetherReleaser {

    /**
     * Returns the {@link IReleaserHandler} for this releaser.
     */
    IReleaserHandler getReleaserHandler();

    /**
     * For disabling an {@link IAetherReleaser}.
     * Should be called whenever the collector is added to the world.
     * Must call {@link AetherManager#enableReleaser} server-side.
     */
    void enable();

    /**
     * For disabling an {@link IAetherReleaser}.
     * Should be called whenever the collector is destroyed or unloaded.
     * Must call {@link AetherManager#disableReleaser} server-side.
     */
    void disable();
}
