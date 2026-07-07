package com.ruling_0.luxaetheria.common.tileentities;

import com.ruling_0.luxaetheria.api.aether.handlers.SimpleRelayHandler;
import com.ruling_0.luxaetheria.common.aether.handlers.SplitterHandler;

public class TileEntityAetherSplitter extends BaseAetherRelay {

    public static final int MAX_OUTPUTS = 5;

    public TileEntityAetherSplitter() {
        super(MAX_OUTPUTS);
    }

    @Override
    protected SimpleRelayHandler createAetherHandler(int maxAetherSinks) {
        return new SplitterHandler(maxAetherSinks, this);
    }
}
