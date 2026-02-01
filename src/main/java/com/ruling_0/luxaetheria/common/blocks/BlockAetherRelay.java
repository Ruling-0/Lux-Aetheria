package com.ruling_0.luxaetheria.common.blocks;

import static net.minecraftforge.common.util.ForgeDirection.DOWN;
import static net.minecraftforge.common.util.ForgeDirection.EAST;
import static net.minecraftforge.common.util.ForgeDirection.NORTH;
import static net.minecraftforge.common.util.ForgeDirection.SOUTH;
import static net.minecraftforge.common.util.ForgeDirection.UP;
import static net.minecraftforge.common.util.ForgeDirection.WEST;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.api.IModelProvider;
import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.gtnewhorizon.gtnhlib.client.model.loading.ResourceLoc;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.JSONModel;
import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.api.aether.handlers.IAetherHandler;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;
import com.ruling_0.luxaetheria.client.model.ModelAetherRelay;
import com.ruling_0.luxaetheria.common.tileentities.TileEntityAetherRelay;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

public class BlockAetherRelay extends BlockContainer implements IModelProvider {

    public BlockAetherRelay() {
        super(Material.glass);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityAetherRelay();
    }

    @Override
    public int getRenderType() { return ModelISBRH.JSON_ISBRH_ID; }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() { return false; }

    @Override
    public int getRenderBlockPass() { return 1; }

    @Override
    public boolean canRenderInPass(int pass) {
        return true;
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        return (dir == DOWN && world.isSideSolid(x, y + 1, z, DOWN)) ||
            (dir == UP && world.isSideSolid(x, y - 1, z, UP)) ||
            (dir == NORTH && world.isSideSolid(x, y, z + 1, NORTH)) ||
            (dir == SOUTH && world.isSideSolid(x, y, z - 1, SOUTH)) ||
            (dir == WEST && world.isSideSolid(x + 1, y, z, WEST)) ||
            (dir == EAST && world.isSideSolid(x - 1, y, z, EAST));
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return world.isSideSolid(x - 1, y, z, EAST) ||
            world.isSideSolid(x + 1, y, z, WEST) ||
            world.isSideSolid(x, y, z - 1, SOUTH) ||
            world.isSideSolid(x, y, z + 1, NORTH) ||
            world.isSideSolid(x, y - 1, z, UP) ||
            world.isSideSolid(x, y + 1, z, DOWN);
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ,
                             int metadata) {
        return ForgeDirection.getOrientation(side).getOpposite().ordinal();
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
        final int meta = world.getBlockMetadata(x, y, z);
        final ForgeDirection curDir = ForgeDirection.getOrientation(meta).getOpposite();
        if (world.isSideSolid(x - curDir.offsetX, curDir.offsetY, curDir.offsetZ, curDir)) return;
        if (this.checkAndBreakInvalid(world, x, y, z)) {

            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                if (world.isSideSolid(x - dir.offsetX, y - dir.offsetY, z - dir.offsetZ, dir)) {
                    world.setBlockMetadataWithNotify(x, y, z, dir.getOpposite().ordinal(), 3);
                    return;
                }
            }

            this.dropBlockAsItem(world, x, y, z, meta, 0);
            world.setBlockToAir(x, y, z);
        }
    }

    private boolean checkAndBreakInvalid(World world, int x, int y, int z) {
        if (!this.canPlaceBlockAt(world, x, y, z)) {
            this.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
            return false;
        }
        return true;
    }

    @Override
    public BakedModel getModel(@Nullable IBlockAccess world, Block block, int meta, int x, int y, int z) {
        Vector3i pos = new Vector3i(x, y, z);
        ArrayList<Vector3i> targets = new ArrayList<>(4);
        if (world != null) {
            final IAetherManipulator te = (IAetherManipulator) world.getTileEntity(x, y, z);
            final IAetherHandler handler = te.getAetherHandler();
            final InterDimCoords coords = handler.getInterDimCoords();
            final var iter = handler.getAetherSinksIter();
            while (iter.hasNext()) {
                var sink = iter.next();
                InterDimCoords sinkCoords = sink.getKey();
                if (sinkCoords.getDimID() != coords.getDimID()) continue;
                targets.add(new Vector3i(sinkCoords.getX(), sinkCoords.getY(), sinkCoords.getZ()));
            }
        }
        final var data = new ModelAetherRelay.RelayBakeData(pos, targets.toArray(new Vector3i[0]), meta);
        final JSONModel jsonModel = ModelRegistry
            .getJSONModel(new ResourceLoc.ModelLoc("luxaetheria", "blocks/aether_relay"));
        final ModelAetherRelay model = new ModelAetherRelay(jsonModel);
        return model.bake(data);
    }
}
