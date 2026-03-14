package com.ruling_0.luxaetheria.client.model;

import static com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer.ModelElement.Rotation.NOOP;
import static com.gtnewhorizon.gtnhlib.client.model.loading.ModelRegistry.MODEL_LOGGER;
import static java.lang.Math.min;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import net.minecraftforge.common.util.ForgeDirection;

import com.google.common.base.Objects;
import com.gtnewhorizon.gtnhlib.client.model.baked.BakedModel;
import com.gtnewhorizon.gtnhlib.client.model.baked.PileOfQuads;
import com.gtnewhorizon.gtnhlib.client.model.loading.ModelDeserializer;
import com.gtnewhorizon.gtnhlib.client.model.unbaked.JSONModel;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuad;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.ModelQuadView;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.model.quad.properties.ModelQuadFacing;
import com.gtnewhorizon.gtnhlib.client.renderer.cel.util.MathUtil;

import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector3i;
import org.joml.Vector4f;

public class ModelAetherRelay extends JSONModel {

    public ModelAetherRelay(JSONModel model) {
        super(model);
    }

    public static final class RelayBakeData {

        public enum RelayType {
            RELAY,
            SPLITTER
        }

        private final Matrix4fc[] matrices;
        private final RelayType relayType;
        private final float gemRotation;

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

        // spotless:off
        private static Matrix4f computeArmMatrix(Vector3i target, Vector3i pos, Vector3fc orig,
                                                  ForgeDirection forgeDir) {
            final Vector3f dir = new Vector3f(target.sub(pos)).normalize();

            final int axis;
            final boolean negateDir, negateSin, dirRotFirst;
            switch (forgeDir) {
                case DOWN, UNKNOWN -> { axis = 1; negateDir = false; negateSin = false; dirRotFirst = false; }
                case UP            -> { axis = 1; negateDir = true;  negateSin = false; dirRotFirst = false; }
                case NORTH         -> { axis = 2; negateDir = false; negateSin = false; dirRotFirst = true;  }
                case SOUTH         -> { axis = 2; negateDir = true;  negateSin = true;  dirRotFirst = true;  }
                case WEST          -> { axis = 0; negateDir = false; negateSin = false; dirRotFirst = true;  }
                case EAST          -> { axis = 0; negateDir = true;  negateSin = true;  dirRotFirst = true;  }
                default -> throw new IllegalArgumentException("Unexpected direction: " + forgeDir);
            }
            // spotless:on

            if (negateDir) dir.setComponent(axis, -dir.get(axis));

            final Vector3f dirAZ = new Vector3f(dir);
            dirAZ.setComponent(axis, 0.0f);
            dirAZ.normalize();

            float sinAZ = new Vector3f(orig).cross(dirAZ).get(axis);
            if (negateSin) sinAZ = -sinAZ;
            final float angAZ = sinAZ == 0 ? (float) Math.acos(orig.dot(dirAZ)) :
                (float) Math.acos(orig.dot(dirAZ)) * Math.signum(sinAZ);
            final float angEL = (float) Math.acos(dirAZ.dot(dir)) * Math.signum(dir.get(axis));

            final var mat = new Matrix4f().translate(0.5f, 0.5f, 0.5f);
            if (dirRotFirst) {
                mat.rotate(getDirRot(forgeDir)).rotateY(angAZ).rotateX(angEL);
            }
            else {
                mat.rotateY(angAZ).rotateX(angEL).rotate(getDirRot(forgeDir));
            }
            return mat.translate(-0.5f, -0.5f, -0.5f);
        }

        public RelayBakeData(Vector3i pos, Vector3i[] targets, int meta, RelayType relayType) {
            this.relayType = relayType;
            this.gemRotation = relayType == RelayType.SPLITTER ? computeGemRotation(pos, targets) : 0.0f;
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

            for (int i = 1; i < this.matrices.length; ++i) {
                this.matrices[i] = computeArmMatrix(targets[i - 1], pos, orig, forgeDir);
            }
        }

        public Matrix4fc getAffineMatrix(int i) {
            return this.matrices[i];
        }

        public int count() {
            return this.matrices.length;
        }

        public RelayType relayType() {
            return this.relayType;
        }

        public float gemRotation() {
            return this.gemRotation;
        }

        private static float computeGemRotation(Vector3i pos, Vector3i[] targets) {
            if (targets.length == 0) return 0.0f;

            final float TWO_PI = (float) (2 * Math.PI);
            float[] angles = new float[targets.length];
            for (int i = 0; i < targets.length; i++) {
                float dx = targets[i].x - pos.x;
                float dz = targets[i].z - pos.z;
                float a = (float) Math.atan2(dx, dz);
                angles[i] = a < 0 ? a + TWO_PI : a;
            }
            Arrays.sort(angles);

            int largestIdx = angles.length - 1;
            float largestGap = angles[0] + TWO_PI - angles[angles.length - 1];
            for (int i = 0; i < angles.length - 1; i++) {
                float gap = angles[i + 1] - angles[i];
                if (gap > largestGap) {
                    largestGap = gap;
                    largestIdx = i;
                }
            }
            float arcStart = angles[(largestIdx + 1) % angles.length];

            // Rotate so a face normal points along the middle direction.
            // Default face 0 normal points +Z, so rotate by middleAngle from +Z.
            return arcStart + (TWO_PI - largestGap) / 2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RelayBakeData other)) return false;
            return this.relayType == other.relayType && Float.compare(this.gemRotation, other.gemRotation) == 0 &&
                Arrays.equals(this.matrices, other.matrices);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(this.matrices);
            result = 31 * result + this.relayType.hashCode();
            result = 31 * result + Float.hashCode(this.gemRotation);
            return result;
        }
    }

    private static void componentMinMax(Vector3f a, Vector3f b, Vector3f min, Vector3f max) {
        min.set(a);
        max.set(b);
        if (a.x > b.x) {
            min.x = b.x;
            max.x = a.x;
        }
        if (a.y > b.y) {
            min.y = b.y;
            max.y = a.y;
        }
        if (a.z > b.z) {
            min.z = b.z;
            max.z = a.z;
        }
    }

    private void addGemQuad(ModelQuad quad, ForgeDirection lightFace,
                            HashMap<ModelQuadFacing, ArrayList<ModelQuadView>> sidedQuadStore) {
        quad.setEmissiveness(0);
        quad.setDirectionalShading(true);
        quad.setHasAmbientOcclusion(true);
        quad.setLightFace(ModelQuadFacing.fromForgeDir(lightFace));
        quad.setColorIndex(-1);
        quad.setTransparent();
        this.bakeSprite(quad, "luxaetheria:models/crystal");
        sidedQuadStore.computeIfAbsent(ModelQuadFacing.fromForgeDir(ForgeDirection.UNKNOWN), d -> new ArrayList<>())
            .add(quad);
    }

    protected void generateQuads(ModelDeserializer.ModelElement e,
                                 HashMap<ModelQuadFacing, ArrayList<ModelQuadView>> sidedQuadStore, Matrix4fc affine,
                                 boolean isTransparent, int tintIndex) {
        final Matrix4f rot = (e.rotation() == null) ? NOOP.getAffineMatrix() : e.rotation().getAffineMatrix();
        for (ModelDeserializer.ModelElement.Face f : e.faces()) {

            // Assign vertexes
            final var quad = new ModelQuad();
            for (int i = 0; i < 4; ++i) {
                final Vector3f vert = mapSideToVertex(e.from(), e.to(), i, f.name()).mulPosition(rot)
                    .mulPosition(affine);
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

            quad.setColorIndex(tintIndex >= 0 ? tintIndex : f.tintIndex());

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
        final var parts = new ArrayList<ArrayList<ModelDeserializer.ModelElement>>(data.count());
        int colCount = 0;
        for (int i = 0; i < data.count(); ++i) {
            parts.add(new ArrayList<>());
        }

        for (ModelDeserializer.ModelElement e : this.elements) {
            final int partID = Integer.parseInt(e.name().split(":")[1]);
            // partID 0 always rendered
            if (partID > data.count() - 1) continue;
            parts.get(partID).add(e);
            if (partID == 0) ++colCount;
        }
        final Vector3f[] colBoxes = new Vector3f[colCount * 2];
        int colIdx = 0;

        // i=0 handles base and collisions
        final ArrayList<ModelDeserializer.ModelElement> base = parts.get(0);
        final var baseRot = data.getAffineMatrix(0);
        for (ModelDeserializer.ModelElement e : base) {
            final var eNameParts = e.name().split(":");
            final boolean isTransparent = eNameParts.length > 3 && eNameParts[3].equals("t");
            final Vector3f r1 = new Vector3f(e.from()).mulPosition(baseRot);
            final Vector3f r2 = new Vector3f(e.to()).mulPosition(baseRot);
            final var cFrom = new Vector3f();
            final var cTo = new Vector3f();
            componentMinMax(r1, r2, cFrom, cTo);
            colBoxes[colIdx++] = cFrom;
            colBoxes[colIdx++] = cTo;
            this.generateQuads(e, sidedQuadStore, baseRot, isTransparent, -1);
        }

        for (int i = 1; i < data.count(); ++i) {
            final ArrayList<ModelDeserializer.ModelElement> part = parts.get(i);
            final var partRot = data.getAffineMatrix(i);
            int cullIdx = Integer.MAX_VALUE;

            // Find the first element that collides with a collision box
            for (ModelDeserializer.ModelElement e : part) {
                final var eID = Integer.parseInt(e.name().split(":")[2]);
                if (eID > cullIdx) continue;
                final Vector3f r1 = new Vector3f(e.from()).mulPosition(partRot);
                final Vector3f r2 = new Vector3f(e.to()).mulPosition(partRot);
                final var rFrom = new Vector3f();
                final var rTo = new Vector3f();
                componentMinMax(r1, r2, rFrom, rTo);

                for (int c = 0; c < (colCount * 2) - 1; c += 2) {
                    final Vector3f cFrom = colBoxes[c];
                    final Vector3f cTo = colBoxes[c + 1];

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

                int tintIndex = -1;
                if (eNameParts[0].equals("lens") && eID != 0) {
                    if (data.relayType() == RelayBakeData.RelayType.SPLITTER) {
                        tintIndex = i - 1;
                    }
                    else {
                        tintIndex = 0;
                    }
                }
                this.generateQuads(e, sidedQuadStore, partRot, isTransparent, tintIndex);
            }
        }

        // Generate gem
        if (data.relayType() == RelayBakeData.RelayType.SPLITTER) {
            this.generateTriPrismGem(sidedQuadStore, baseRot, data.gemRotation());
        }
        else {
            this.generateGem(sidedQuadStore, baseRot);
        }

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
            usedTwice };
        final ForgeDirection[] dirs = { ForgeDirection.NORTH, ForgeDirection.EAST, ForgeDirection.SOUTH,
            ForgeDirection.WEST };

        for (int i = 0; i < 2; ++i) {
            for (int j = 2; j < 6; ++j) {
                final var quad = new ModelQuad();
                quad.setX(0, vertices[i].x);
                quad.setY(0, vertices[i].y);
                quad.setZ(0, vertices[i].z);
                if (i == 0) {
                    quad.setX(1, vertices[j + 1].x);
                    quad.setY(1, vertices[j + 1].y);
                    quad.setZ(1, vertices[j + 1].z);

                    quad.setX(2, vertices[j].x);
                    quad.setY(2, vertices[j].y);
                    quad.setZ(2, vertices[j].z);
                    // This is a triangle, v2=v3 for degenerate quad
                    quad.setX(3, vertices[j].x);
                    quad.setY(3, vertices[j].y);
                    quad.setZ(3, vertices[j].z);
                }
                else {
                    quad.setX(1, vertices[j].x);
                    quad.setY(1, vertices[j].y);
                    quad.setZ(1, vertices[j].z);

                    quad.setX(2, vertices[j + 1].x);
                    quad.setY(2, vertices[j + 1].y);
                    quad.setZ(2, vertices[j + 1].z);
                    // This is a triangle, v2=v3 for degenerate quad
                    quad.setX(3, vertices[j + 1].x);
                    quad.setY(3, vertices[j + 1].y);
                    quad.setZ(3, vertices[j + 1].z);
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

                this.addGemQuad(quad, dirs[j - 2], sidedQuadStore);
            }
        }
    }

    private void generateTriPrismGem(HashMap<ModelQuadFacing, ArrayList<ModelQuadView>> sidedQuadStore,
                                     Matrix4fc affine, float gemRotation) {
        final Vector3f center = new Vector3f(0.5F, 0.484373F, 0.5F);
        final float halfHeight = 0.25F;
        final float R = 0.125F;
        final float sin60 = (float) Math.sin(Math.toRadians(60));

        final Matrix4f combinedAffine = new Matrix4f()
            .translate(0.5f, 0.5f, 0.5f)
            .rotateY(gemRotation)
            .translate(-0.5f, -0.5f, -0.5f)
            .mul(affine);

        // Equilateral triangle vertices in XZ.
        // v0: north, v1: SE, v2: SW.
        // Face between v1-v2 has outward normal +Z; after gemRotation
        // it aligns parallel to the middle beam direction.
        final float topY = center.y + halfHeight;
        final float botY = center.y - halfHeight;

        final Vector3f[] top = {
            new Vector3f(center.x, topY, center.z - R).mulPosition(combinedAffine),
            new Vector3f(center.x + R * sin60, topY, center.z + R * 0.5f).mulPosition(combinedAffine),
            new Vector3f(center.x - R * sin60, topY, center.z + R * 0.5f).mulPosition(combinedAffine), };
        final Vector3f[] bot = {
            new Vector3f(center.x, botY, center.z - R).mulPosition(combinedAffine),
            new Vector3f(center.x + R * sin60, botY, center.z + R * 0.5f).mulPosition(combinedAffine),
            new Vector3f(center.x - R * sin60, botY, center.z + R * 0.5f).mulPosition(combinedAffine), };

        // 3 rectangular side faces
        final int[][] edges = { { 1, 2 }, { 2, 0 }, { 0, 1 } };
        final ForgeDirection[] faceDirs = { ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST };

        for (int f = 0; f < 3; f++) {
            final int a = edges[f][0], b = edges[f][1];
            final var quad = new ModelQuad();
            quad.setX(0, top[b].x);
            quad.setY(0, top[b].y);
            quad.setZ(0, top[b].z);
            quad.setX(1, bot[b].x);
            quad.setY(1, bot[b].y);
            quad.setZ(1, bot[b].z);
            quad.setX(2, bot[a].x);
            quad.setY(2, bot[a].y);
            quad.setZ(2, bot[a].z);
            quad.setX(3, top[a].x);
            quad.setY(3, top[a].y);
            quad.setZ(3, top[a].z);

            quad.setTexU(0, 0.0F);
            quad.setTexV(0, 0.0F);
            quad.setTexU(1, 0.0F);
            quad.setTexV(1, 16.0F);
            quad.setTexU(2, 16.0F);
            quad.setTexV(2, 16.0F);
            quad.setTexU(3, 16.0F);
            quad.setTexV(3, 0.0F);

            this.addGemQuad(quad, faceDirs[f], sidedQuadStore);
        }

        // Triangle caps (top: CCW winding, bottom: CW winding)
        final Vector3f[][] capVerts = { top, bot };
        final int[][] capWindings = { { 0, 2, 1 }, { 0, 1, 2 } };
        final ForgeDirection[] capDirs = { ForgeDirection.UP, ForgeDirection.DOWN };

        for (int c = 0; c < 2; c++) {
            final var quad = new ModelQuad();
            final var v = capVerts[c];
            final var w = capWindings[c];
            quad.setX(0, v[w[0]].x);
            quad.setY(0, v[w[0]].y);
            quad.setZ(0, v[w[0]].z);
            quad.setX(1, v[w[1]].x);
            quad.setY(1, v[w[1]].y);
            quad.setZ(1, v[w[1]].z);
            quad.setX(2, v[w[2]].x);
            quad.setY(2, v[w[2]].y);
            quad.setZ(2, v[w[2]].z);
            // Degenerate quad: v3=v2
            quad.setX(3, v[w[2]].x);
            quad.setY(3, v[w[2]].y);
            quad.setZ(3, v[w[2]].z);

            quad.setTexU(0, 8.0F);
            quad.setTexV(0, 0.0F);
            quad.setTexU(1, 0.0F);
            quad.setTexV(1, 16.0F);
            quad.setTexU(2, 16.0F);
            quad.setTexV(2, 16.0F);
            quad.setTexU(3, 16.0F);
            quad.setTexV(3, 16.0F);

            this.addGemQuad(quad, capDirs[c], sidedQuadStore);
        }
    }
}
