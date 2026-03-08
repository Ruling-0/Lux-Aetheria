package com.ruling_0.luxaetheria.common.aether.handlers;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;

import com.ruling_0.luxaetheria.api.aether.AetherAspect;
import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.handlers.IRelayHandler;
import com.ruling_0.luxaetheria.api.aether.handlers.SimpleRelayHandler;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;

public class SplitterHandler extends SimpleRelayHandler {

    public SplitterHandler(int maxAetherSinks, TileEntity te) {
        super(maxAetherSinks, te);
    }

    @Nonnull
    @Override
    public AethericEnergyUnit getAetherForSink(long tick, @Nonnull IRelayHandler sinkHandler, double dist) {
        if (tick != this.lastSinkTick) {
            this.lastSinkTick = tick;
        }
        AethericEnergyUnit returnedAether = new AethericEnergyUnit(this.aetherIn);
        if (this.lastManipulatorTick != tick) return returnedAether;
        InterDimCoords sinkCoords = sinkHandler.getInterDimCoords();

        int outputIndex = this.getOutputIndex(sinkCoords);
        if (outputIndex < 0 || outputIndex >= AetherAspect.VALUES.length) return returnedAether;

        AetherAspect aspect = AetherAspect.VALUES[outputIndex];
        long aspectAmount = this.aetherIn.getAspectAmount(aspect);
        double[] ratios = new double[AetherAspect.VALUES.length];
        ratios[outputIndex] = 1.0;
        AethericEnergyUnit.AEUID id = this.aetherIn.getID();
        returnedAether = new AethericEnergyUnit(aspectAmount, ratios, id);

        if (this.handleSinkCollision(returnedAether, sinkCoords)) return returnedAether;
        this.handleLoss(dist, returnedAether);

        return returnedAether;
    }
}
