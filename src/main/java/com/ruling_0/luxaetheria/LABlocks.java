package com.ruling_0.luxaetheria;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import com.ruling_0.luxaetheria.common.blocks.BlockAetherRelay;
import com.ruling_0.luxaetheria.common.blocks.BlockAethericFurnace;
import com.ruling_0.luxaetheria.common.blocks.BlockAspectSplitter;
import com.ruling_0.luxaetheria.common.blocks.BlockCollectorPylon;

import cpw.mods.fml.common.registry.GameRegistry;

// Credit to Et Futurum (Requiem)
public enum LABlocks {
    // spotless:off

    AETHERIC_FURNACE(true, new BlockAethericFurnace(false), "aetheric_furnace"),
    AETHERIC_FURNACE_LIT(true, new BlockAethericFurnace(true), "aetheric_furance_lit", false),
    COLLECTOR_PYLON(true, new BlockCollectorPylon(), "collector_pylon"),
    AETHER_RELAY(true, new BlockAetherRelay(), "aether_relay"),
    AETHER_SPLITTER(true, new BlockAspectSplitter(), "aether_splitter"),
    ;

    // spotless:on

    public static final LABlocks[] VALUES = values();

    public static void init() {
        for (LABlocks blockEntry : VALUES) {
            if (blockEntry.isEnabled()) {
                if (blockEntry.addToTab) blockEntry.block.setCreativeTab(LuxAetheria.tabLuxAetheria);
                if (blockEntry.getItemBlock() != null || !blockEntry.getHasItemBlock()) {
                    /*
                     * This part is used if the getItemBlock() is not ItemBlock.class,
                     * so we register a custom ItemBlock class as the ItemBlock.
                     * It is also used if the getItemBlock() == null and getHasItemBlock() is false,
                     * meaning we WANT to register it as null, making the block have no inventory item.
                     */
                    GameRegistry.registerBlock(blockEntry.get(), blockEntry.getItemBlock(), blockEntry.name);
                }
                else {
                    // Register with default item if getItemBlock() == null but getHasItemBlock() is true.
                    GameRegistry.registerBlock(blockEntry.get(), blockEntry.name);
                }
                blockEntry.block.setBlockName(blockEntry.name);
                blockEntry.block.setBlockTextureName(LuxAetheria.MODID + ":" + blockEntry.name);
            }
        }
    }

    private final boolean isEnabled;
    private final Block block;
    private final boolean addToTab;
    /**
     * null == default ItemBlock
     */
    private final Class<? extends ItemBlock> itemBlock;
    /**
     * Determines if we should register the block with an ItemBlock.
     * Set to false when the constructor that specifies the ItemBlock is specifically set to false.
     */
    private boolean hasItemBlock;
    private final String name;

    LABlocks(Boolean enabled, Block block, String name) {
        this(enabled, block, name, true);
    }

    LABlocks(Boolean enabled, Block block, String name, Boolean addToTab) {
        this(enabled, block, null, name, addToTab);
        this.hasItemBlock = true;
    }

    LABlocks(Boolean enabled, Block block, Class<? extends ItemBlock> itemBlock, String name, Boolean addToTab) {
        this.isEnabled = enabled;
        this.block = block;
        this.itemBlock = itemBlock;
        this.addToTab = addToTab;
        this.hasItemBlock = itemBlock != null;
        this.name = name;
    }

    /**
     * If this is false, the block is initialized without an inventory item, or ItemBlock.
     */
    public boolean getHasItemBlock() { return this.hasItemBlock; }

    public Block get() {
        return this.block;
    }

    public Class<? extends ItemBlock> getItemBlock() { return this.itemBlock; }

    public Item getItem() { return Item.getItemFromBlock(get()); }

    public boolean isEnabled() { return this.isEnabled; }

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
