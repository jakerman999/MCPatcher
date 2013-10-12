package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.biome.BiomeAPI;
import net.minecraft.src.BiomeGenBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract class ColorMapBase {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);

    static final int DEFAULT_HEIGHT = 64;

    static final class Blended implements IColorMap {
        private final IColorMap parent;
        private final int[][] offset;
        private final float[] weight;
        private final float[] lastColor = new float[3];

        Blended(IColorMap parent, int blendRadius) {
            this.parent = parent;
            List<int[]> blendOffset = new ArrayList<int[]>();
            List<Float> blendWeight = new ArrayList<Float>();
            float blendScale = 0.0f;
            for (int r = 0; r <= blendRadius; r++) {
                if (r == 0) {
                    blendScale += addSample(blendOffset, blendWeight, 0, 0);
                } else {
                    switch (r % 8) {
                        case 1:
                            blendScale += addSamples(blendOffset, blendWeight, r, 0, 1);
                            break;

                        case 2:
                            blendScale += addSamples(blendOffset, blendWeight, r, 1, 1);
                            break;

                        case 3:
                        case 4:
                            blendScale += addSamples(blendOffset, blendWeight, r, 1, 2);
                            break;

                        case 5:
                        case 6:
                            blendScale += addSamples(blendOffset, blendWeight, r, 1, 3);
                            break;

                        case 7:
                        default:
                            blendScale += addSamples(blendOffset, blendWeight, r, 2, 3);
                            break;
                    }
                }
            }
            offset = blendOffset.toArray(new int[blendOffset.size()][]);
            this.weight = new float[blendWeight.size()];
            for (int i = 0; i < blendWeight.size(); i++) {
                this.weight[i] = blendWeight.get(i) / blendScale;
            }
        }

        private static float addSample(List<int[]> blendOffset, List<Float> blendWeight, int di, int dk) {
            float weight = (float) Math.pow(1.0f + Math.max(Math.abs(di), Math.abs(dk)), -0.5);
            blendOffset.add(new int[]{di, dk});
            blendWeight.add(weight);
            return weight;
        }

        private static float addSamples(List<int[]> blendOffset, List<Float> blendWeight, int r, int num, int denom) {
            int s = num * r / denom;
            float sum = 0.0f;
            if (r % 2 == 0) {
                r ^= s;
                s ^= r;
                r ^= s;
            }
            sum += addSample(blendOffset, blendWeight, r, s);
            sum += addSample(blendOffset, blendWeight, -s, r);
            sum += addSample(blendOffset, blendWeight, -r, -s);
            sum += addSample(blendOffset, blendWeight, s, -r);
            return sum;
        }

        @Override
        public String toString() {
            return parent.toString();
        }

        @Override
        public boolean isHeightDependent() {
            return parent.isHeightDependent();
        }

        @Override
        public int getColorMultiplier() {
            return parent.getColorMultiplier();
        }

        @Override
        public int getColorMultiplier(int i, int j, int k) {
            return Colorizer.float3ToInt(getColorMultiplierF(i, j, k));
        }

        @Override
        public float[] getColorMultiplierF(int i, int j, int k) {
            lastColor[0] = 0.0f;
            lastColor[1] = 0.0f;
            lastColor[2] = 0.0f;
            for (int n = 0; n < weight.length; n++) {
                int[] offset = this.offset[n];
                float weight = this.weight[n];
                float[] tmpColor = parent.getColorMultiplierF(i + offset[0], j, k + offset[1]);
                lastColor[0] += tmpColor[0] * weight;
                lastColor[1] += tmpColor[1] * weight;
                lastColor[2] += tmpColor[2] * weight;
            }
            return lastColor;
        }
    }

    static final class Cached implements IColorMap {
        private final IColorMap parent;

        private int lastI = Integer.MIN_VALUE;
        private int lastJ = Integer.MIN_VALUE;
        private int lastK = Integer.MIN_VALUE;
        private int lastColorI;
        private float[] lastColorF;

        Cached(IColorMap parent) {
            this.parent = parent;
        }

        @Override
        public String toString() {
            return parent.toString();
        }

        @Override
        public boolean isHeightDependent() {
            return parent.isHeightDependent();
        }

        @Override
        public int getColorMultiplier() {
            return parent.getColorMultiplier();
        }

        @Override
        public int getColorMultiplier(int i, int j, int k) {
            if (i != lastI || j != lastJ || k != lastK) {
                lastColorI = parent.getColorMultiplier(i, j, k);
                lastI = i;
                lastJ = j;
                lastK = k;
            }
            return lastColorI;
        }

        @Override
        public float[] getColorMultiplierF(int i, int j, int k) {
            if (i != lastI || j != lastJ || k != lastK) {
                lastColorF = parent.getColorMultiplierF(i, j, k);
                lastI = i;
                lastJ = j;
                lastK = k;
            }
            return lastColorF;
        }
    }

    static final class Smoothed implements IColorMap {
        private final IColorMap parent;
        private final float smoothTime;

        private final float[] lastColor = new float[3];
        private long lastTime;

        Smoothed(IColorMap parent, float smoothTime) {
            this.parent = parent;
            this.smoothTime = smoothTime;
        }

        @Override
        public String toString() {
            return parent.toString();
        }

        @Override
        public boolean isHeightDependent() {
            return parent.isHeightDependent();
        }

        @Override
        public int getColorMultiplier() {
            return parent.getColorMultiplier();
        }

        @Override
        public int getColorMultiplier(int i, int j, int k) {
            return Colorizer.float3ToInt(getColorMultiplierF(i, j, k));
        }

        @Override
        public float[] getColorMultiplierF(int i, int j, int k) {
            float[] currentColor = parent.getColorMultiplierF(i, j, k);
            long now = System.currentTimeMillis();
            if (lastTime == 0L) {
                lastColor[0] = currentColor[0];
                lastColor[1] = currentColor[1];
                lastColor[2] = currentColor[2];
            } else {
                float r = Colorizer.clamp((float) (now - lastTime) / smoothTime);
                float s = 1.0f - r;
                lastColor[0] = r * currentColor[0] + s * lastColor[0];
                lastColor[1] = r * currentColor[1] + s * lastColor[1];
                lastColor[2] = r * currentColor[2] + s * lastColor[2];
            }
            lastTime = now;
            return lastColor;
        }
    }

    static final class Chunked implements IColorMap {
        private static final int I_MASK = ~0xf;
        private static final int K_MASK = ~0xf;

        private static final int I_SIZE = 17;
        private static final int J_SIZE = 2;
        private static final int K_SIZE = 17;
        private static final int IK_SIZE = I_SIZE * K_SIZE;
        private static final int IJK_SIZE = I_SIZE * J_SIZE * K_SIZE;

        private final IColorMap parent;

        private int baseI = Integer.MIN_VALUE;
        private int baseJ = Integer.MIN_VALUE;
        private int baseK = Integer.MIN_VALUE;
        private int baseOffset;

        private final float[][] data = new float[IJK_SIZE][];
        private final float[][] data1 = new float[IJK_SIZE][3];

        private long lastCC = System.currentTimeMillis();
        private int calls;
        private int miss1;
        private int miss2;
        private int miss3;

        Chunked(IColorMap parent) {
            this.parent = parent;
        }

        @Override
        public String toString() {
            return parent.toString();
        }

        @Override
        public boolean isHeightDependent() {
            return parent.isHeightDependent();
        }

        @Override
        public int getColorMultiplier() {
            return parent.getColorMultiplier();
        }

        @Override
        public int getColorMultiplier(int i, int j, int k) {
            return Colorizer.float3ToInt(getColorMultiplierF(i, j, k));
        }

        @Override
        public float[] getColorMultiplierF(int i, int j, int k) {
            int offset = getChunkOffset(i, j, k);
            calls++;
            if (offset < 0) {
                setChunkBase(i, j, k);
                offset = getChunkOffset(i, j, k);
                miss1++;
            }
            float[] color = data[offset];
            if (color == null) {
                color = data[offset] = data1[offset];
                copy3f(color, parent.getColorMultiplierF(i, j, k));
                miss3++;
            }
            long now = System.currentTimeMillis();
            if (now - lastCC > 5000L) {
                logger.finer("%s: calls: %d, miss chunk: %.2f%%, miss sheet: %.2f%%, miss block: %.2f%%",
                    this,
                    calls,
                    100.0f * (float) miss1 / (float) calls,
                    100.0f * (float) miss2 / (float) calls,
                    100.0f * (float) miss3 / (float) calls
                );
                lastCC = now;
            }
            return color;
        }

        private static void copy3f(float[] dst, float[] src) {
            dst[0] = src[0];
            dst[1] = src[1];
            dst[2] = src[2];
        }

        private void setChunkBase(int i, int j, int k) {
            baseI = i & I_MASK;
            baseJ = j;
            baseK = k & K_MASK;
            baseOffset = 0;
            Arrays.fill(data, null);
        }

        private int getChunkOffset(int i, int j, int k) {
            i -= baseI;
            j -= baseJ;
            k -= baseK;
            if (j >= 0 && j <= J_SIZE && k >= 0 && k < K_SIZE && i >= 0 && i < I_SIZE) {
                if (j == J_SIZE) {
                    j--;
                    baseJ++;
                    miss2++;
                    Arrays.fill(data, baseOffset, IJK_SIZE, null);
                    baseOffset ^= IK_SIZE;
                }
                return (baseOffset + j * IK_SIZE + k * I_SIZE + i) % IJK_SIZE;
            } else {
                return -1;
            }
        }
    }

    static final class Outer implements IColorMap {
        private final IColorMap parent;
        private final boolean isHeightDependent;
        private final int mapDefault;

        Outer(IColorMap parent) {
            this.parent = parent;
            isHeightDependent = parent.isHeightDependent();
            mapDefault = parent.getColorMultiplier();
        }

        @Override
        public String toString() {
            return parent.toString();
        }

        @Override
        public boolean isHeightDependent() {
            return parent.isHeightDependent();
        }

        @Override
        public int getColorMultiplier() {
            return mapDefault;
        }

        @Override
        public int getColorMultiplier(int i, int j, int k) {
            if (!isHeightDependent) {
                j = DEFAULT_HEIGHT;
            }
            return parent.getColorMultiplier(i, j, k);
        }

        @Override
        public float[] getColorMultiplierF(int i, int j, int k) {
            if (!isHeightDependent) {
                j = DEFAULT_HEIGHT;
            }
            return parent.getColorMultiplierF(i, j, k);
        }
    }
}
