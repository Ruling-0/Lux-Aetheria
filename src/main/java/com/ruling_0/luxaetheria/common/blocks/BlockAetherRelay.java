package com.ruling_0.luxaetheria.common.blocks;

import com.ruling_0.luxaetheria.common.tileentities.TileEntityAetherRelay;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockAetherRelay extends BlockContainer {

    public BlockAetherRelay() {
        super(Material.glass);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityAetherRelay();
    }
}
