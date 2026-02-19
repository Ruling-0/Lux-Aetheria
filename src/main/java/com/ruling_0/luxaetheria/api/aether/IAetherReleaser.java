package com.ruling_0.luxaetheria.api.aether;

import com.ruling_0.luxaetheria.api.aether.handlers.IReleaserHandler;
import com.ruling_0.luxaetheria.common.aether.AetherManager;

/**
 * An Interface for things which release Aether into the environment..
 */
public interface IAetherReleaser {

    /**
     * Returns a clone {@link AethericEnergyUnit} representing the amount of Aether released by this releaser.
     */
    AethericEnergyUnit getAetherRelease();

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
