package com.ruling_0.luxaetheria.common.aether;

public enum AetherAspects {

    RED(0, "LA.aether.aspect_red"),
    GREEN(1, "LA.aether.aspect_green"),
    BLUE(2, "LA.aether.aspect_blue"),;

    public static final AetherAspects[] VALUES = values();

    public final int index;
    public final String local;

    AetherAspects(int index, String local) {
        this.index = index;
        this.local = local;
    }
}
