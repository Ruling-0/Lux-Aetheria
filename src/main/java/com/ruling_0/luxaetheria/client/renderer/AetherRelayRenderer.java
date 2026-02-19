package com.ruling_0.luxaetheria.client.renderer;

import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.api.aether.AetherAspect;
import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherRelay;
import com.ruling_0.luxaetheria.api.aether.connections.ImmutableSinkConnection;
import com.ruling_0.luxaetheria.api.aether.handlers.IRelayHandler;
import com.ruling_0.luxaetheria.utils.RenderUtils;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;

public class AetherRelayRenderer extends TileEntitySpecialRenderer {

    private static final ResourceLocation BEAM_TEXTURE = new ResourceLocation(
        LuxAetheria.MODID, "textures/entity/aether_beam.png");
    private static final ResourceLocation FLARE_TEXTURE = new ResourceLocation(
        LuxAetheria.MODID, "textures/entity/aether_flare.png");

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float timeSinceLastTick) {
        final IRelayHandler handler = ((IAetherRelay) te).getAetherHandler();
        if (handler == null) return;
        final Vector3f pos = new Vector3f((float) x + 0.5f, (float) y + 0.5f, (float) z + 0.5f);

        Iterator<ImmutableSinkConnection> iterSinks = handler.getAetherSinksIter();
        AethericEnergyUnit aether = handler.getAetherOut();

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);

        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        final Vector3f cameraPos = new Vector3f(ActiveRenderInfo.objectX, ActiveRenderInfo.objectY,
            ActiveRenderInfo.objectZ);

        Tessellator tessellator = Tessellator.instance;
        if (iterSinks.hasNext() && aether.getAmount() > 0) {
            final Vector3f beamColor = new Vector3f((float) aether.getAspectRatio(AetherAspect.RED),
                (float) aether.getAspectRatio(AetherAspect.GREEN), (float) aether.getAspectRatio(AetherAspect.BLUE));
            this.bindTexture(BEAM_TEXTURE);

            double time = (double) te.getWorldObj().getTotalWorldTime() + timeSinceLastTick;
            final Vector3f sourcePos = new Vector3f(te.xCoord + 0.5F, te.yCoord + 0.5F, te.zCoord + 0.5F);

            while (iterSinks.hasNext()) {
                ImmutableSinkConnection sinkConn = iterSinks.next();
                final Vector3fc sinkPos = sinkConn.getColCoords();
                RenderUtils.drawBeam(tessellator, sourcePos, sinkPos, beamColor, cameraPos, time);
            }
        }

        this.bindTexture(FLARE_TEXTURE);
        RenderUtils.drawFlare(tessellator, pos, cameraPos);

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
}
