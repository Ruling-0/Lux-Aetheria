package com.ruling_0.luxaetheria.crossmod.wdmla;

import com.gtnewhorizons.wdmla.api.IWDMlaClientRegistration;
import com.gtnewhorizons.wdmla.api.IWDMlaCommonRegistration;
import com.gtnewhorizons.wdmla.api.IWDMlaPlugin;
import com.gtnewhorizons.wdmla.api.WDMlaPlugin;
import com.ruling_0.luxaetheria.common.blocks.BlockAethericFurnace;
import com.ruling_0.luxaetheria.common.blocks.BlockCollectorPylon;
import org.jetbrains.annotations.NotNull;

@WDMlaPlugin(uid = "luxaetheria")
public class LuxAetheriaWDMLAPlugin implements IWDMlaPlugin {

    @Override
    public void register(@NotNull IWDMlaCommonRegistration registration) {
        registration.registerBlockDataProvider(AetherManipulatorProvider.INSTANCE, BlockAethericFurnace.class);
        registration.registerBlockDataProvider(AetherManipulatorProvider.INSTANCE, BlockCollectorPylon.class);
    }

    @Override
    public void registerClient(@NotNull IWDMlaClientRegistration registration) {
        registration.registerBlockComponent(AetherManipulatorProvider.INSTANCE, BlockAethericFurnace.class);
        registration.registerBlockComponent(AetherManipulatorProvider.INSTANCE, BlockCollectorPylon.class);
    }
}
