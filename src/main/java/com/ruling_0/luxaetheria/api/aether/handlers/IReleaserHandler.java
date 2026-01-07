package com.ruling_0.luxaetheria.api.aether.handlers;

import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherReleaser;

/**
 * Responsible for handling Aether for an {@link IAetherReleaser}.
 */
public interface IReleaserHandler {

    /**
     * Returns a clone {@link AethericEnergyUnit} representing the amount of Aether released by this releaser.
     */
    AethericEnergyUnit getAetherRelease();
}
