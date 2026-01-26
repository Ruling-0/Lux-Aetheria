package com.ruling_0.luxaetheria.common.blocks;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3i;

import com.gtnewhorizon.gtnhlib.api.IModelSelector;
import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry;
import com.gtnewhorizon.gtnhlib.client.model.loading.ResourceLoc;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.JSONModel;
import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.api.aether.handlers.IAetherHandler;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;
import com.ruling_0.luxaetheria.client.model.ModelAetherRelay;
import com.ruling_0.luxaetheria.common.tileentities.TileEntityAetherRelay;

public class BlockAetherRelay extends BlockContainer implements IModelSelector {

    public BlockAetherRelay() {
        super(Material.glass);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityAetherRelay();
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        register.registerIcon("luxaetheria:models/crystal");
        register.registerIcon("luxaetheria:models/focusarm");
        super.registerBlockIcons(register);
    }

    @Override
    public int getRenderType() {
        return ModelISBRH.JSON_ISBRH_ID;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    private static final float DEG2RAD = (float) (Math.PI / 180);

    public Matrix4fc getAffineMatrix() {
        return new Matrix4f().translation(-.5f, -.5f, -.5f)
            .rotateLocalX(90 * DEG2RAD)
            .rotateLocalY(0 * DEG2RAD)
            .translateLocal(.5f, .5f, .5f);
    }

    @Override
    public BakedModel getModel(@Nullable IBlockAccess world, Block block, int meta, int x, int y, int z) {
        Vector3i pos = new Vector3i(x, y, z);
        ArrayList<Vector3i> targets = new ArrayList<>(4);
        if (world != null) {
            final IAetherManipulator te = (IAetherManipulator) world.getTileEntity(x, y, z);
            final IAetherHandler handler = te.getAetherHandler();
            final InterDimCoords coords = handler.getInterDimCoords();
            final var iter = handler.getAetherSinksIter();
            while (iter.hasNext()) {
                var sink = iter.next();
                InterDimCoords sinkCoords = sink.getKey();
                if (sinkCoords.getDimID() != coords.getDimID()) continue;
                targets.add(new Vector3i(sinkCoords.getX(), sinkCoords.getY(), sinkCoords.getZ()));
            }
        }
        final var data = new ModelAetherRelay.RelayBakeData(pos, targets.toArray(new Vector3i[0]));
        final JSONModel jsonModel = ModelRegistry
            .getJSONModel(new ResourceLoc.ModelLoc("luxaetheria", "blocks/aether_relay"));
        final ModelAetherRelay model = new ModelAetherRelay(jsonModel);
        return model.bake(data);
    }
}
