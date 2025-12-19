package com.ruling_0.luxaetheria.common.blocks;

import com.ruling_0.luxaetheria.common.tileentities.TileEntityCollectorPylon;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockCollectorPylon extends BlockContainer {
    public BlockCollectorPylon() {
        super(Material.glass);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        //TODO: get range/collection using meta as tier
        return new TileEntityCollectorPylon(12, 3);
    }
}
