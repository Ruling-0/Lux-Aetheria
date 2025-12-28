package com.ruling_0.luxaetheria.crossmod.wdmla;

import com.gtnewhorizons.wdmla.api.accessor.BlockAccessor;
import com.gtnewhorizons.wdmla.api.provider.IBlockComponentProvider;
import com.gtnewhorizons.wdmla.api.provider.IServerDataProvider;
import com.gtnewhorizons.wdmla.api.ui.ITooltip;
import com.gtnewhorizons.wdmla.impl.ui.component.TextComponent;
import com.ruling_0.luxaetheria.common.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.common.aether.IAetherCollector;
import com.ruling_0.luxaetheria.common.tileentities.TileEntityCollectorPylon;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

public enum AetherManipulatorProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

    INSTANCE;

    @Override
    public ResourceLocation getUid() { return Identifiers.PROV_AETHERMANIPULATOR; }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor) {
        AethericEnergyUnit aetherIn = new AethericEnergyUnit();
        AethericEnergyUnit aetherOut = new AethericEnergyUnit();
        AethericEnergyUnit aetherRelease = new AethericEnergyUnit();
        NBTTagCompound compound = accessor.getServerData();
        aetherIn.readFromNBT((NBTTagCompound) compound.getTag("aetherIn"));
        aetherOut.readFromNBT((NBTTagCompound) compound.getTag("aetherOut"));
        aetherRelease.readFromNBT((NBTTagCompound) compound.getTag("aetherRelease"));
        TileEntity te =  accessor.getTileEntity();
        if (te instanceof IAetherCollector collector) {
            tooltip.child(new TextComponent(StatCollector.translateToLocal("LA.waila.aether_out")
                + ": " + aetherOut));
            tooltip.child(new TextComponent(StatCollector.translateToLocal("LA.waila.aether_ambient")
                + ": " + collector.getAmbientAether()));
            return;
        }
        tooltip.child(new TextComponent(StatCollector.translateToLocal("LA.waila.aether_in")
            + ": " + aetherIn));
        tooltip.child(new TextComponent(StatCollector.translateToLocal("LA.waila.aether_out")
            + ": " + aetherOut));
        tooltip.child(new TextComponent(StatCollector.translateToLocal("LA.waila.aether_release")
            + ": " + aetherRelease));
    }

    @Override
    public void appendServerData(NBTTagCompound data, BlockAccessor accessor) {
        TileEntity te = accessor.getTileEntity();
        if (te == null) return;
        te.writeToNBT(data);
    }
}
