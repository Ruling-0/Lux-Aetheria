package com.ruling_0.luxaetheria.common.items;

import java.util.Iterator;

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
                boolean success;
                boolean isBound = false;
                Iterator<IAetherManipulator> iter = boundManipulator.getAetherSinksIter();
                while (iter.hasNext()) {
                    if (iter.next()
                        .equals(aetherManipulator)) {
                        isBound = true;
                        break;
                    }
                }
                if (isBound) {
                    success = aetherManipulator.removeAetherSource(boundManipulator);
                    if (!success) {
                        player.addChatMessage(new ChatComponentTranslation("LA.binder.fail.remove_source"));
                        return true;
                    }
                    success = boundManipulator.removeAetherSink(aetherManipulator);
                    if (!success) {
                        player.addChatMessage(new ChatComponentTranslation("LA.binder.fail.remove_sink"));
                        aetherManipulator.addAetherSource(boundManipulator);
                        return true;
                    }
                    boundManipulator = null;
                    player.addChatMessage(new ChatComponentTranslation("LA.binder.unbound"));
                } else {
                    Vec3 boundVec = boundManipulator.getPosVec3();
                    Vec3 targetVec = Vec3.createVectorHelper(x, y, z);
                    boolean pathClear = LAUtils.checkRayCollision(world, boundVec, targetVec, true);
                    if (!pathClear) {
                        player.addChatMessage(new ChatComponentTranslation("LA.binder.fail.blocked"));
                        return true;
                    }
                    success = aetherManipulator.addAetherSource(boundManipulator);
                    if (!success) {
                        player.addChatMessage(new ChatComponentTranslation("LA.binder.fail.add_source"));
                        return true;
                    }
                    success = boundManipulator.addAetherSink(aetherManipulator);
                    if (!success) {
                        player.addChatMessage(new ChatComponentTranslation("LA.binder.fail.add_sink"));
                        aetherManipulator.removeAetherSource(boundManipulator);
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
