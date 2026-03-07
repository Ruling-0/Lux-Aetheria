package com.ruling_0.luxaetheria.api.aether;

public enum AetherAspect {

    RED("LA.aether.aspect_red"),
    GREEN("LA.aether.aspect_green"),
    BLUE("LA.aether.aspect_blue"),;

    public static final AetherAspect[] VALUES = values();

    public final String local;

    AetherAspect(String local) {
        this.local = local;
    }
}
