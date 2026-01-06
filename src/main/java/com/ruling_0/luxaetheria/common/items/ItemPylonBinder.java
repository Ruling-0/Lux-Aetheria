package com.ruling_0.luxaetheria.common.items;

import com.ruling_0.luxaetheria.api.aether.handlers.IAetherHandler;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;
import com.ruling_0.luxaetheria.utils.LAUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;

public class ItemPylonBinder extends Item {

    private IAetherManipulator boundManipulator = null;

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int ordSide,
        float hitx, float hity, float hitz) {
        if (world.isRemote) return true;
        TileEntity te = world.getTileEntity(x, y, z);
        if (te == null) return false;
        if (te instanceof IAetherManipulator aetherManipulator) {
            if (this.boundManipulator != null) {
                if (this.boundManipulator.equals(aetherManipulator)) return true;
                boolean success;
                IAetherHandler boundHandler = this.boundManipulator.getAetherHandler();
                InterDimCoords targetPos = aetherManipulator.getAetherHandler().getInterDimCoords();
                if (boundHandler.hasOutput(targetPos)) {
                    success = boundHandler.removeAetherSink(aetherManipulator);
                    if (!success) {
                        player.addChatMessage(new ChatComponentTranslation("LA.binder.fail.remove_sink"));
                        return true;
                    }
                    this.boundManipulator = null;
                    player.addChatMessage(new ChatComponentTranslation("LA.binder.unbound"));
                } else {
                    if (!LAUtils.checkRayCollision(world, this.boundManipulator, aetherManipulator, true)) {
                        player.addChatMessage(new ChatComponentTranslation("LA.binder.fail.blocked"));
                        return true;
                    }
                    success = boundHandler.addAetherSink(aetherManipulator);
                    if (!success) {
                        player.addChatMessage(new ChatComponentTranslation("LA.binder.fail.add_sink"));
                        return true;
                    }
                    this.boundManipulator = null;
                    player.addChatMessage(new ChatComponentTranslation("LA.binder.bound"));
                }
            } else {
                this.boundManipulator = aetherManipulator;
            }
            return true;
        }
        return false;
    }
}
