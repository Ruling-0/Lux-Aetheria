package com.ruling_0.luxaetheria.common.aether;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.gtnewhorizon.gtnhlib.datastructs.space.ArrayProximityMap4D;
import com.gtnewhorizon.gtnhlib.datastructs.space.VolumeShape;

import com.gtnewhorizon.gtnhlib.util.CoordinatePacker;
import com.ruling_0.luxaetheria.api.AetherConstants;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.gameevent.TickEvent;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import org.jetbrains.annotations.NotNull;

public class AetherManager {

    private static ArrayProximityMap4D<IAetherCollector> AetherCollectors;
    private static ArrayProximityMap4D<IAetherReleaser> AetherReleasers;
    private static HashSet<IAetherManipulator> AetherRootCollectors;
    private static Deque<IAetherManipulator> AetherSearchQueue;
    private static HashSet<IAetherManipulator> AetherUpdateQueue;
    private long serverTick = 0L;

    public AetherManager() {
        AetherCollectors = new ArrayProximityMap4D<>(VolumeShape.SPHERE);
        AetherReleasers = new ArrayProximityMap4D<>(VolumeShape.SPHERE);
        AetherRootCollectors = new HashSet<>();
        AetherSearchQueue = new ArrayDeque<>();
        AetherUpdateQueue = new HashSet<>();
    }

    public void enableCollector(@NotNull IAetherCollector collector, int dim, int x, int y, int z) {
        if (collector.isRemote()) return;
        // TODO: PR to GTNHLib that returns count from forEachInRange
        AtomicInteger count = new AtomicInteger();
        AtomicLong totalCollection = new AtomicLong();
        AetherCollectors.forEachInRange(dim, x, y, z, c -> {
            count.getAndIncrement();
            c.addCollectorInRange(collector);
            totalCollection.getAndAdd(c.getAetherCollectionAmount());
        });
        collector.bulkUpdateCollectors(-totalCollection.get(), count.get());
        AetherCollectors.put(collector, dim, x, y, z, collector.getCollectorRange());
        AetherReleasers.forEachInRange(dim, x, y, z, collector::addReleaserInRange);
        if (collector instanceof IAetherManipulator manipulator) AetherRootCollectors.add(manipulator);
    }

    public void disableCollector(@NotNull IAetherCollector collector, int dim, int x, int y, int z) {
        if (collector.isRemote()) return;
        AetherCollectors.remove(dim, x, y, z);
        AetherCollectors.forEachInRange(dim, x, y, z, c -> c.removeCollectorInRange(collector));
        if (collector instanceof IAetherManipulator manipulator) AetherRootCollectors.remove(manipulator);
    }

    public void enableReleaser(@NotNull IAetherReleaser releaser, int dim, int x, int y, int z) {
        if (releaser.isRemote()) return;
        AetherReleasers.put(releaser, dim, x, y, z, AetherConstants.MAX_COLLECTOR_RANGE);
        AetherCollectors.forEachInRange(dim, x, y, z, c -> c.addReleaserInRange(releaser));
    }

    public void disableReleaser(@NotNull IAetherReleaser releaser, int dim, int x, int y, int z) {
        if (releaser.isRemote()) return;
        AetherReleasers.remove(dim, x, y, z);
        AetherCollectors.forEachInRange(dim, x, y, z, c -> c.removeReleaserInRange(releaser));
    }

    public void onServerTick(TickEvent.ServerTickEvent event) {
        /*
         * Starting with known collectors, calculate Aether propagation using BFS.
         * Loops are handled in manipulators' getAetherFromSource
         */
        AetherSearchQueue.clear();
        AetherSearchQueue.addAll(AetherRootCollectors);
        this.serverTick++;
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        while (!AetherSearchQueue.isEmpty()) {
            IAetherManipulator curr = AetherSearchQueue.pop();
            Iterator<Map.Entry<Long, Pair<IAetherManipulator, Integer>>> iterSinks = curr.getAetherSinksIter();
            while (iterSinks.hasNext()) {
                // TODO: listen for block updates and only check collision in range
                Map.Entry<Long, Pair<IAetherManipulator, Integer>> entry = iterSinks.next();
                if (entry.getValue().left() == null) {
                    BlockPos pos = new BlockPos();
                    CoordinatePacker.unpack(entry.getKey(), pos);
                    World dim = server.worldServerForDimension(entry.getValue().right());
                    TileEntity te = dim.getTileEntity(pos.x, pos.y, pos.z);
                    if (te instanceof IAetherManipulator sink) {
                        entry.setValue(Pair.of(sink, entry.getValue().right()));
                    } else {
                        iterSinks.remove();
                        continue;
                    }
                }
                IAetherManipulator next = entry.getValue().left();
                if (curr.validateSink(next)) {
                    AetherSearchQueue.push(next);
                }
            }
            iterSinks = curr.getAetherSinksIter();
            while (iterSinks.hasNext()) {
                IAetherManipulator next = iterSinks.next().getValue().left();
                boolean shouldPropagate = next.getAetherFromSource(curr, this.serverTick);
                if (shouldPropagate) AetherSearchQueue.push(next);
            }
            if (curr.isUpdateable()) AetherUpdateQueue.add(curr);
        }
        for (IAetherManipulator curr : AetherUpdateQueue) {
            curr.updateAether();
        }
        AetherUpdateQueue.clear();
    }

    public void reset() {
        AetherCollectors = new ArrayProximityMap4D<>(VolumeShape.SPHERE);
        AetherReleasers = new ArrayProximityMap4D<>(VolumeShape.SPHERE);
        AetherRootCollectors = new HashSet<>();
        AetherSearchQueue = new ArrayDeque<>();
        AetherUpdateQueue = new HashSet<>();
    }
}
