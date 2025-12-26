package com.ruling_0.luxaetheria.crossmod;

import com.gtnewhorizon.gtnhlib.util.data.IMod;
import cpw.mods.fml.common.Loader;
import net.minecraft.util.ResourceLocation;

import java.util.Locale;

// Credit to GT5-Unofficial
public enum Mods implements IMod {

    // spotless:off

    WDMLA(ModIDs.WDMLA),
    ;

    // spotless:on

    public final String ID;
    public final String resourceDomain;
    protected boolean checked, modLoaded;

    Mods(String ID) {
        this.ID = ID;
        this.resourceDomain = ID.toLowerCase(Locale.ENGLISH);
    }

    // isModLoaded is final to allow the JIT to inline this
    @Override
    public final boolean isModLoaded() {
        if (!this.checked) {
            this.modLoaded = Loader.isModLoaded(ID);
            this.checked = true;
        }
        return this.modLoaded;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String getResourceLocation() {
        return resourceDomain;
    }

    public String getResourcePath(String path) {
        return this.getResourceLocation(path)
            .toString();
    }

    public String getResourcePath(String... path) {
        return this.getResourceLocation(path)
            .toString();
    }

    public ResourceLocation getResourceLocation(String path) {
        return new ResourceLocation(this.resourceDomain, path);
    }

    public ResourceLocation getResourceLocation(String... path) {
        return new ResourceLocation(this.resourceDomain, String.join("/", path));
    }

    public static class ModIDs {

        // spotless:off

        public static final String WDMLA = "wdmla";

        // spotless:on
    }
}
