package com.ruling_0.luxaetheria.common.aether.handlers;

import javax.annotation.Nonnull;

import net.minecraft.tileentity.TileEntity;

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
            long remainder = this.aetherIn.getAmount() % this.aetherSinks.numConnections();
            if (remainder > 0) {
                AethericEnergyUnit toRelease = new AethericEnergyUnit(this.aetherIn);
                toRelease.setAmount(remainder);
                this.aetherRelease.merge(toRelease);
            }
        }
        AethericEnergyUnit returnedAether = new AethericEnergyUnit(this.aetherIn);
        if (this.lastManipulatorTick != tick) return returnedAether;
        InterDimCoords sinkCoords = sinkHandler.getInterDimCoords();

        returnedAether.setAmount(this.aetherIn.getAmount() / this.aetherSinks.numConnections());
        if (this.handleSinkCollision(returnedAether, sinkCoords)) return returnedAether;
        this.handleLoss(dist, returnedAether);

        return returnedAether;
    }
}
