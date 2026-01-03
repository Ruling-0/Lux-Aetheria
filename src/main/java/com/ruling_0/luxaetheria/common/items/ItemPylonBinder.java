package com.ruling_0.luxaetheria.common.items;

import java.util.Iterator;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.ruling_0.luxaetheria.utils.LAUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.ruling_0.luxaetheria.common.aether.IAetherManipulator;

public class ItemPylonBinder extends Item {

    private IAetherManipulator boundManipulator = null;

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int ordSide,
        float hitx, float hity, float hitz) {
        if (world.isRemote) return true;
        TileEntity te = world.getTileEntity(x, y, z);
        if (te == null) return false;
        if (te instanceof IAetherManipulator aetherManipulator) {
            if (boundManipulator != null) {
                if (boundManipulator.equals(aetherManipulator)) return true;
                boolean success;
                BlockPos targetPos = aetherManipulator.getPosBlockPos();
                if (boundManipulator.hasOutput(targetPos.asLong())) {
                    success = boundManipulator.removeAetherSink(aetherManipulator);
                    if (!success) {
                        player.addChatMessage(new ChatComponentTranslation("LA.binder.fail.remove_sink"));
                        return true;
                    }
                    boundManipulator = null;
                    player.addChatMessage(new ChatComponentTranslation("LA.binder.unbound"));
                } else {
                    if (!LAUtils.checkRayCollision(world, boundManipulator, aetherManipulator, true)) {
                        player.addChatMessage(new ChatComponentTranslation("LA.binder.fail.blocked"));
                        return true;
                    }
                    success = boundManipulator.addAetherSink(aetherManipulator);
                    if (!success) {
                        player.addChatMessage(new ChatComponentTranslation("LA.binder.fail.add_sink"));
                        return true;
                    }
                    boundManipulator = null;
                    player.addChatMessage(new ChatComponentTranslation("LA.binder.bound"));
                }
            } else {
                boundManipulator = aetherManipulator;
            }
            return true;
        }
        return false;
    }
}
