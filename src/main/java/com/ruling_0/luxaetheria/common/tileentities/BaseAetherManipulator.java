package com.ruling_0.luxaetheria.common.tileentities;

import com.ruling_0.luxaetheria.common.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.common.aether.IAetherManipulator;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;

import java.util.HashMap;

/**
 * Base class for any {@link TileEntity} that can be linked into an Aether processing chain.
 */
public abstract class BaseAetherManipulator extends TileEntity implements IAetherManipulator {
    public BaseAetherManipulator aetherSource = null;
    public BaseAetherManipulator aetherSink = null;
    public AethericEnergyUnit aetherIn = new AethericEnergyUnit();
    public AethericEnergyUnit aetherOut = new AethericEnergyUnit();

    protected int maxAetherSources = 0;
    protected int maxAetherSinks = 0;
    protected HashMap<IAetherManipulator, AethericEnergyUnit> aetherSources = new HashMap<>();
    protected HashMap<IAetherManipulator, AethericEnergyUnit> aetherSinks = new HashMap<>();

    public boolean addAetherSource(IAetherManipulator source) {
        return false;
    }

    public boolean removeAetherSource(IAetherManipulator source) {
        return false;
    }

    public IAetherManipulator getAetherSource() {
        return this.aetherSource;
    }

    public boolean addAetherSink(IAetherManipulator sink) {
        return false;
    }

    public boolean removeAetherSink(IAetherManipulator sink) {
        return false;
    }

    public IAetherManipulator getAetherSink() {
        return this.aetherSink;
    }

    public void addAetherIn(AethericEnergyUnit aetherIn) {
        this.aetherIn.merge(aetherIn);
    }

    public void removeAetherIn(AethericEnergyUnit aetherIn) {
        this.aetherIn.split(aetherIn);
    }

    public Vec3 getPosVec3() {
        return Vec3.createVectorHelper(xCoord, yCoord, zCoord);
    }
}
