package com.ruling_0.luxaetheria.utils;

import com.ruling_0.luxaetheria.LuxAetheria;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.opengl.GL11;

public final class RenderUtils {

    public static void drawBeam(Tessellator tessellator, Vector3fc origin, Vector3fc target, Vector3f color, Vector3f camera, double time) {
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.f, 240.f);

        GL11.glColor4f(color.x, color.y, color.z, 1.0f);
        final double uOffset = -time * 0.1;

        final Vector3f v = new Vector3f(target).sub(origin);
        final double dist = v.length();
        final double uvdist = dist * 4;
        if (dist < 1e-6) return;

        final Vector3f p = new Vector3f(camera).sub(new Vector3f(v).mul(0.5f));
        Vector3f w = new Vector3f(v).cross(p);
        if (w.length() < 1e-6) {
            // If the camera is perfectly aligned with the beam, pick an arbitrary perpendicular
            w = new Vector3f(v).cross(new Vector3f(0.0F, 1.0F, 0.0F));
            if (w.length() < 1e-6) {
                w = new Vector3f(v).cross(new Vector3f(1.0F, 0.0F, 0.0F));
                if (w.length() < 1e-6) w = new Vector3f(0.0F, 0.0F, 1.0F);
            }
        }
        w.normalize();

        final double radius = 0.07;
        final double wx = w.x * radius;
        final double wy = w.y * radius;
        final double wz = w.z * radius;

        final double dx = target.x() - origin.x();
        final double dy = target.y() - origin.y();
        final double dz = target.z() - origin.z();

        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(-wx, -wy, -wz, 0, uOffset);
        tessellator.addVertexWithUV(wx, wy, wz, 1, uOffset);
        tessellator.addVertexWithUV(dx + wx, dy + wy, dz + wz, 1, uvdist + uOffset);
        tessellator.addVertexWithUV(dx - wx, dy - wy, dz - wz, 0, uvdist + uOffset);
        tessellator.draw();
    }
}
