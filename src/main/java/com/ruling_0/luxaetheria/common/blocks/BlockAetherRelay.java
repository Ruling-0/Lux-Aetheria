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
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.gtnhlib.api.IBlockModelProvider;
import com.gtnewhorizon.gtnhlib.client.model.BakedModelQuadContext;
import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.color.IBlockColor;
import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.api.aether.IAetherRelay;
import com.ruling_0.luxaetheria.api.aether.handlers.IRelayHandler;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;
import com.ruling_0.luxaetheria.client.model.LAModelRegistry;
import com.ruling_0.luxaetheria.client.model.ModelAetherRelay;
import com.ruling_0.luxaetheria.common.tileentities.TileEntityAetherRelay;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import org.joml.Vector3i;

public class BlockAetherRelay extends BlockContainer implements IBlockModelProvider, IBlockColor {

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
    public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world, x, y, z);
        if (!this.checkAndBreakInvalid(world, x, y, z)) return;
        ForgeDirection dir = ForgeDirection.getOrientation(world.getBlockMetadata(x, y, z));
        TileEntity te = world.getTileEntity(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
        if (te instanceof IAetherManipulator manipulator) {
            manipulator.bindRelay(new InterDimCoords(x, y, z, world));
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block blockBroken, int meta) {
        final ForgeDirection curDir = ForgeDirection.getOrientation(meta);
        TileEntity te = world.getTileEntity(x + curDir.offsetX, y + curDir.offsetY, z + curDir.offsetZ);
        if (te instanceof IAetherManipulator manipulator) {
            TileEntity thisTE = world.getTileEntity(x, y, z);
            if (thisTE instanceof IAetherRelay relayTE) {
                manipulator.unbindRelay(relayTE);
            }
        }
        super.breakBlock(world, x, y, z, blockBroken, meta);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
        final int meta = world.getBlockMetadata(x, y, z);
        final ForgeDirection curDir = ForgeDirection.getOrientation(meta).getOpposite();
        if (world.isSideSolid(x - curDir.offsetX, y - curDir.offsetY, z - curDir.offsetZ, curDir)) return;
        this.checkAndBreakInvalid(world, x, y, z);
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
    public int colorMultiplier(IBlockAccess world, int x, int y, int z, int tintIndex) {
        if (tintIndex == 0) return 0xFFD700;
        return -1;
    }

    @Override
    public int colorMultiplier(ItemStack stack, int tintIndex) {
        return colorMultiplier(null, 0, 0, 0, tintIndex);
    }

    protected ModelAetherRelay.RelayBakeData.RelayType getRelayType() {
        return ModelAetherRelay.RelayBakeData.RelayType.RELAY;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BakedModel getModel(BakedModelQuadContext context) {
        int meta = 0;
        Vector3i pos = new Vector3i(0, 0, 0);
        ArrayList<Vector3i> targets = new ArrayList<>(4);
        if (context instanceof BakedModelQuadContext.World worldContext) {
            final IBlockAccess world = worldContext.getWorld();
            final int x = worldContext.getX();
            final int y = worldContext.getY();
            final int z = worldContext.getZ();
            pos.x = x;
            pos.y = y;
            pos.z = z;
            meta = world.getBlockMetadata(x, y, z);
            if (world.getTileEntity(x, y, z) instanceof IAetherRelay te) {
                final IRelayHandler handler = te.getAetherHandler();
                final InterDimCoords coords = handler.getInterDimCoords();
                final var iter = handler.getAetherSinksIter();
                while (iter.hasNext()) {
                    var sink = iter.next();
                    InterDimCoords sinkCoords = sink.getSinkCoords();
                    if (sinkCoords.getDimID() != coords.getDimID()) continue;
                    targets.add(new Vector3i(sinkCoords.getX(), sinkCoords.getY(), sinkCoords.getZ()));
                }
            }
        }
        else { // this is an item render, so show 1 arm pointing North
            targets.add(new Vector3i(0, 0, -1));
        }
        final var data = new ModelAetherRelay.RelayBakeData(
            pos, targets.toArray(new Vector3i[0]), meta, this.getRelayType());
        return LAModelRegistry.getAetherRelayModel(data);
    }
}
