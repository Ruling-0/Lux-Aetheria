package com.ruling_0.luxaetheria.api.aether.handlers;

import java.util.HashSet;
import java.util.Iterator;

import javax.annotation.Nonnull;

import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherRelay;
import com.ruling_0.luxaetheria.api.aether.connections.AetherSinkArray;
import com.ruling_0.luxaetheria.api.aether.connections.ImmutableSinkConnection;
import com.ruling_0.luxaetheria.api.aether.connections.SinkConnection;
import com.ruling_0.luxaetheria.api.utils.IWDMLAProvider;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;
import com.ruling_0.luxaetheria.utils.LAUtils;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraftforge.common.DimensionManager;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class SimpleRelayHandler implements IRelayHandler, IReleaserHandler, IWDMLAProvider {

    public final AethericEnergyUnit aetherIn = new AethericEnergyUnit();
    public final AethericEnergyUnit aetherOut = new AethericEnergyUnit();
    public final AethericEnergyUnit aetherRelease = new AethericEnergyUnit();
    /**
     * This is used so the aether values are available to WDMLA.
     * Triggers an aether reset on the next {@link #getAetherFromSource(IAetherRelay, long)} call.
     */
    public boolean doResetAether = false;

    protected final int maxAetherSinks;
    protected final HashSet<AethericEnergyUnit.AEUID> encounteredIDs = new HashSet<>();
    protected final Object2DoubleOpenHashMap<IAetherRelay> aetherSources = new Object2DoubleOpenHashMap<>();
    protected final AetherSinkArray aetherSinks;
    protected long lastSourceTick = -1L;
    protected long lastSinkTick = -1L;
    protected long lastManipulatorTick = -1L;
    protected int validSinks = 0;
    protected IAetherManipulator manipulator = null;
    protected InterDimCoords manipCoords = null;
    protected boolean wasManipulated = false;

    private final IAetherRelay owner;

    public SimpleRelayHandler(int maxAetherSinks, TileEntity te) {
        this.maxAetherSinks = maxAetherSinks;
        this.owner = (IAetherRelay) te;
        this.aetherSinks = new AetherSinkArray(this.maxAetherSinks, this);
    }

    @Override
    public boolean addAetherSink(@Nonnull IAetherRelay sink) {
        if (!this.aetherSinks.add(sink)) return false;
        this.validSinks++;
        this.markForUpdate();
        return true;
    }

    @Override
    public boolean removeAetherSink(@Nonnull IAetherRelay sink) {
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
    public Vector3fc getSinkCollisionCoords(InterDimCoords coords) {
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
    public AethericEnergyUnit getAetherIn() { return new AethericEnergyUnit(this.aetherIn); }

    @Override
    public AethericEnergyUnit getAetherOut() { return new AethericEnergyUnit(this.aetherOut); }

    @Override
    public double getMaxSinkDistance() { return this.aetherSinks.getMaxSinkDistance(); }

    /**
     * Returns true if the manipulator was able to manipulate the aether.
     * Sets the last manipulator tick if true or if the manipulator is inactive.
     */
    protected boolean handleManipulator(long tick) {
        boolean didChange = false;

        if (tick == lastManipulatorTick) return false;
        if (this.manipulator == null) {
            if (this.manipCoords != null) {
                World world = this.manipCoords.getWorld();
                TileEntity te = world.getTileEntity(this.manipCoords.x, this.manipCoords.y, this.manipCoords.z);
                if (te instanceof IAetherManipulator manip) {
                    this.manipulator = manip;
                    manip.bindRelay(this.getInterDimCoords());
                }
            }
            this.lastManipulatorTick = tick;
            return false;
        }

        if (this.wasManipulated) didChange = true;
        this.wasManipulated = false;

        if (!manipulator.isActive(this.owner)) {
            this.lastManipulatorTick = tick;
            if (didChange) this.markForUpdate();
            return false;
        }

        if (manipulator.manipulate(this.aetherIn)) {
            this.lastManipulatorTick = tick;
            this.wasManipulated = true;
            if (!didChange) this.markForUpdate();
            return true;
        }

        return false;
    }

    @Override
    public boolean getAetherFromSource(@Nonnull IAetherRelay source, long tick) {
        if (tick != this.lastSourceTick) {
            this.lastSourceTick = tick;
            this.aetherIn.reset();
            this.aetherRelease.reset();
            this.encounteredIDs.clear();
        }
        IRelayHandler sourceHandler = source.getAetherHandler();
        if (!this.aetherSources.containsKey(source)) {
            this.aetherSources.put(source, this.getInterDimCoords().distance(sourceHandler.getInterDimCoords()));
        }
        double dist = this.aetherSources.getDouble(source);
        AethericEnergyUnit incoming = sourceHandler.getAetherForSink(tick, this, dist);
        if (this.encounteredIDs.add(incoming.getID())) {
            this.aetherIn.merge(incoming);
            if (this.lastManipulatorTick == tick) {
                if (this.aetherSinks.isEmpty()) this.aetherRelease.merge(incoming);
                return true;
            }
            if (!this.handleManipulator(tick)) {
                this.aetherRelease.merge(incoming);
            }
            else {
                this.aetherRelease.reset();
                if (this.aetherSinks.isEmpty()) this.aetherRelease.merge(this.aetherIn);
            }
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
        final Vector3fc oldCoords = this.aetherSinks.getColCoords(sinkCoords);
        if (mop != null) {
            final Vector3fc hit = LAUtils.vec3ToVector3f(mop.hitVec);
            if (!hit.equals(oldCoords)) {
                if (oldCoords == null || !oldCoords.equals(sinkCoords.getVec3fc()))
                    this.validSinks--;
                this.aetherSinks.setColCoords(sinkCoords, hit);
                this.markForUpdate();
            }
            this.aetherRelease.merge(returnedAether);
            returnedAether.setAmount(0L);
            return true;
        }
        if (oldCoords == null || !oldCoords.equals(sinkCoords.getVec3fc())) {
            this.aetherSinks.setColCoords(sinkCoords, sinkCoords.getVec3fc());
            this.validSinks++;
            this.markForUpdate();
        }
        return false;
    }

    @Nonnull
    @Override
    public AethericEnergyUnit getAetherForSink(long tick, @Nonnull IRelayHandler sinkHandler, double dist) {
        if (tick != this.lastSinkTick) {
            this.lastSinkTick = tick;
            this.aetherOut.reset();
        }
        AethericEnergyUnit returnedAether = new AethericEnergyUnit(this.aetherIn);
        if (!(this.lastManipulatorTick == tick)) return returnedAether;
        InterDimCoords sinkCoords = sinkHandler.getInterDimCoords();

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
        for (IAetherRelay source : this.aetherSources.keySet()) {
            source.getAetherHandler().removeAetherSink(this.owner);
        }
    }

    @Override
    public void setManipulator(IAetherManipulator manipulator) {
        this.manipulator = manipulator;
        this.manipCoords = manipulator.getInterDimCoords();
    }

    @Override
    public IAetherManipulator getManipulator() {
        if (this.manipulator == null && this.manipCoords != null) {
            World world = this.manipCoords.getWorld();
            TileEntity te = world.getTileEntity(this.manipCoords.x, this.manipCoords.y, this.manipCoords.z);
            if (te instanceof IAetherManipulator manip) this.manipulator = manip;
        }
        return this.manipulator;
    }

    @Override
    public boolean wasManipulated() {
        return this.wasManipulated;
    }

    protected void clearSinks() {
        this.aetherSinks.clear();
    }

    @Override
    public void markForUpdate() {
        final InterDimCoords coords = this.getInterDimCoords();
        final World world = coords.getWorld();
        world.markBlockForUpdate(coords.x, coords.y, coords.z);
        Iterator<SinkConnection> sinkIter = this.aetherSinks.iterator();
        while (sinkIter.hasNext()) {
            SinkConnection sinkConn = sinkIter.next();
            sinkConn.getSink().getAetherHandler().markForUpdate();
        }
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
            Vector3fc colCoords = sinkConn.colCoords;
            nbtAetherSink.setFloat("cx", colCoords.x());
            nbtAetherSink.setFloat("cy", colCoords.y());
            nbtAetherSink.setFloat("cz", colCoords.z());
            nbtAetherSink.setByte("idx", (byte) i);
            nbtAetherSinks.appendTag(nbtAetherSink);
        }
        compound.setTag("aetherSinks", nbtAetherSinks);
        compound.setBoolean("wasManipulated", this.wasManipulated);
        if (this.manipCoords != null) {
            InterDimCoords manipCoords = this.manipCoords;
            compound.setInteger("manipX", manipCoords.getX());
            compound.setInteger("manipY", manipCoords.getY());
            compound.setInteger("manipZ", manipCoords.getZ());
            compound.setInteger("manipDim", manipCoords.getDimID());
        }
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
            final Vector3f colCoords = new Vector3f((float) cx, (float) cy, (float) cz);
            final double dist = this.getInterDimCoords().distance(coords);
            this.aetherSinks.add(nbtAetherSink.getByte("idx"), coords, null, colCoords, dist);

            if (coords.getVec3fc().equals(colCoords)) validSinks++;
            else validSinks--;
        }
        this.wasManipulated = compound.getBoolean("wasManipulated");
        if (compound.hasKey("manipX")) {
            final int x = compound.getInteger("manipX");
            final int y = compound.getInteger("manipY");
            final int z = compound.getInteger("manipZ");
            final int dim = compound.getInteger("manipDim");
            this.manipCoords = new InterDimCoords(x, y, z, dim);
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
