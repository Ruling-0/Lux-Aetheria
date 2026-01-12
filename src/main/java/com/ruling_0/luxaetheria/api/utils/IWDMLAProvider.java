package com.ruling_0.luxaetheria.api.utils;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;

public interface IWDMLAProvider {

    /**
     * Write aesthetic data for WDMLA consumption.
     */
    void writeWDMLAData(@Nonnull NBTTagCompound compound);

}
