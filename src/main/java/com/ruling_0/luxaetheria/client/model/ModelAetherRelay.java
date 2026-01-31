package com.ruling_0.luxaetheria.client.model;

import static com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.ModelElement.Rotation.NOOP;
import static com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry.MODEL_LOGGER;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;

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

        private final Matrix4f[] matrices;
        /**
         * Original vector of the arms, pointing North (negative z)
         */
        private static final Vector3f orig = new Vector3f(0.0f, 0.0f, -1.0f);

        public RelayBakeData(Vector3i pos, Vector3i[] targets) {
            if (targets.length == 0) {
                this.matrices = new Matrix4f[] { new Matrix4f(), new Matrix4f() };
                return;
            }
            this.matrices = new Matrix4f[targets.length + 1];
            this.matrices[0] = new Matrix4f();
            for (int i = 1; i < this.matrices.length; ++i) {
                final Vector3f dir = new Vector3f(targets[i - 1].sub(pos)).normalize();
                final Vector3f dirXZ = new Vector3f(dir.x, 0.0f, dir.z).normalize();
                final float sXZ = new Vector3f(orig).cross(dirXZ).y;
                final float cXZ = orig.dot(dirXZ);
                final Matrix4f Ry = new Matrix4f(
                    cXZ, 0.0f, -sXZ, 0.0f,
                    0.0f, 1.0f, 0.0f, 0.0f,
                    sXZ, 0.0f, cXZ, 0.0f,
                    0.0f, 0.0f, 0.0f, 1.0f);
                final float sY = new Vector3f(dirXZ).cross(dir).x;
                final float cY = dirXZ.dot(dir);
                final Matrix4f Rx = new Matrix4f(
                    1.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, cY, -sY, 0.0f,
                    0.0f, sY, cY, 0.0f,
                    0.0f, 0.0f, 0.0f, 1.0f);
                final Matrix4f T1 = new Matrix4f(
                    1.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, 1.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 1.0f, 0.0f,
                    0.5f, 0.5f, 0.5f, 1.0f);
                final Matrix4f T2 = new Matrix4f(
                    1.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, 1.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 1.0f, 0.0f,
                    -0.5f, -0.5f, -0.5f, 1.0f);
                this.matrices[i] = new Matrix4f(T1).mul(Ry).mul(Rx).mul(T2);
            }
        }

        public Matrix4fc getAffineMatrix(int i) {
            return this.matrices[i];
        }

        public int count() {
            return this.matrices.length;
        }
    }

    private void generateQuads(ModelDeserializer.ModelElement e, Vector3f from, Vector3f to,
                               HashMap<ModelQuadFacing, ArrayList<ModelQuadView>> sidedQuadStore, Matrix4fc affine,
                               boolean isTransparent) {
        final Matrix4f rot = (e.rotation() == null) ? NOOP.getAffineMatrix() : e.rotation().getAffineMatrix();
        for (ModelDeserializer.ModelElement.Face f : e.faces()) {

            float x = Float.MAX_VALUE;
            float y = Float.MAX_VALUE;
            float z = Float.MAX_VALUE;
            float X = Float.MIN_VALUE;
            float Y = Float.MIN_VALUE;
            float Z = Float.MIN_VALUE;

            // Assign vertexes
            final var quad = new ModelQuad();
            for (int i = 0; i < 4; ++i) {
                final Vector3f vert = mapSideToVertex(from, to, i, f.name()).mulPosition(rot).mulPosition(affine);
                quad.setX(i, vert.x);
                quad.setY(i, vert.y);
                quad.setZ(i, vert.z);

                x = min(x, vert.x);
                y = min(y, vert.y);
                z = min(z, vert.z);
                X = max(X, vert.x);
                Y = max(Y, vert.y);
                Z = max(Z, vert.z);
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
            final Vector3f from = e.from();
            final Vector3f to = e.to();
            colBoxes[colIdx++] = from;
            colBoxes[colIdx++] = to;
            this.generateQuads(e, from, to, sidedQuadStore, baseRot, isTransparent);
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
                    // 3-comparison method so that rFrom is the min components and r2 the max
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

                    cullIdx = Math.min(cullIdx, eID);
                }
            }

            for (ModelDeserializer.ModelElement e : part) {
                final var eNameParts = e.name().split(":");
                final var eID = Integer.parseInt(eNameParts[2]);
                final boolean isTransparent = eNameParts.length > 3 && eNameParts[3].equals("t");
                if (eID > cullIdx) continue;
                this.generateQuads(e, e.from(), e.to(), sidedQuadStore, partRot, isTransparent);
            }
        }
        return new PileOfQuads(sidedQuadStore, this.display, this.getParticle());
    }
}
