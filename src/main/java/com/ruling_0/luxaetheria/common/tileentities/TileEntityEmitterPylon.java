package com.ruling_0.luxaetheria.common.tileentities;

import com.ruling_0.luxaetheria.common.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.common.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.common.aether.IAetherReleaser;

public class TileEntityEmitterPylon extends BaseAetherManipulator implements IAetherReleaser {
    public TileEntityEmitterPylon() {super();}

    public AethericEnergyUnit getAetherRelease() {
        //TODO: Aether chain logic to determine this value
    }

    public boolean addSource(BaseAetherManipulator source) {
        this.aetherSource = source;
        return true;
    }

    public boolean removeSource(BaseAetherManipulator source) {
        this.aetherSource = null;
        return true;
    }
}
