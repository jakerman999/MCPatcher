package com.prupe.mcpatcher.mal.block;

import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.Icon;

// Shared by both CTM and Custom Colors.
public class RenderBlocksUtils {
    private static final boolean enableBetterGrass = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "grass", false);

    private static final Block grassBlock = BlockAPI.getFixedBlock("minecraft:grass");
    private static final Block snowBlock = BlockAPI.getFixedBlock("minecraft:snow_layer");
    private static final Block craftedSnowBlock = BlockAPI.getFixedBlock("minecraft:snow");

    private static final boolean[] useColorMultiplier = new boolean[]{true, true, true, true, true, true};
    private static final boolean[] useBetterGrass = new boolean[6];
    private static final float[][] nonAOMultipliers = new float[6][3];

    public static final float[] AO_BASE = new float[]{0.5f, 1.0f, 0.8f, 0.8f, 0.6f, 0.6f};

    public static void setupColorMultiplier(Block block, IBlockAccess blockAccess, int i, int j, int k,
                                            boolean haveOverrideTexture, float r, float g, float b) {
        useBetterGrass[2] = false;
        useBetterGrass[3] = false;
        useBetterGrass[4] = false;
        useBetterGrass[5] = false;
        if (haveOverrideTexture || !RenderPassAPI.instance.useColorMultiplierThisPass(block)) {
            useColorMultiplier[0] = false;
            useColorMultiplier[2] = false;
            useColorMultiplier[3] = false;
            useColorMultiplier[4] = false;
            useColorMultiplier[5] = false;
        } else if (block == grassBlock) {
            useColorMultiplier[0] = false;
            if (enableBetterGrass) {
                Block topBlock = BlockAPI.getBlockAt(blockAccess, i, j + 1, k);
                if (topBlock == snowBlock || topBlock == craftedSnowBlock) {
                    useColorMultiplier[2] = false;
                    useColorMultiplier[3] = false;
                    useColorMultiplier[4] = false;
                    useColorMultiplier[5] = false;
                } else {
                    j--;
                    useBetterGrass[2] = useColorMultiplier[2] = block == BlockAPI.getBlockAt(blockAccess, i, j, k - 1);
                    useBetterGrass[3] = useColorMultiplier[3] = block == BlockAPI.getBlockAt(blockAccess, i, j, k + 1);
                    useBetterGrass[4] = useColorMultiplier[4] = block == BlockAPI.getBlockAt(blockAccess, i - 1, j, k);
                    useBetterGrass[5] = useColorMultiplier[5] = block == BlockAPI.getBlockAt(blockAccess, i + 1, j, k);
                }
            } else {
                useColorMultiplier[2] = false;
                useColorMultiplier[3] = false;
                useColorMultiplier[4] = false;
                useColorMultiplier[5] = false;
            }
        } else {
            useColorMultiplier[0] = true;
            useColorMultiplier[2] = true;
            useColorMultiplier[3] = true;
            useColorMultiplier[4] = true;
            useColorMultiplier[5] = true;
        }
        if (!isAmbientOcclusionEnabled() || BlockAPI.getBlockLightValue(block) != 0) {
            setupColorMultiplier(0, r, g, b);
            setupColorMultiplier(1, r, g, b);
            setupColorMultiplier(2, r, g, b);
            setupColorMultiplier(3, r, g, b);
            setupColorMultiplier(4, r, g, b);
            setupColorMultiplier(5, r, g, b);
        }
    }

    private static void setupColorMultiplier(int face, float r, float g, float b) {
        float[] mult = nonAOMultipliers[face];
        float ao = AO_BASE[face];
        mult[0] = ao;
        mult[1] = ao;
        mult[2] = ao;
        if (useColorMultiplier[face]) {
            mult[0] *= r;
            mult[1] *= g;
            mult[2] *= b;
        }
    }

    public static boolean useColorMultiplier(int face) {
        return useColorMultiplier[face % 6];
    }

    public static float getColorMultiplierRed(int face) {
        return nonAOMultipliers[face % 6][0];
    }

    public static float getColorMultiplierGreen(int face) {
        return nonAOMultipliers[face % 6][1];
    }

    public static float getColorMultiplierBlue(int face) {
        return nonAOMultipliers[face % 6][2];
    }

    public static boolean isBetterGrass(Block block, IBlockAccess blockAccess, int i, int j, int k, int face) {
        if (enableBetterGrass && face > 1) {
            Block topBlock = BlockAPI.getBlockAt(blockAccess, i, j + 1, k);
            if (topBlock != snowBlock && topBlock != craftedSnowBlock) {
                j--;
                switch (face) {
                    case 2:
                        return block == BlockAPI.getBlockAt(blockAccess, i, j, k - 1);

                    case 3:
                        return block == BlockAPI.getBlockAt(blockAccess, i, j, k + 1);

                    case 4:
                        return block == BlockAPI.getBlockAt(blockAccess, i - 1, j, k);

                    case 5:
                        return block == BlockAPI.getBlockAt(blockAccess, i + 1, j, k);

                    default:
                        break;
                }
            }
        }
        return false;
    }

    public static boolean isAmbientOcclusionEnabled() {
        return Minecraft.isAmbientOcclusionEnabled();
    }

    public static Icon getSecondaryIcon(Icon icon, int face) {
        return useBetterGrass[face] ? null : icon;
    }
}
