package com.ruling_0.luxaetheria.common.items;

import com.ruling_0.luxaetheria.common.aether.IAetherManipulator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

public class ItemPylonBinder extends Item {
    private IAetherManipulator boundManipulator = null;

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int ordSide, float hitx, float hity, float hitz) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te == null) return false;
        if (te instanceof IAetherManipulator aetherManipulator) {
            if (boundManipulator != null) {
                boolean success;
                if (boundManipulator.getSink() == aetherManipulator) {
                    success = aetherManipulator.removeSource(boundManipulator);
                    if (!success) {
                        player.addChatMessage(new ChatComponentTranslation("LA.binder.fail.remove_source"));
                        return true;
                    }
                    success = boundManipulator.removeSink(aetherManipulator);
                    if (!success) {
                        player.addChatMessage(new ChatComponentTranslation("LA.binder.fail.remove_sink"));
                        aetherManipulator.addSource(boundManipulator);
                        return true;
                    }
                    boundManipulator = null;
                    player.addChatMessage(new ChatComponentTranslation("LA.binder.unbound"));
                }
                else {
                    success = aetherManipulator.addSource(boundManipulator);
                    if (!success) {
                        player.addChatMessage(new ChatComponentTranslation("LA.binder.fail.add_source"));
                        return true;
                    }
                    success = boundManipulator.addSink(aetherManipulator);
                    if (!success) {
                        player.addChatMessage(new ChatComponentTranslation("LA.binder.fail.add_sink"));
                        aetherManipulator.removeSource(boundManipulator);
                        return true;
                    }
                    boundManipulator = null;
                    player.addChatMessage(new ChatComponentTranslation("LA.binder.bound"));
                }
            }
            else {
                boundManipulator = aetherManipulator;
            }
            return true;
        }
        return false;
    }
}
