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
        AethericEnergyUnit returnedAether = new AethericEnergyUnit(this.aetherIn);
        final int connections = this.aetherSinks.numConnections();
        if (this.lastManipulatorTick != tick || connections == 0) return returnedAether;
        if (tick != this.lastSinkTick) {
            this.lastSinkTick = tick;
            long remainder = this.aetherIn.getAmount() % connections;
            if (remainder > 0) {
                AethericEnergyUnit toRelease = new AethericEnergyUnit(this.aetherIn);
                toRelease.setAmount(remainder);
                this.aetherRelease.merge(toRelease);
            }
        }
        InterDimCoords sinkCoords = sinkHandler.getInterDimCoords();

        returnedAether.setAmount(this.aetherIn.getAmount() / connections);
        if (this.handleSinkCollision(returnedAether, sinkCoords)) return returnedAether;
        this.handleLoss(dist, returnedAether);

        return returnedAether;
    }
}
