package com.ruling_0.luxaetheria.common.tileentities;

import com.ruling_0.luxaetheria.api.aether.AetherAspect;
import com.ruling_0.luxaetheria.api.aether.handlers.SimpleRelayHandler;
import com.ruling_0.luxaetheria.common.aether.handlers.SplitterHandler;

public class TileEntityAetherSplitter extends BaseAetherRelay {

    public TileEntityAetherSplitter() {
        super(AetherAspect.VALUES.length);
    }

    @Override
    protected SimpleRelayHandler createAetherHandler(int maxAetherSinks) {
        return new SplitterHandler(maxAetherSinks, this);
    }
}
