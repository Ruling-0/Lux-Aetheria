package com.ruling_0.luxaetheria.crossmod.wdmla;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import com.gtnewhorizons.wdmla.api.accessor.BlockAccessor;
import com.gtnewhorizons.wdmla.api.provider.IBlockComponentProvider;
import com.gtnewhorizons.wdmla.api.provider.IServerDataProvider;
import com.gtnewhorizons.wdmla.api.ui.ITooltip;
import com.gtnewhorizons.wdmla.impl.ui.component.TextComponent;
import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherCollector;
import com.ruling_0.luxaetheria.api.aether.IAetherReleaser;

public enum AetherManipulatorProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    INSTANCE;

    @Override
    public ResourceLocation getUid() {
        return Identifiers.PROV_AETHERMANIPULATOR;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, @Nonnull BlockAccessor accessor) {
        AethericEnergyUnit aetherIn = new AethericEnergyUnit();
        AethericEnergyUnit aetherOut = new AethericEnergyUnit();
        NBTTagCompound compound = accessor.getServerData();
        aetherIn.readFromNBT((NBTTagCompound) compound.getTag("aetherIn"));
        aetherOut.readFromNBT((NBTTagCompound) compound.getTag("aetherOut"));
        TileEntity te = accessor.getTileEntity();
        if (te instanceof IAetherCollector) {
            tooltip.child(new TextComponent(StatCollector.translateToLocal("LA.waila.aether_out") + ": " + aetherOut));
            AethericEnergyUnit aetherAmbient = new AethericEnergyUnit();
            aetherAmbient.readFromNBT((NBTTagCompound) compound.getTag("aetherAmbient"));
            tooltip.child(
                new TextComponent(StatCollector.translateToLocal("LA.waila.aether_ambient") + ": " + aetherAmbient));
            return;
        }
        tooltip.child(new TextComponent(StatCollector.translateToLocal("LA.waila.aether_in") + ": " + aetherIn));
        tooltip.child(new TextComponent(StatCollector.translateToLocal("LA.waila.aether_out") + ": " + aetherOut));
        if (te instanceof IAetherReleaser) {
            AethericEnergyUnit aetherRelease = new AethericEnergyUnit();
            aetherRelease.readFromNBT((NBTTagCompound) compound.getTag("aetherRelease"));
            tooltip.child(
                new TextComponent(StatCollector.translateToLocal("LA.waila.aether_release") + ": " + aetherRelease));
        }
    }

    @Override
    public void appendServerData(NBTTagCompound data, @Nonnull BlockAccessor accessor) {
        TileEntity te = accessor.getTileEntity();
        if (te == null) return;
        if (te instanceof IAetherReleaser releaser) {
            releaser.writeWAILAData(data);
            return;
        }
        te.writeToNBT(data);
    }
}
