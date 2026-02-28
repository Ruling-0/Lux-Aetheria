package com.ruling_0.luxaetheria.client.model;

import static com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.ModelElement.Rotation.NOOP;
import static com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry.MODEL_LOGGER;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraftforge.common.util.ForgeDirection;

import com.google.common.base.Objects;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.baked.PileOfQuads;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer;
import com.gtnewhorizon.gtnhlib.client.model.loading.ResourceLoc;
import com.gtnewhorizon.gtnhlib.client.model.state.StateDeserializer;
import com.gtnewhorizon.gtnhlib.client.model.state.StateModelMap;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.JSONModel;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.util.MathUtil;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;
import org.joml.Vector4f;

// TODO: Refactor and break out into some ComplexModel, and make this relay-specific child class
public class ModelAetherRelay extends JSONModel {

    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(StateModelMap.class, new StateDeserializer())
        .registerTypeAdapter(JSONModel.class, new ModelDeserializer())
        .create();

    public ModelAetherRelay(ResourceLoc.@Nullable ModelLoc parentId, boolean useAO,
                            Map<ModelDeserializer.Position, ModelDeserializer.Position.ModelDisplay> display,
                            @NotNull Object2ObjectMap<String, String> textures,
                            List<ModelDeserializer.ModelElement> elements) {
        super(parentId, useAO, display, textures, elements);
    }

    public ModelAetherRelay(JSONModel model) {
        super(model);
    }

    public static final class RelayBakeData {

        private final Matrix4fc[] matrices;

        /**
         * Original vector of the arms
         */
        private static Vector3fc getOrig(ForgeDirection dir) {
            return switch (dir) {
                case DOWN, EAST, WEST, UNKNOWN -> new Vector3f(0.0f, 0.0f, -1.0f); // North
                case NORTH -> new Vector3f(0.0f, 1.0f, 0.0f); // Up
                case UP -> new Vector3f(0.0f, 0.0f, 1.0f); // South
                case SOUTH -> new Vector3f(0.0f, -1.0f, 0.0f); // Down
            };
        }

        private static AxisAngle4f getDirRot(ForgeDirection dir) {
            return switch (dir) {
                case DOWN, UNKNOWN -> new AxisAngle4f(0.0f, 0.0f, 1.0f, 0.0f);
                case UP -> new AxisAngle4f((float) Math.toRadians(180.0f), 1.0f, 0.0f, 0.0f);
                case NORTH -> new AxisAngle4f((float) Math.toRadians(90.0f), 1.0f, 0.0f, 0.0f);
                case SOUTH -> new AxisAngle4f((float) Math.toRadians(-90.0f), 1.0f, 0.0f, 0.0f);
                case EAST -> new AxisAngle4f((float) Math.toRadians(90.0f), 0.0f, 0.0f, 1.0f);
                case WEST -> new AxisAngle4f((float) Math.toRadians(-90.0f), 0.0f, 0.0f, 1.0f);
            };
        }

        public RelayBakeData(Vector3i pos, Vector3i[] targets, int meta) {
            final ForgeDirection forgeDir = ForgeDirection.getOrientation(meta);
            final Vector3fc orig = getOrig(forgeDir);

            final Matrix4f baseMat = new Matrix4f()
                .translate(0.5f, 0.5f, 0.5f)
                .rotate(getDirRot(forgeDir))
                .translate(-0.5f, -0.5f, -0.5f);
            if (targets.length == 0) {
                this.matrices = new Matrix4fc[] { baseMat };
                return;
            }

            this.matrices = new Matrix4f[targets.length + 1];
            this.matrices[0] = baseMat;

            switch (forgeDir) {
                case DOWN, UNKNOWN:
                    for (int i = 1; i < this.matrices.length; ++i) {
                        final Vector3f dir = new Vector3f(targets[i - 1].sub(pos)).normalize();
                        final Vector3f dirAZ = new Vector3f(dir.x, 0.0f, dir.z).normalize();

                        final float sinAZ = new Vector3f(orig).cross(dirAZ).y;
                        final float angAZ = sinAZ == 0 ? (float) Math.acos(orig.dot(dirAZ)) :
                            (float) Math.acos(orig.dot(dirAZ)) * Math.signum(sinAZ);
                        final float angEL = (float) Math.acos(dirAZ.dot(dir)) * Math.signum(dir.y);

                        this.matrices[i] = new Matrix4f()
                            .translate(0.5f, 0.5f, 0.5f)
                            .rotateY(angAZ)
                            .rotateX(angEL)
                            .rotate(getDirRot(forgeDir))
                            .translate(-0.5f, -0.5f, -0.5f);
                    }
                    break;
                case UP:
                    for (int i = 1; i < this.matrices.length; ++i) {
                        final Vector3f dir = new Vector3f(targets[i - 1].sub(pos)).normalize();
                        dir.y = -dir.y;
                        final Vector3f dirAZ = new Vector3f(dir.x, 0.0f, dir.z).normalize();

                        final float sinAZ = new Vector3f(orig).cross(dirAZ).y;
                        final float angAZ = sinAZ == 0 ? (float) Math.acos(orig.dot(dirAZ)) :
                            (float) Math.acos(orig.dot(dirAZ)) * Math.signum(sinAZ);
                        final float angEL = (float) Math.acos(dirAZ.dot(dir)) * Math.signum(dir.y);

                        this.matrices[i] = new Matrix4f()
                            .translate(0.5f, 0.5f, 0.5f)
                            .rotateY(angAZ)
                            .rotateX(angEL)
                            .rotate(getDirRot(forgeDir))
                            .translate(-0.5f, -0.5f, -0.5f);
                    }
                    break;
                case NORTH:
                    for (int i = 1; i < this.matrices.length; ++i) {
                        final Vector3f dir = new Vector3f(targets[i - 1].sub(pos)).normalize();
                        final Vector3f dirAZ = new Vector3f(dir.x, dir.y, 0.0f).normalize();

                        final float sinAZ = new Vector3f(orig).cross(dirAZ).z;
                        final float angAZ = sinAZ == 0 ? (float) Math.acos(orig.dot(dirAZ)) :
                            (float) Math.acos(orig.dot(dirAZ)) * Math.signum(sinAZ);
                        final float angEL = (float) Math.acos(dirAZ.dot(dir)) * Math.signum(dir.z);

                        this.matrices[i] = new Matrix4f()
                            .translate(0.5f, 0.5f, 0.5f)
                            .rotate(getDirRot(forgeDir))
                            .rotateY(angAZ)
                            .rotateX(angEL)
                            .translate(-0.5f, -0.5f, -0.5f);
                    }
                    break;
                case SOUTH:
                    for (int i = 1; i < this.matrices.length; ++i) {
                        final Vector3f dir = new Vector3f(targets[i - 1].sub(pos)).normalize();
                        dir.z = -dir.z;
                        final Vector3f dirAZ = new Vector3f(dir.x, dir.y, 0.0f).normalize();

                        final float sinAZ = -(new Vector3f(orig).cross(dirAZ).z);
                        final float angAZ = sinAZ == 0 ? (float) Math.acos(orig.dot(dirAZ)) :
                            (float) Math.acos(orig.dot(dirAZ)) * Math.signum(sinAZ);
                        final float angEL = (float) Math.acos(dirAZ.dot(dir)) * Math.signum(dir.z);

                        this.matrices[i] = new Matrix4f()
                            .translate(0.5f, 0.5f, 0.5f)
                            .rotate(getDirRot(forgeDir))
                            .rotateY(angAZ)
                            .rotateX(angEL)
                            .translate(-0.5f, -0.5f, -0.5f);
                    }
                    break;
                case WEST:
                    for (int i = 1; i < this.matrices.length; ++i) {
                        final Vector3f dir = new Vector3f(targets[i - 1].sub(pos)).normalize();
                        final Vector3f dirAZ = new Vector3f(0.0f, dir.y, dir.z).normalize();

                        final float sinAZ = new Vector3f(orig).cross(dirAZ).x;
                        final float angAZ = sinAZ == 0 ? (float) Math.acos(orig.dot(dirAZ)) :
                            (float) Math.acos(orig.dot(dirAZ)) * Math.signum(sinAZ);
                        final float angEL = (float) Math.acos(dirAZ.dot(dir)) * Math.signum(dir.x);

                        this.matrices[i] = new Matrix4f()
                            .translate(0.5f, 0.5f, 0.5f)
                            .rotate(getDirRot(forgeDir))
                            .rotateY(angAZ)
                            .rotateX(angEL)
                            .translate(-0.5f, -0.5f, -0.5f);
                    }
                    break;
                case EAST:
                    for (int i = 1; i < this.matrices.length; ++i) {
                        final Vector3f dir = new Vector3f(targets[i - 1].sub(pos)).normalize();
                        dir.x = -dir.x;
                        final Vector3f dirAZ = new Vector3f(0.0f, dir.y, dir.z).normalize();

                        final float sinAZ = -(new Vector3f(orig).cross(dirAZ).x);
                        final float angAZ = sinAZ == 0 ? (float) Math.acos(orig.dot(dirAZ)) :
                            (float) Math.acos(orig.dot(dirAZ)) * Math.signum(sinAZ);
                        final float angEL = (float) Math.acos(dirAZ.dot(dir)) * Math.signum(dir.x);

                        this.matrices[i] = new Matrix4f()
                            .translate(0.5f, 0.5f, 0.5f)
                            .rotate(getDirRot(forgeDir))
                            .rotateY(angAZ)
                            .rotateX(angEL)
                            .translate(-0.5f, -0.5f, -0.5f);
                    }
                    break;
            }
        }

        public Matrix4fc getAffineMatrix(int i) {
            return this.matrices[i];
        }

        public int count() {
            return this.matrices.length;
        }
    }

    protected void generateQuads(ModelDeserializer.ModelElement e,
                               HashMap<ModelQuadFacing, ArrayList<ModelQuadView>> sidedQuadStore, Matrix4fc affine,
                               boolean isTransparent) {
        final Matrix4f rot = (e.rotation() == null) ? NOOP.getAffineMatrix() : e.rotation().getAffineMatrix();
        for (ModelDeserializer.ModelElement.Face f : e.faces()) {

            // Assign vertexes
            final var quad = new ModelQuad();
            for (int i = 0; i < 4; ++i) {
                final Vector3f vert = mapSideToVertex(e.from(), e.to(), i,
                    f.name()).mulPosition(rot).mulPosition(affine);
                quad.setX(i, vert.x);
                quad.setY(i, vert.y);
                quad.setZ(i, vert.z);
            }

            // Set shading and lighting
            quad.setEmissiveness(e.lightEmission());
            quad.setDirectionalShading(e.shade());
            quad.setHasAmbientOcclusion(this.useAO);
            quad.setLightFace(ModelQuadFacing.fromForgeDir(f.name()));

            // Set UV
            Vector4f uv = Objects.firstNonNull(f.uv(), DEFAULT_UV);
            Vector2f[] uvs = new Vector2f[] { new Vector2f(uv.x, uv.y), new Vector2f(uv.x, uv.w),
                new Vector2f(uv.z, uv.w), new Vector2f(uv.z, uv.y) };

            for (int i = 0; i < 4; i++) {
                setUV(quad, i, uvs[i].x, uvs[i].y);
            }

            // Set the sprite
            var texKey = f.texture();
            var texName = this.textures.get(texKey);
            if (texName.startsWith("#")) {
                MODEL_LOGGER.warn("Model {} has unflattened texture variable {} when baking!", this, texName);
                this.textures.put(texKey, "minecraft:missing");
                texName = "minecraft:missing";
            }
            this.bakeSprite(quad, texName);

            // Set the tint index
            quad.setColorIndex(f.tintIndex());

            if (isTransparent) quad.setTransparent();

            // Bake and add it

            ModelQuadFacing cullFace = ModelQuadFacing.fromForgeDir(f.cullFace());
            if (cullFace.isDirection()) {
                // If cullface is not unassigned, we rotate it by the affine matrix, so that way the direction we
                // check for culling also is rotated.
                Vector3f facing = new Vector3f(cullFace.getStepX(), cullFace.getStepY(), cullFace.getStepZ())
                    .mulDirection(affine);
                // Only one of these three vector coordinates should be a value other than 0. Error handling should
                // be done where the baseRot's data is serialized.
                if (MathUtil.roughlyEqual(facing.x, 1)) {
                    cullFace = ModelQuadFacing.POS_X;
                }
                if (MathUtil.roughlyEqual(facing.x, -1)) {
                    cullFace = ModelQuadFacing.NEG_X;
                }
                if (MathUtil.roughlyEqual(facing.y, 1)) {
                    cullFace = ModelQuadFacing.POS_Y;
                }
                if (MathUtil.roughlyEqual(facing.y, -1)) {
                    cullFace = ModelQuadFacing.NEG_Y;
                }
                if (MathUtil.roughlyEqual(facing.z, 1)) {
                    cullFace = ModelQuadFacing.POS_Z;
                }
                if (MathUtil.roughlyEqual(facing.z, -1)) {
                    cullFace = ModelQuadFacing.NEG_Z;
                }
            }
            sidedQuadStore.computeIfAbsent(cullFace, d -> new ArrayList<>()).add(quad);
        }
    }

    public BakedModel bake(RelayBakeData data) {
        final var sidedQuadStore = new HashMap<ModelQuadFacing, ArrayList<ModelQuadView>>(7);
        final var parts = new ArrayList[data.count()];
        int colCount = 0;
        for (int i = 0; i < data.count(); ++i) {
            parts[i] = new ArrayList<ModelDeserializer.ModelElement>();
        }

        for (ModelDeserializer.ModelElement e : this.elements) {
            final int partID = Integer.parseInt(e.name().split(":")[1]);
            // partID 0 always rendered
            if (partID > data.count() - 1) continue;
            // noinspection unchecked
            parts[partID].add(e);
            if (partID == 0) ++colCount;
        }
        final Vector3f[] colBoxes = new Vector3f[colCount * 2];
        int colIdx = 0;

        // i=0 handles base and collisions
        // noinspection unchecked
        final ArrayList<ModelDeserializer.ModelElement> base = parts[0];
        final var baseRot = data.getAffineMatrix(0);
        for (ModelDeserializer.ModelElement e : base) {
            final var eNameParts = e.name().split(":");
            final boolean isTransparent = eNameParts.length > 3 && eNameParts[3].equals("t");
            final Vector3f r1 = new Vector3f(e.from()).mulPosition(baseRot);
            final Vector3f r2 = new Vector3f(e.to()).mulPosition(baseRot);
            final Vector3f cFrom;
            final Vector3f cTo;

            // 3-comparison method so that cFrom is the min components and cTo the max
            if (r1.x < r2.x) {
                if (r1.y < r2.y) {
                    if (r1.z < r2.z) {
                        cFrom = r1;
                        cTo = r2;
                    }
                    else {
                        cFrom = new Vector3f(r1.x, r1.y, r2.z);
                        cTo = new Vector3f(r2.x, r2.y, r1.z);
                    }
                }
                else {
                    if (r1.z < r2.z) {
                        cFrom = new Vector3f(r1.x, r2.y, r1.z);
                        cTo = new Vector3f(r2.x, r1.y, r2.z);
                    }
                    else {
                        cFrom = new Vector3f(r1.x, r2.y, r2.z);
                        cTo = new Vector3f(r2.x, r1.y, r1.z);
                    }
                }
            }
            else {
                if (r1.y < r2.y) {
                    if (r1.z < r2.z) {
                        cFrom = new Vector3f(r2.x, r1.y, r1.z);
                        cTo = new Vector3f(r1.x, r2.y, r2.z);
                    }
                    else {
                        cFrom = new Vector3f(r2.x, r1.y, r2.z);
                        cTo = new Vector3f(r1.x, r2.y, r1.z);
                    }
                }
                else {
                    if (r1.z < r2.z) {
                        cFrom = new Vector3f(r2.x, r2.y, r1.z);
                        cTo = new Vector3f(r1.x, r1.y, r2.z);
                    }
                    else {
                        cFrom = r2;
                        cTo = r1;
                    }
                }
            }
            colBoxes[colIdx++] = cFrom;
            colBoxes[colIdx++] = cTo;
            this.generateQuads(e, sidedQuadStore, baseRot, isTransparent);
        }

        for (int i = 1; i < data.count(); ++i) {
            // noinspection unchecked
            final ArrayList<ModelDeserializer.ModelElement> part = parts[i];
            final var partRot = data.getAffineMatrix(i);
            int cullIdx = Integer.MAX_VALUE;

            // Find the first element that collides with a collision Box
            for (ModelDeserializer.ModelElement e : part) {
                final var eID = Integer.parseInt(e.name().split(":")[2]);
                if (eID > cullIdx) continue;
                final Vector3f from = e.from();
                final Vector3f to = e.to();

                for (int c = 0; c < (colCount * 2) - 1; c += 2) {
                    final Vector3f cFrom = colBoxes[c];
                    final Vector3f cTo = colBoxes[c + 1];
                    final Vector3f r1 = new Vector3f(from).mulPosition(partRot);
                    final Vector3f r2 = new Vector3f(to).mulPosition(partRot);
                    final Vector3f rFrom;
                    final Vector3f rTo;

                    // 3-comparison method so that rFrom is the min components and rTo the max
                    if (r1.x < r2.x) {
                        if (r1.y < r2.y) {
                            if (r1.z < r2.z) {
                                rFrom = r1;
                                rTo = r2;
                            }
                            else {
                                rFrom = new Vector3f(r1.x, r1.y, r2.z);
                                rTo = new Vector3f(r2.x, r2.y, r1.z);
                            }
                        }
                        else {
                            if (r1.z < r2.z) {
                                rFrom = new Vector3f(r1.x, r2.y, r1.z);
                                rTo = new Vector3f(r2.x, r1.y, r2.z);
                            }
                            else {
                                rFrom = new Vector3f(r1.x, r2.y, r2.z);
                                rTo = new Vector3f(r2.x, r1.y, r1.z);
                            }
                        }
                    }
                    else {
                        if (r1.y < r2.y) {
                            if (r1.z < r2.z) {
                                rFrom = new Vector3f(r2.x, r1.y, r1.z);
                                rTo = new Vector3f(r1.x, r2.y, r2.z);
                            }
                            else {
                                rFrom = new Vector3f(r2.x, r1.y, r2.z);
                                rTo = new Vector3f(r1.x, r2.y, r1.z);
                            }
                        }
                        else {
                            if (r1.z < r2.z) {
                                rFrom = new Vector3f(r2.x, r2.y, r1.z);
                                rTo = new Vector3f(r1.x, r1.y, r2.z);
                            }
                            else {
                                rFrom = r2;
                                rTo = r1;
                            }
                        }
                    }

                    if (rTo.x < cFrom.x || cTo.x < rFrom.x) continue;
                    if (rTo.y < cFrom.y || cTo.y < rFrom.y) continue;
                    if (rTo.z < cFrom.z || cTo.z < rFrom.z) continue;

                    cullIdx = min(cullIdx, eID);
                }
            }

            for (ModelDeserializer.ModelElement e : part) {
                final var eNameParts = e.name().split(":");
                final var eID = Integer.parseInt(eNameParts[2]);
                final boolean isTransparent = eNameParts.length > 3 && eNameParts[3].equals("t");
                if (eID > cullIdx) continue;
                this.generateQuads(e, sidedQuadStore, partRot, isTransparent);
            }
        }

        // Generate gem
        this.generateGem(sidedQuadStore, baseRot);

        return new PileOfQuads(sidedQuadStore, this.display, this.getParticle());
    }

    private void generateGem(HashMap<ModelQuadFacing, ArrayList<ModelQuadView>> sidedQuadStore, Matrix4fc affine) {
        final Vector3f pos = new Vector3f(0.5F, 0.484373F, 0.5F);
        final float offsetY = 0.25F;
        final float offsetXZ = 0.125F;
        // Last element is for easy iteration without conditional
        final Vector3f usedTwice = new Vector3f(pos.x - offsetXZ, pos.y, pos.z - offsetXZ).mulPosition(affine);
        final Vector3f[] vertices = {
            new Vector3f(pos.x, pos.y + offsetY, pos.z).mulPosition(affine),
            new Vector3f(pos.x, pos.y - offsetY, pos.z).mulPosition(affine),
            usedTwice,
            new Vector3f(pos.x + offsetXZ, pos.y, pos.z - offsetXZ).mulPosition(affine),
            new Vector3f(pos.x + offsetXZ, pos.y, pos.z + offsetXZ).mulPosition(affine),
            new Vector3f(pos.x - offsetXZ, pos.y, pos.z + offsetXZ).mulPosition(affine),
            usedTwice
        };
        final ForgeDirection[] dirs = { ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH, ForgeDirection.WEST };

        for (int i = 0; i < 2; ++i) {
            for (int j = 2; j < 6; ++j) {
                final var quad = new ModelQuad();
                if (i == 0) {
                    quad.setX(0, vertices[i].x);
                    quad.setY(0, vertices[i].y);
                    quad.setZ(0, vertices[i].z);

                    quad.setX(1, vertices[j+1].x);
                    quad.setY(1, vertices[j+1].y);
                    quad.setZ(1, vertices[j+1].z);

                    quad.setX(2, vertices[j].x);
                    quad.setY(2, vertices[j].y);
                    quad.setZ(2, vertices[j].z);
                    // This is a triangle, v2=v3 for degenerate quad
                    quad.setX(3, vertices[j].x);
                    quad.setY(3, vertices[j].y);
                    quad.setZ(3, vertices[j].z);
                }
                else {
                    quad.setX(0, vertices[i].x);
                    quad.setY(0, vertices[i].y);
                    quad.setZ(0, vertices[i].z);

                    quad.setX(1, vertices[j].x);
                    quad.setY(1, vertices[j].y);
                    quad.setZ(1, vertices[j].z);

                    quad.setX(2, vertices[j+1].x);
                    quad.setY(2, vertices[j+1].y);
                    quad.setZ(2, vertices[j+1].z);
                    // This is a triangle, v2=v3 for degenerate quad
                    quad.setX(3, vertices[j+1].x);
                    quad.setY(3, vertices[j+1].y);
                    quad.setZ(3, vertices[j+1].z);
                }
                // These are divided by 16 during bakeSprite
                quad.setTexU(0, 8.0F);
                quad.setTexV(0, 0.0F);
                quad.setTexU(1, 0.0F);
                quad.setTexV(1, 16.0F);
                quad.setTexU(2, 16.0F);
                quad.setTexV(2, 16.0F);
                quad.setTexU(3, 16.0F);
                quad.setTexV(3, 16.0F);

                quad.setEmissiveness(240);
                quad.setDirectionalShading(false);
                quad.setHasAmbientOcclusion(false);
                quad.setLightFace(ModelQuadFacing.fromForgeDir(dirs[j - 2]));
                quad.setTransparent();

                this.bakeSprite(quad, "luxaetheria:models/crystal");

                ModelQuadFacing cullFace = ModelQuadFacing.fromForgeDir(ForgeDirection.UNKNOWN);
                sidedQuadStore.computeIfAbsent(cullFace, d -> new ArrayList<>()).add(quad);
            }
        }
    }
}
