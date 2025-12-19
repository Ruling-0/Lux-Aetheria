package com.ruling_0.luxaetheria.common.aether;

public class AethericEnergyUnit {
    public double aspectRed = 1.0D;
    public double aspectGreen = 1.0D;
    public double aspectBlue = 1.0D;
    public long amount = 0L;
    public static final double SQRT3 = Math.sqrt(3.0D);

    public AethericEnergyUnit(long amount) {
        this.amount = amount;
    }

    public void merge(AethericEnergyUnit incoming) {
        double propIncoming = (double) incoming.amount / this.amount;
        double propCurrent = 1.0D - propIncoming;
        this.aspectRed = propIncoming * incoming.aspectRed + propCurrent * this.aspectRed;
        this.aspectGreen = propIncoming * incoming.aspectGreen + propCurrent * this.aspectGreen;
        this.aspectBlue = propIncoming * incoming.aspectBlue + propCurrent * this.aspectBlue;
        this.amount += incoming.amount;
    }

    public void split(AethericEnergyUnit outgoing) {
        this.amount -= outgoing.amount;
        double propOutgoing = (double) outgoing.amount / this.amount;
        double propCurrent = 1.0D - propOutgoing;
        this.aspectRed = (this.aspectRed - propOutgoing * outgoing.aspectRed) / propCurrent;
        this.aspectGreen = (this.aspectGreen - propOutgoing * outgoing.aspectGreen) / propCurrent;
        this.aspectBlue = (this.aspectBlue - propOutgoing * outgoing.aspectBlue) / propCurrent;
    }

    public double getMagnitude() {
        return Math.sqrt(this.aspectRed * this.aspectRed + this.aspectGreen * this.aspectGreen + this.aspectBlue * this.aspectBlue) / SQRT3;
    }

    public long getEquilibriumAmount() {
        return (long) Math.cbrt(this.amount);
    }
}
