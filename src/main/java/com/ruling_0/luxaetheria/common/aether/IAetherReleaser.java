package com.ruling_0.luxaetheria.common.aether;

/**
 * An Interface for things which release Aether into the environment.
 */
public interface IAetherReleaser {

    AethericEnergyUnit getAetherRelease();

    boolean isRemote();
}
