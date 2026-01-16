package com.ruling_0.luxaetheria;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.ruling_0.luxaetheria.common.items.ItemPylonBinder;

import cpw.mods.fml.common.registry.GameRegistry;

public enum LAItems {
    // spotless:off

    PYLON_BINDER(true, new ItemPylonBinder(), "pylon_binder"),
    ;

    // spotless:on

    public static final LAItems[] VALUES = values();

    public static void init() {
        for (LAItems itemEntry : VALUES) {
            if (itemEntry.isEnabled) {
                if (itemEntry.addToTab) itemEntry.item.setCreativeTab(LuxAetheria.tabLuxAetheria);
                GameRegistry.registerItem(itemEntry.item, itemEntry.name);
                itemEntry.item.setUnlocalizedName(itemEntry.name);
                itemEntry.item.setTextureName(LuxAetheria.MODID + ":" + itemEntry.name);
            }
        }
    }

    private final boolean isEnabled;
    private final Item item;
    private final String name;
    private final boolean addToTab;

    LAItems(Boolean enabled, Item item, String name) {
        this(enabled, item, name, true);
    }

    LAItems(Boolean enabled, Item item, String name, Boolean addToTab) {
        this.isEnabled = enabled;
        this.item = item;
        this.name = name;
        this.addToTab = addToTab;
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public Item get() {
        return this.item;
    }

    public ItemStack newItemStack() {
        return newItemStack(1);
    }

    public ItemStack newItemStack(int count) {
        return newItemStack(count, 0);
    }

    public ItemStack newItemStack(int count, int meta) {
        return new ItemStack(this.get(), count, meta);
    }
}
