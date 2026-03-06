package com.ruling_0.luxaetheria.common.tileentities;

import net.minecraft.world.World;

import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityAetherRelay extends BaseAetherRelay {

    public TileEntityAetherRelay() {
        this(null, ForgeDirection.DOWN);
    }

    public TileEntityAetherRelay(World world, ForgeDirection dir) {
        super(world, dir);
    }
}
