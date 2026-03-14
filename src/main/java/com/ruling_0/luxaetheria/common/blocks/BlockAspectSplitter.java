package com.ruling_0.luxaetheria.common.blocks;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.ruling_0.luxaetheria.client.model.ModelAetherRelay;
import com.ruling_0.luxaetheria.common.tileentities.TileEntityAspectSplitter;

public class BlockAspectSplitter extends BlockAetherRelay {

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityAspectSplitter();
    }

    @Override
    protected ModelAetherRelay.RelayBakeData.RelayType getRelayType() {
        return ModelAetherRelay.RelayBakeData.RelayType.ASPECT_SPLITTER;
    }

    @Override
    public int colorMultiplier(IBlockAccess world, int x, int y, int z, int tintIndex) {
        return switch (tintIndex) {
            case 0 -> 0xFF0000;
            case 1 -> 0x00FF00;
            case 2 -> 0x0000FF;
            default -> -1;
        };
    }
}
