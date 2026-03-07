package com.ruling_0.luxaetheria.crossmod.wdmla;

import net.minecraft.util.ResourceLocation;

public final class Identifiers {

    public static final ResourceLocation PROV_AETHERMANIPULATOR = provLA("aether_manipulator");

    public static ResourceLocation provLA(String path) {
        return new ResourceLocation("luxaetheria", "prov_" + path);
    }
}
