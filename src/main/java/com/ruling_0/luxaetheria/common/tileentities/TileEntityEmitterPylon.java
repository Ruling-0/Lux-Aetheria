package com.ruling_0.luxaetheria.common.tileentities;

import com.ruling_0.luxaetheria.common.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.common.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.common.aether.IAetherReleaser;
import net.minecraft.tileentity.TileEntity;

public class TileEntityEmitterPylon extends TileEntity implements IAetherManipulator, IAetherReleaser {
    private IAetherManipulator source = null;

    public TileEntityEmitterPylon() {super();}

    public AethericEnergyUnit getAetherRelease() {
        //TODO: Aether chain logic to determine this value
    }

    public boolean addSource(IAetherManipulator source) {
        this.source = source;
        return true;
    }

    public boolean removeSource(IAetherManipulator source) {
        this.source = null;
        return true;
    }

    public IAetherManipulator getSource() { return this.source; }

    public boolean addSink(IAetherManipulator sink) { return false; }

    public boolean removeSink(IAetherManipulator sink) { return false; }

    public IAetherManipulator getSink() { return null; }
}
