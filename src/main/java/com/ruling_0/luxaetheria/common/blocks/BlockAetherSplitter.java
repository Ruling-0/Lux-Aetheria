package com.ruling_0.luxaetheria.common.blocks;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.ruling_0.luxaetheria.client.model.ModelAetherRelay;
import com.ruling_0.luxaetheria.common.tileentities.TileEntityAetherSplitter;

public class BlockAetherSplitter extends BlockAetherRelay {

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityAetherSplitter();
    }

    @Override
    protected ModelAetherRelay.RelayBakeData.RelayType getRelayType() {
        return ModelAetherRelay.RelayBakeData.RelayType.SPLITTER;
    }

    @Override
    public int colorMultiplier(IBlockAccess world, int x, int y, int z, int tintIndex) {
        if (tintIndex >= 0 && tintIndex < TileEntityAetherSplitter.MAX_OUTPUTS) return 0xCD7F32;
        return -1;
    }
}
