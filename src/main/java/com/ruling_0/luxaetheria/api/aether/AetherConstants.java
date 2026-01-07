package com.ruling_0.luxaetheria.api.aether;

public final class AetherConstants {

    private AetherConstants() {}

    /**
     * The maximum possible collector range. Used to ensure collectors add previously existing releasers in range.
     */
    public static final int MAX_COLLECTOR_RANGE = 128;
    /**
     * The equilibrium point of ambient Aether in normal regions.
     */
    public static final long BASE_AMBIENT_AETHER = 3_000L;
    /**
     * Ticks to go from 2% to 98%, roughly. Goes 1 to 3000 in 494 ticks
     */
    public static final long BASE_AETHER_RECHARGE = 600L;
}
