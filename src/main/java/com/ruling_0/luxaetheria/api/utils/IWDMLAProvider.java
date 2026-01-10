package com.ruling_0.luxaetheria.api.utils;

import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;

public interface IWDMLAProvider {

    /**
     * Write aesthetic data for WDMLA consumption.
     */
    void writeWDMLAData(@Nonnull NBTTagCompound compound);

}
