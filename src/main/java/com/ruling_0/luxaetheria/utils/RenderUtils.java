package com.ruling_0.luxaetheria.utils;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
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

    public static void drawFlare(Tessellator tesellator, Vector3f pos, Vector3f camera) {
        drawFlare(tesellator, pos, camera, new Vector3f(1.0f, 1.0f, 1.0f));
    }

    public static void drawFlare(Tessellator tessellator, Vector3f pos, Vector3f camera, Vector3f color) {
        drawFlare(tessellator, pos, camera, color, new Vector3f(0.0f, 0.0f, 0.0f));
    }

    public static void drawFlare(Tessellator tessellator, Vector3f pos, Vector3f camera, Vector3f color, Vector3f offset) {
        GL11.glColor4f(color.x, color.y, color.z, 1.0F);

        // Start facing North (neg Z), going TopLeft > BL > BR > TR
        Vector3f v1 = new Vector3f((-1.0F / 16.0F) + offset.x, (1.0F / 16.0F)  + offset.y, offset.z);
        Vector3f v2 = new Vector3f((-1.0F / 16.0F) + offset.x, (-1.0F / 16.0F) + offset.y, offset.z);
        Vector3f v3 = new Vector3f((1.0F / 16.0F)  + offset.x, (-1.0F / 16.0F) + offset.y, offset.z);
        Vector3f v4 = new Vector3f((1.0F / 16.0F)  + offset.x, (1.0F / 16.0F)  + offset.y, offset.z);
        final Vector3f norm = new Vector3f(0.0F, 0.0F, -1.0F);

        // Rotate perpendicular to camera
        final Vector3f p = new Vector3f(camera).sub(new Vector3f(pos).add(offset));
        final Quaternionf q = new Quaternionf();
        norm.rotationTo(p, q);
        final Matrix4f rot = new Matrix4f().rotate(q);
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
    }
}
