package com.ruling_0.luxaetheria.client.renderer;

import java.util.Iterator;
import java.util.Map;

import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import com.ruling_0.luxaetheria.LuxAetheria;
import com.ruling_0.luxaetheria.api.aether.AetherAspects;
import com.ruling_0.luxaetheria.api.aether.AethericEnergyUnit;
import com.ruling_0.luxaetheria.api.aether.IAetherManipulator;
import com.ruling_0.luxaetheria.api.aether.handlers.IAetherHandler;
import com.ruling_0.luxaetheria.api.utils.InterDimCoords;

import org.lwjgl.opengl.GL11;

public class AetherBeamRenderer extends TileEntitySpecialRenderer {

    private static final ResourceLocation BEAM_TEXTURE = new ResourceLocation(
        LuxAetheria.MODID,
        "textures/entity/aether_beam.png");

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float timeSinceLastTick) {
        // TODO: add a flare effect at beam end
        final IAetherHandler handler = ((IAetherManipulator) te).getAetherHandler();
        if (handler == null) return;

        Iterator<Map.Entry<InterDimCoords, IAetherManipulator>> iterSinks = handler.getAetherSinksIter();
        if (!iterSinks.hasNext()) return;

        AethericEnergyUnit aether = handler.getAetherOut();
        if (aether.getAmount() <= 0) return;

        byte red = (byte) Math.ceil(aether.getAspectRatio(AetherAspects.RED.index) * 255);
        byte green = (byte) Math.ceil(aether.getAspectRatio(AetherAspects.GREEN.index) * 255);
        byte blue = (byte) Math.ceil(aether.getAspectRatio(AetherAspects.BLUE.index) * 255);

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);

        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        this.bindTexture(BEAM_TEXTURE);

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.f, 240.f);

        GL11.glColor4ub(red, green, blue, (byte) 200);

        Tessellator tessellator = Tessellator.instance;
        double time = (double) te.getWorldObj().getTotalWorldTime() + timeSinceLastTick;
        double uOffset = -time * 0.1;

        Vec3 cameraPos = Vec3.createVectorHelper(ActiveRenderInfo.objectX, ActiveRenderInfo.objectY, ActiveRenderInfo.objectZ);
        Vec3 sourcePos = Vec3.createVectorHelper(te.xCoord + 0.5, te.yCoord + 0.5, te.zCoord + 0.5);

        while (iterSinks.hasNext()) {
            InterDimCoords sinkCoords = iterSinks.next().getKey();
            Vec3 sinkPos = handler.getSinkCollisionCoords(sinkCoords);

            Vec3 v = Vec3.createVectorHelper(sinkPos.xCoord - sourcePos.xCoord, sinkPos.yCoord - sourcePos.yCoord,
                    sinkPos.zCoord - sourcePos.zCoord); // Vector from source to sink
            double dist = v.lengthVector();
            double uvdist = dist * 4;
            if (dist < 0.0001) continue;

            Vec3 p = Vec3.createVectorHelper(cameraPos.xCoord - x, cameraPos.yCoord - y, cameraPos.zCoord - z);
            Vec3 w = v.crossProduct(p);
            if (w.lengthVector() < 1e-6) {
                // If camera is perfectly aligned with beam, pick an arbitrary perpendicular
                w = v.crossProduct(Vec3.createVectorHelper(0, 1, 0));
                if (w.lengthVector() < 1e-6) {
                    w = v.crossProduct(Vec3.createVectorHelper(1, 0, 0));
                }
            }
            w = w.normalize();

            double radius = 0.07;
            double wx = w.xCoord * radius;
            double wy = w.yCoord * radius;
            double wz = w.zCoord * radius;

            double dx = sinkPos.xCoord - sourcePos.xCoord;
            double dy = sinkPos.yCoord - sourcePos.yCoord;
            double dz = sinkPos.zCoord - sourcePos.zCoord;

            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(-wx, -wy, -wz, 0, uOffset);
            tessellator.addVertexWithUV(wx, wy, wz, 1, uOffset);
            tessellator.addVertexWithUV(dx + wx, dy + wy, dz + wz, 1, uvdist + uOffset);
            tessellator.addVertexWithUV(dx - wx, dy - wy, dz - wz, 0, uvdist + uOffset);
            tessellator.draw();
        }

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
}
