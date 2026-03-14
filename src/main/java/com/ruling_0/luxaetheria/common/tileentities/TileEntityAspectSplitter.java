package com.ruling_0.luxaetheria.common.tileentities;

import com.ruling_0.luxaetheria.api.aether.AetherAspect;
import com.ruling_0.luxaetheria.api.aether.handlers.SimpleRelayHandler;
import com.ruling_0.luxaetheria.common.aether.handlers.AspectSplitterHandler;

public class TileEntityAspectSplitter extends BaseAetherRelay {

    public TileEntityAspectSplitter() {
        super(AetherAspect.VALUES.length);
    }

    @Override
    protected SimpleRelayHandler createAetherHandler(int maxAetherSinks) {
        return new AspectSplitterHandler(maxAetherSinks, this);
    }
}
