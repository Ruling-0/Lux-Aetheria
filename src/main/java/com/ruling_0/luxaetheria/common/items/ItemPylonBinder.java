package com.ruling_0.luxaetheria.common.items;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

import net.minecraftforge.common.DimensionManager;

import com.ruling_0.luxaetheria.api.aether.IAetherRelay;
import com.ruling_0.luxaetheria.api.aether.handlers.IRelayHandler;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;
import com.ruling_0.luxaetheria.utils.LAUtils;

public class ItemPylonBinder extends Item {

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int ordSide,
                             float hitx, float hity, float hitz) {
        if (world.isRemote) return true;
        IAetherRelay boundManipulator = getBoundManipulator(stack);
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof IAetherRelay aetherManipulator) {
            if (boundManipulator != null) {
                if (boundManipulator.equals(aetherManipulator)) return true;
                boolean success;
                IRelayHandler boundHandler = boundManipulator.getAetherHandler();
                InterDimCoords targetPos = aetherManipulator.getAetherHandler().getInterDimCoords();
                if (boundHandler.hasSink(targetPos)) {
                    success = boundHandler.removeAetherSink(aetherManipulator);
                    if (!success) {
                        player.addChatMessage(new ChatComponentTranslation("LA.binder.fail.remove_sink"));
                        return true;
                    }
                    setBoundManipulator(stack, null);
                    player.addChatMessage(new ChatComponentTranslation("LA.binder.unbound"));
                }
                else {
                    if (!LAUtils.checkRayCollision(world, boundManipulator, aetherManipulator, true)) {
                        player.addChatMessage(new ChatComponentTranslation("LA.binder.fail.blocked"));
                        return true;
                    }
                    success = boundHandler.addAetherSink(aetherManipulator);
                    if (!success) {
                        player.addChatMessage(new ChatComponentTranslation("LA.binder.fail.add_sink"));
                        return true;
                    }
                    setBoundManipulator(stack, null);
                    player.addChatMessage(new ChatComponentTranslation("LA.binder.bound"));
                }
            }
            else {
                setBoundManipulator(stack, aetherManipulator);
                player.addChatMessage(new ChatComponentTranslation("LA.binder.selected"));
            }
            return true;
        }
        if (player.isSneaking() && boundManipulator != null) {
            setBoundManipulator(stack, null);
            player.addChatMessage(new ChatComponentTranslation("LA.binder.clear"));
            return true;
        }
        return false;
    }

    @Nullable
    private static IAetherRelay getBoundManipulator(@Nonnull ItemStack stack) {
        NBTTagCompound compound = stack.getTagCompound();
        if (compound == null || !compound.hasKey("bound") || !compound.getBoolean("bound")) return null;
        int x = compound.getInteger("boundX");
        int y = compound.getInteger("boundY");
        int z = compound.getInteger("boundZ");
        int dim = compound.getInteger("boundDim");
        TileEntity te = DimensionManager.getWorld(dim).getTileEntity(x, y, z);
        if (te instanceof IAetherRelay aetherManipulator) return aetherManipulator;
        return null;
    }

    private static void setBoundManipulator(@Nonnull ItemStack stack, @Nullable IAetherRelay boundManipulator) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        NBTTagCompound compound = stack.getTagCompound();
        if (boundManipulator == null) {
            compound.setBoolean("bound", false);
            return;
        }
        InterDimCoords targetPos = boundManipulator.getAetherHandler().getInterDimCoords();
        compound.setInteger("boundX", targetPos.getX());
        compound.setInteger("boundY", targetPos.getY());
        compound.setInteger("boundZ", targetPos.getZ());
        compound.setInteger("boundDim", targetPos.getDimID());
        compound.setBoolean("bound", true);
    }
}
