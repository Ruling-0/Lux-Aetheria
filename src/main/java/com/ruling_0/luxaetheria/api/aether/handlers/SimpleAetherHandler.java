package com.ruling_0.luxaetheria.api.aether.handlers;

import java.util.HashSet;
import java.util.Iterator;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.api.aether.connections.AetherSinkArray;
import com.ruling_0.luxaetheria.api.aether.connections.ImmutableSinkConnection;
import com.ruling_0.luxaetheria.api.aether.connections.SinkConnection;
import com.ruling_0.luxaetheria.api.utils.IWDMLAProvider;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;
import com.ruling_0.luxaetheria.utils.LAUtils;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.jetbrains.annotations.NotNull;

public class SimpleAetherHandler implements IAetherHandler, IReleaserHandler, IWDMLAProvider {

    public final AethericEnergyUnit aetherIn = new AethericEnergyUnit();
    public final AethericEnergyUnit aetherOut = new AethericEnergyUnit();
    public final AethericEnergyUnit aetherRelease = new AethericEnergyUnit();
    /**
     * This is used so the aether values are available to WDMLA.
     * Triggers an aether reset on the next {@link #getAetherFromSource(IAetherManipulator, long)} call.
     */
    public boolean doResetAether = false;

    protected final int maxAetherSinks;
    protected final HashSet<AethericEnergyUnit.AEUID> encounteredIDs = new HashSet<>();
    protected final Object2DoubleOpenHashMap<IAetherManipulator> aetherSources = new Object2DoubleOpenHashMap<>();
    protected final AetherSinkArray aetherSinks;
    protected long lastSourceTick = -1L;
    protected long lastSinkTick = -1L;
    protected int validSinks = 0;

    private final IAetherManipulator owner;

    public SimpleAetherHandler(int maxAetherSinks, TileEntity te) {
        this.maxAetherSinks = maxAetherSinks;
        this.owner = (IAetherManipulator) te;
        this.aetherSinks = new AetherSinkArray(this.maxAetherSinks, this);
    }

    @Override
    public boolean addAetherSink(@Nonnull IAetherManipulator sink) {
        if (!this.aetherSinks.add(sink)) return false;
        this.validSinks++;
        this.markForUpdate();
        return true;
    }

    @Override
    public boolean removeAetherSink(@Nonnull IAetherManipulator sink) {
        SinkConnection removed = this.aetherSinks.remove(sink.getInterDimCoords());
        if (removed == null) return false;
        this.aetherOut.split(removed.aeu);
        LuxAetheria.proxy.aetherManager.addOrphanedManipulator(sink);
        this.markForUpdate();
        this.validSinks--;
        return true;
    }

    @Override
    public Iterator<ImmutableSinkConnection> getAetherSinksIter() { return this.aetherSinks.immutableIter(); }

    @Override
    public Iterator<SinkConnection> getMutableSinksIter() { return this.aetherSinks.iterator(); }

    @Override
    public Vec3 getSinkCollisionCoords(InterDimCoords coords) {
        return this.aetherSinks.getColCoords(coords);
    }

    @Override
    public boolean hasSink(InterDimCoords coords) {
        return this.aetherSinks.hasSink(coords);
    }

    @Override
    public int getOutputIndex(InterDimCoords coords) {
        return this.aetherSinks.getOutputIndex(coords);
    }

    @Nonnull
    @Override
    public Vec3 getPosVec3() { return this.getInterDimCoords().getVec3(); }

    @Nonnull
    @Override
    public InterDimCoords getInterDimCoords() { return this.owner.getInterDimCoords(); }

    @Override
    public AethericEnergyUnit getAetherOut() { return new AethericEnergyUnit(this.aetherOut); }

    @Override
    public double getMaxSinkDistance() { return this.aetherSinks.getMaxSinkDistance(); }

    @Override
    public boolean getAetherFromSource(@Nonnull IAetherManipulator source, long tick) {
        if (tick != this.lastSourceTick) {
            this.lastSourceTick = tick;
            this.aetherIn.reset();
            this.aetherRelease.reset();
            this.encounteredIDs.clear();
        }
        IAetherHandler sourceHandler = source.getAetherHandler();
        if (!this.aetherSources.containsKey(source)) {
            this.aetherSources.put(source, this.getInterDimCoords().distance(sourceHandler.getInterDimCoords()));
        }
        double dist = this.aetherSources.getDouble(source);
        AethericEnergyUnit incoming = sourceHandler.getAetherForSink(tick, this, dist);
        if (this.encounteredIDs.add(incoming.getID())) {
            this.aetherIn.merge(incoming);
            if (this.aetherSinks.isEmpty()) this.aetherRelease.merge(incoming);
            return true;
        }
        else {
            this.aetherRelease.merge(incoming);
        }
        return false;
    }

    protected boolean handleSinkCollision(AethericEnergyUnit returnedAether, InterDimCoords sinkCoords) {
        this.aetherOut.merge(returnedAether);
        this.aetherSinks.setAeu(sinkCoords, returnedAether);

        MovingObjectPosition mop = LAUtils.getRayCollision(this.getInterDimCoords().getWorld(), this.getPosVec3(),
            sinkCoords.getVec3(), true);
        Vec3 oldCoords = this.aetherSinks.getColCoords(sinkCoords);
        if (mop != null) {
            if (!mop.hitVec.equals(oldCoords)) {
                if (oldCoords == null || !LAUtils.vec3Equals(oldCoords, sinkCoords.getVec3()))
                    this.validSinks--;
                this.aetherSinks.setColCoords(sinkCoords, mop.hitVec);
                this.markForUpdate();
            }
            this.aetherRelease.merge(returnedAether);
            returnedAether.setAmount(0L);
            return true;
        }
        if (oldCoords == null || !LAUtils.vec3Equals(oldCoords, sinkCoords.getVec3())) {
            this.aetherSinks.setColCoords(sinkCoords, sinkCoords.getVec3());
            this.validSinks++;
            this.markForUpdate();
        }
        return false;
    }

    @Nonnull
    @Override
    public AethericEnergyUnit getAetherForSink(long tick, @Nonnull IAetherHandler sinkHandler, double dist) {
        if (tick != this.lastSinkTick) {
            this.lastSinkTick = tick;
            this.aetherOut.reset();
        }
        InterDimCoords sinkCoords = sinkHandler.getInterDimCoords();
        AethericEnergyUnit returnedAether = new AethericEnergyUnit(this.aetherIn);

        returnedAether.setAmount(this.aetherIn.getAmount() / this.aetherSinks.size());
        if (this.handleSinkCollision(returnedAether, sinkCoords)) return returnedAether;

        long loss = returnedAether.calculateLoss(dist);
        returnedAether.setAmount(Math.max(0L, returnedAether.getAmount() - loss));

        AethericEnergyUnit toRelease = new AethericEnergyUnit(returnedAether);
        toRelease.setAmount(loss);
        this.aetherRelease.merge(toRelease);

        return returnedAether;
    }

    @Override
    public AethericEnergyUnit getAetherRelease() { return new AethericEnergyUnit(this.aetherRelease); }

    @Override
    public boolean isUpdatable() { return false; }

    @Override
    public void updateAether() {}

    @Override
    public void resetAether() {
        this.aetherIn.reset();
        this.aetherOut.reset();
        this.aetherRelease.reset();
        this.encounteredIDs.clear();
    }

    @Override
    public void disconnectFromSources() {
        for (IAetherManipulator source : this.aetherSources.keySet()) {
            source.getAetherHandler().removeAetherSink(this.owner);
        }
    }

    protected void clearSinks() {
        this.aetherSinks.clear();
    }

    protected void markForUpdate() {
        final InterDimCoords coords = this.getInterDimCoords();
        final World world = coords.getWorld();
        world.markBlockForUpdate(coords.x, coords.y, coords.z);
    }

    @Override
    public void writeToNBT(@Nonnull NBTTagCompound compound) {
        NBTTagCompound nbtAetherIn = new NBTTagCompound();
        this.aetherIn.writeToNBT(nbtAetherIn);
        compound.setTag("aetherIn", nbtAetherIn);
        NBTTagCompound nbtAetherOut = new NBTTagCompound();
        this.aetherOut.writeToNBT(nbtAetherOut);
        compound.setTag("aetherOut", nbtAetherOut);

        NBTTagList nbtAetherSinks = new NBTTagList();
        for (int i = 0; i < this.maxAetherSinks; ++i) {
            SinkConnection sinkConn = this.aetherSinks.get(i);
            if (sinkConn == null) continue;
            NBTTagCompound nbtAetherSink = new NBTTagCompound();
            InterDimCoords coords = sinkConn.sinkCoords;
            nbtAetherSink.setInteger("x", coords.getX());
            nbtAetherSink.setInteger("y", coords.getY());
            nbtAetherSink.setInteger("z", coords.getZ());
            nbtAetherSink.setInteger("dim", coords.getDimID());
            Vec3 colCoords = sinkConn.colCoords;
            nbtAetherSink.setDouble("cx", colCoords.xCoord);
            nbtAetherSink.setDouble("cy", colCoords.yCoord);
            nbtAetherSink.setDouble("cz", colCoords.zCoord);
            nbtAetherSink.setByte("idx", (byte) i);
            nbtAetherSinks.appendTag(nbtAetherSink);
        }
        compound.setTag("aetherSinks", nbtAetherSinks);
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound compound) {
        this.aetherIn.readFromNBT(compound.getCompoundTag("aetherIn"));
        this.aetherOut.readFromNBT(compound.getCompoundTag("aetherOut"));

        NBTTagList nbtAetherSinks = compound.getTagList("aetherSinks", 10);
        this.clearSinks();
        for (int i = 0; i < nbtAetherSinks.tagCount(); ++i) {
            NBTTagCompound nbtAetherSink = nbtAetherSinks.getCompoundTagAt(i);
            if (this.aetherSinks.hasOutput(nbtAetherSink.getByte("idx"))) continue;
            final int x = nbtAetherSink.getInteger("x");
            final int y = nbtAetherSink.getInteger("y");
            final int z = nbtAetherSink.getInteger("z");
            final int dim = nbtAetherSink.getInteger("dim");
            final double cx = nbtAetherSink.getDouble("cx");
            final double cy = nbtAetherSink.getDouble("cy");
            final double cz = nbtAetherSink.getDouble("cz");

            final InterDimCoords coords = new InterDimCoords(x, y, z, dim);
            final Vec3 colCoords = Vec3.createVectorHelper(cx, cy, cz);
            final double dist = this.getInterDimCoords().distance(coords);
            this.aetherSinks.add(nbtAetherSink.getByte("idx"), coords, null, colCoords, dist);

            if (coords.getVec3().equals(colCoords)) validSinks++;
            else validSinks--;
        }
    }

    @Override
    public void writeWDMLAData(@NotNull NBTTagCompound compound) {
        NBTTagCompound nbtAetherIn = new NBTTagCompound();
        this.aetherIn.writeToNBT(nbtAetherIn);
        compound.setTag("aetherIn", nbtAetherIn);
        NBTTagCompound nbtAetherOut = new NBTTagCompound();
        this.aetherOut.writeToNBT(nbtAetherOut);
        compound.setTag("aetherOut", nbtAetherOut);
        NBTTagCompound nbtAetherRelease = new NBTTagCompound();
        this.aetherRelease.writeToNBT(nbtAetherRelease);
        compound.setTag("aetherRelease", nbtAetherRelease);
    }
}
