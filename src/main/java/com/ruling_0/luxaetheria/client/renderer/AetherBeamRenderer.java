package com.ruling_0.luxaetheria.client.renderer;

import java.util.Iterator;

import com.ruling_0.luxaetheria.utils.RenderUtils;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.api.aether.AetherAspect;
import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.api.aether.connections.ImmutableSinkConnection;
import com.ruling_0.luxaetheria.api.aether.handlers.IAetherHandler;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.opengl.GL11;

public class AetherBeamRenderer extends TileEntitySpecialRenderer {

    private static final ResourceLocation BEAM_TEXTURE = new ResourceLocation(
        LuxAetheria.MODID, "textures/entity/aether_beam.png");
    private static final ResourceLocation FLARE_TEXTURE = new ResourceLocation(
        LuxAetheria.MODID, "textures/entity/aether_flare.png");

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float timeSinceLastTick) {
        final IAetherHandler handler = ((IAetherManipulator) te).getAetherHandler();
        if (handler == null) return;

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
        final Vector3f p = new Vector3f(cameraPos.x - ((float) x + 0.5F), cameraPos.y - ((float) y + 0.5F), cameraPos.z - ((float) z + 0.5F));

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
                RenderUtils.drawBeam(tessellator, sourcePos, sinkPos, beamColor, p, time);
            }
        }

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.bindTexture(FLARE_TEXTURE);

        // Start facing North (neg Z), going TopLeft > BL > BR > TR
        Vector3f v1 = new Vector3f(-1.0F / 16.0F, 1.0F / 16.0F, 0.0F);
        Vector3f v2 = new Vector3f(-1.0F / 16.0F, -1.0F / 16.0F, 0.0F);
        Vector3f v3 = new Vector3f(1.0F / 16.0F, -1.0F / 16.0F, 0.0F);
        Vector3f v4 = new Vector3f(1.0F / 16.0F, 1.0F / 16.0F, 0.0F);
        final Vector3f norm = new Vector3f(0.0F, 0.0F, -1.0F);

        // Rotate perpendicular to camera
        final Quaternionf q = new Quaternionf();
        norm.rotationTo(p, q);
        final Vector3f mov = new Vector3f(norm).mul(0.5F);
        final Matrix4f rot = new Matrix4f().rotate(q).translate(mov);
        v1.mulPosition(rot);
        v2.mulPosition(rot);
        v3.mulPosition(rot);
        v4.mulPosition(rot);

        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(v1.x, v1.y, v1.z, 0.0D, 0.0D);
        tessellator.addVertexWithUV(v2.x, v2.y, v2.z, 0.0D, 1.0D);
        tessellator.addVertexWithUV(v3.x, v3.y, v3.z, 1.0D, 1.0D);
        tessellator.addVertexWithUV(v4.x, v4.y, v4.z, 1.0D, 0.0D);
        tessellator.draw();

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
}
