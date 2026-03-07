package com.ruling_0.luxaetheria.common.blocks;

import net.minecraft.block.BlockFurnace;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.ruling_0.luxaetheria.common.tileentities.TileEntityAethericFurnace;

public class BlockAethericFurnace extends BlockFurnace {

    protected boolean isActive;

    public BlockAethericFurnace(boolean isActive) {
        super(isActive);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityAethericFurnace();
    }
}
