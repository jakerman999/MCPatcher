package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import com.prupe.mcpatcher.mal.block.BlockAPI;
import com.prupe.mcpatcher.mal.block.RenderBlocksUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Properties;

public class ColorizeBlock {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);

    private static final boolean enableSmoothBiomes = Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "smoothBiomes", true);
    private static final boolean enableTestColorSmoothing = Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "testColorSmoothing", false);

    private static final ResourceLocation REDSTONE_COLORS = TexturePackAPI.newMCPatcherResourceLocation("colormap/redstone.png");
    private static final ResourceLocation STEM_COLORS = TexturePackAPI.newMCPatcherResourceLocation("colormap/stem.png");
    private static final ResourceLocation PUMPKIN_STEM_COLORS = TexturePackAPI.newMCPatcherResourceLocation("colormap/pumpkinstem.png");
    private static final ResourceLocation MELON_STEM_COLORS = TexturePackAPI.newMCPatcherResourceLocation("colormap/melonstem.png");
    private static final ResourceLocation SWAMPGRASSCOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/swampgrass.png");
    private static final ResourceLocation SWAMPFOLIAGECOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/swampfoliage.png");
    private static final ResourceLocation DEFAULT_GRASSCOLOR = new ResourceLocation("minecraft", "textures/colormap/grass.png");
    private static final ResourceLocation DEFAULT_FOLIAGECOLOR = new ResourceLocation("minecraft", "textures/colormap/foliage.png");
    private static final ResourceLocation PINECOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/pine.png");
    private static final ResourceLocation BIRCHCOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/birch.png");
    private static final ResourceLocation WATERCOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/water.png");

    private static final String PALETTE_BLOCK_KEY = "palette.block.";

    private static Block waterBlock;
    private static Block staticWaterBlock;
    private static Block pumpkinStemBlock;
    private static Block melonStemBlock;

    private static final Map<Block, IColorMap[]> blockColorMaps = new IdentityHashMap<Block, IColorMap[]>(); // bitmaps from palette.block.*
    private static IColorMap waterColorMap;
    private static int lilypadColor; // lilypad
    private static float[][] redstoneColor; // colormap/redstone.png
    private static int[] pumpkinStemColors; // colormap/pumpkinstem.png
    private static int[] melonStemColors; // colormap/melonstem.png

    private static final int blockBlendRadius = Config.getInt(MCPatcherUtils.CUSTOM_COLORS, "blockBlendRadius", 1);

    public static int blockColor;
    public static float[] waterColor;
    public static boolean isSmooth;

    private static final int[][][] FACE_VERTICES = new int[][][]{
        // bottom face (y=0)
        {
            {0, 0, 1}, // top left
            {0, 0, 0}, // bottom left
            {1, 0, 0}, // bottom right
            {1, 0, 1}, // top right
        },
        // top face (y=1)
        {
            {1, 1, 1},
            {1, 1, 0},
            {0, 1, 0},
            {0, 1, 1},
        },
        // north face (z=0)
        {
            {0, 1, 0},
            {1, 1, 0},
            {1, 0, 0},
            {0, 0, 0},
        },
        // south face (z=1)
        {
            {0, 1, 1},
            {0, 0, 1},
            {1, 0, 1},
            {1, 1, 1},
        },
        // west face (x=0)
        {
            {0, 1, 1},
            {0, 1, 0},
            {0, 0, 0},
            {0, 0, 1},
        },
        // east face (x=1)
        {
            {1, 0, 1},
            {1, 0, 0},
            {1, 1, 0},
            {1, 1, 1},
        },

        // bottom face, water (y=0)
        {
            {0, 0, 1}, // top left
            {0, 0, 0}, // bottom left
            {1, 0, 0}, // bottom right
            {1, 0, 1}, // top right
        },
        // top face, water (y=1) cycle by 2
        {
            {0, 1, 0},
            {0, 1, 1},
            {1, 1, 1},
            {1, 1, 0},
        },
        // north face, water (z=0)
        {
            {0, 1, 0},
            {1, 1, 0},
            {1, 0, 0},
            {0, 0, 0},
        },
        // south face, water (z=1) cycle by 1
        {
            {1, 1, 1},
            {0, 1, 1},
            {0, 0, 1},
            {1, 0, 1},
        },
        // west face, water (x=0)
        {
            {0, 1, 1},
            {0, 1, 0},
            {0, 0, 0},
            {0, 0, 1},
        },
        // east face, water (x=1) cycle by 2
        {
            {1, 1, 0},
            {1, 1, 1},
            {1, 0, 1},
            {1, 0, 0},
        },
    };

    static {
        try {
            reset();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    static void reset() {
        waterBlock = BlockAPI.getFixedBlock("minecraft:flowing_water");
        staticWaterBlock = BlockAPI.getFixedBlock("minecraft:water");
        pumpkinStemBlock = BlockAPI.getFixedBlock("minecraft:pumpkin_stem");
        melonStemBlock = BlockAPI.getFixedBlock("minecraft:melon_stem");

        blockColorMaps.clear();
        waterColorMap = null;

        lilypadColor = 0x208030;
        waterColor = new float[]{0.2f, 0.3f, 1.0f};
        redstoneColor = null;
        pumpkinStemColors = null;
        melonStemColors = null;
    }

    static void reloadFoliageColors(Properties properties) {
        IColorMap colorMap = ColorMap.loadColorMap(true, DEFAULT_GRASSCOLOR, SWAMPGRASSCOLOR);
        registerColorMap(colorMap, DEFAULT_GRASSCOLOR, "minecraft:grass minecraft:tallgrass:1,2");
        colorMap = ColorMap.loadColorMap(true, DEFAULT_FOLIAGECOLOR, SWAMPFOLIAGECOLOR);
        registerColorMap(colorMap, DEFAULT_FOLIAGECOLOR, "minecraft:leaves:0,4,8,12 minecraft:vine");
        registerColorMap(PINECOLOR, "minecraft:leaves:1,5,9,13");
        registerColorMap(BIRCHCOLOR, "minecraft:leaves:2,6,10,14");
    }

    private static IColorMap wrapBlockMap(IColorMap map) {
        if (map == null) {
            return null;
        } else {
            map = new ColorMapBase.Blended(map, blockBlendRadius);
            map = new ColorMapBase.Chunked(map);
            map = new ColorMapBase.Outer(map);
            return map;
        }
    }

    static void reloadWaterColors(Properties properties) {
        waterColorMap = registerColorMap(WATERCOLOR, "minecraft:flowing_water minecraft:water");
        if (waterColorMap == null) {
            waterColorMap = new ColorMap.Water();
            registerColorMap(waterColorMap, null, "minecraft:flowing_water minecraft:water");
        } else {
            Colorizer.intToFloat3(waterColorMap.getColorMultiplier(), waterColor);
        }
    }

    static void reloadSwampColors(Properties properties) {
        int[] temp = new int[]{lilypadColor};
        Colorizer.loadIntColor("lilypad", temp, 0);
        lilypadColor = temp[0];
    }

    static void reloadBlockColors(Properties properties) {
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof String)) {
                continue;
            }
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (!key.startsWith(PALETTE_BLOCK_KEY)) {
                continue;
            }
            key = key.substring(PALETTE_BLOCK_KEY.length()).trim();
            ResourceLocation resource = TexturePackAPI.parseResourceLocation(Colorizer.COLOR_PROPERTIES, key);
            if (resource == null) {
                continue;
            }
            registerColorMap(resource, value);
        }
    }

    private static IColorMap registerColorMap(ResourceLocation resource, String idList) {
        IColorMap colorMap = ColorMap.loadColorMap(true, resource);
        if (colorMap == null) {
            return null;
        }
        return registerColorMap(colorMap, resource, idList);
    }

    private static IColorMap registerColorMap(IColorMap colorMap, ResourceLocation resource, String idList) {
        colorMap = wrapBlockMap(colorMap);
        int[] metadata = new int[1];
        for (String idString : idList.split("\\s+")) {
            Block block = BlockAPI.parseBlockAndMetadata(idString, metadata);
            if (block != null) {
                IColorMap[] maps = blockColorMaps.get(block);
                if (maps == null) {
                    maps = new IColorMap[BlockAPI.METADATA_ARRAY_SIZE];
                    blockColorMaps.put(block, maps);
                }
                for (int i = 0; i < maps.length; i++) {
                    if ((metadata[0] & (1 << i)) != 0) {
                        maps[i] = colorMap;
                    }
                }
                if (resource != null) {
                    logger.finer("using %s for block %s, default color %06x",
                        colorMap, BlockAPI.getBlockName(block, metadata[0]), colorMap.getColorMultiplier()
                    );
                }
            }
        }
        return colorMap;
    }

    static void reloadRedstoneColors(Properties properties) {
        int[] rgb = MCPatcherUtils.getImageRGB(TexturePackAPI.getImage(REDSTONE_COLORS));
        if (rgb != null && rgb.length >= 16) {
            redstoneColor = new float[16][];
            for (int i = 0; i < 16; i++) {
                float[] f = new float[3];
                Colorizer.intToFloat3(rgb[i], f);
                redstoneColor[i] = f;
            }
        }
    }

    static void reloadStemColors(Properties properties) {
        int[] stemColors = getStemRGB(STEM_COLORS);
        pumpkinStemColors = getStemRGB(PUMPKIN_STEM_COLORS);
        if (pumpkinStemColors == null) {
            pumpkinStemColors = stemColors;
        }
        melonStemColors = getStemRGB(MELON_STEM_COLORS);
        if (melonStemColors == null) {
            melonStemColors = stemColors;
        }
    }

    private static int[] getStemRGB(ResourceLocation resource) {
        int[] rgb = MCPatcherUtils.getImageRGB(TexturePackAPI.getImage(resource));
        return rgb == null || rgb.length < 8 ? null : rgb;
    }

    private static IColorMap findColorMap(Block block, int metadata) {
        IColorMap[] maps = blockColorMaps.get(block);
        if (maps == null) {
            return null;
        }
        IColorMap colorMap = maps[metadata];
        if (colorMap != null) {
            return colorMap;
        }
        return maps[BlockAPI.NO_METADATA];
    }

    private static IColorMap findColorMap(Block block, IBlockAccess blockAccess, int i, int j, int k) {
        int metadata = blockAccess.getBlockMetadata(i, j, k);
        return findColorMap(block, metadata);
    }

    public static boolean colorizeBlock(Block block) {
        return colorizeBlock(block, BlockAPI.NO_METADATA);
    }

    public static boolean colorizeBlock(Block block, int metadata) {
        IColorMap colorMap = findColorMap(block, metadata);
        if (colorMap == null) {
            return false;
        } else {
            blockColor = colorMap.getColorMultiplier();
            return true;
        }
    }

    public static boolean colorizeBlock(Block block, IBlockAccess blockAccess, int i, int j, int k) {
        IColorMap colorMap = findColorMap(block, blockAccess, i, j, k);
        return colorizeBlock(block, blockAccess, colorMap, i, j, k);
    }

    private static boolean colorizeBlock(Block block, IBlockAccess blockAccess, IColorMap colorMap, int i, int j, int k) {
        if (colorMap == null) {
            return false;
        } else {
            blockColor = colorMap.getColorMultiplier(i, j, k);
            return true;
        }
    }

    public static int getColorMultiplier(Block block, int i, int j, int k) {
        if (colorizeBlock(block, Minecraft.getInstance().theWorld, i, j, k)) {
            return blockColor;
        } else {
            return 0xffffff;
        }
    }

    public static void computeWaterColor() {
        int color = waterColorMap == null ? waterBlock.getBlockColor() : waterColorMap.getColorMultiplier();
        Colorizer.setColorF(color);
    }

    public static boolean computeWaterColor(int i, int j, int k) {
        if (waterColorMap == null) {
            return false;
        } else {
            Colorizer.setColorF(waterColorMap.getColorMultiplier(i, j, k));
            return true;
        }
    }

    public static int colorizeStem(int defaultColor, Block block, int blockMetadata) {
        int[] colors;
        if (block == pumpkinStemBlock) {
            colors = pumpkinStemColors;
        } else if (block == melonStemBlock) {
            colors = melonStemColors;
        } else {
            return defaultColor;
        }
        return colors == null ? defaultColor : colors[blockMetadata & 0x7];
    }

    public static int getLilyPadColor() {
        return lilypadColor;
    }

    public static int getItemColorFromDamage(int defaultColor, Block block, int damage) {
        if (block == waterBlock || block == staticWaterBlock) {
            return colorizeBlock(block, damage) ? blockColor : defaultColor;
        } else {
            return defaultColor;
        }
    }

    public static boolean computeRedstoneWireColor(int current) {
        if (redstoneColor == null) {
            return false;
        } else {
            System.arraycopy(redstoneColor[Math.max(Math.min(current, 15), 0)], 0, Colorizer.setColor, 0, 3);
            return true;
        }
    }

    public static int colorizeRedstoneWire(IBlockAccess blockAccess, int i, int j, int k, int defaultColor) {
        if (redstoneColor == null) {
            return defaultColor;
        } else {
            int metadata = Math.max(Math.min(blockAccess.getBlockMetadata(i, j, k), 15), 0);
            return Colorizer.float3ToInt(redstoneColor[metadata]);
        }
    }

    public static void colorizeWaterBlockGL(Block block) {
        if (block == waterBlock || block == staticWaterBlock) {
            GL11.glColor4f(waterColor[0], waterColor[1], waterColor[2], 1.0f);
        }
    }

    private static void computeVertexColor(IColorMap colorMap, int i, int j, int k, int[] offsets, float[] color) {
        int rgb;
        if (enableTestColorSmoothing) {
            rgb = 0;
            rgb |= (i + offsets[0]) % 2 == 0 ? 0 : 0xff0000;
            rgb |= (j + offsets[1]) % 2 == 0 ? 0 : 0xff00;
            rgb |= (k + offsets[2]) % 2 == 0 ? 0 : 0xff;
        } else {
            rgb = colorMap.getColorMultiplier(i + offsets[0], j + offsets[1], k + offsets[2]);
        }
        Colorizer.intToFloat3(rgb, color);
    }

    public static boolean setupBlockSmoothing(RenderBlocks renderBlocks, Block block, IBlockAccess blockAccess,
                                              int i, int j, int k, int face,
                                              float topLeft, float bottomLeft, float bottomRight, float topRight) {
        return checkBiomeSmoothing(block) &&
            RenderBlocksUtils.useColorMultiplier(face) &&
            setupBiomeSmoothing(renderBlocks, block, blockAccess, i, j, k, face, true, topLeft, bottomLeft, bottomRight, topRight);
    }

    public static boolean setupBlockSmoothing(RenderBlocks renderBlocks, Block block, IBlockAccess blockAccess,
                                              int i, int j, int k, int face) {
        return checkBiomeSmoothing(block) &&
            setupBiomeSmoothing(renderBlocks, block, blockAccess, i, j, k, face, true, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static boolean checkBiomeSmoothing(Block block) {
        return enableSmoothBiomes && RenderBlocksUtils.isAmbientOcclusionEnabled() && BlockAPI.getBlockLightValue(block) == 0;
    }

    private static boolean setupBiomeSmoothing(RenderBlocks renderBlocks, Block block, IBlockAccess blockAccess,
                                               int i, int j, int k, int face,
                                               boolean useAO, float topLeft, float bottomLeft, float bottomRight, float topRight) {
        IColorMap colorMap = findColorMap(block, blockAccess, i, j, k);
        if (colorMap == null) {
            return false;
        }

        if (useAO) {
            float aoBase = RenderBlocksUtils.AO_BASE[face % 6];
            topLeft *= aoBase;
            bottomLeft *= aoBase;
            bottomRight *= aoBase;
            topRight *= aoBase;
        }

        int[][] offsets = FACE_VERTICES[face];
        float[] color = Colorizer.setColor;

        computeVertexColor(colorMap, i, j, k, offsets[0], color);
        renderBlocks.colorRedTopLeft = topLeft * color[0];
        renderBlocks.colorGreenTopLeft = topLeft * color[1];
        renderBlocks.colorBlueTopLeft = topLeft * color[2];

        computeVertexColor(colorMap, i, j, k, offsets[1], color);
        renderBlocks.colorRedBottomLeft = bottomLeft * color[0];
        renderBlocks.colorGreenBottomLeft = bottomLeft * color[1];
        renderBlocks.colorBlueBottomLeft = bottomLeft * color[2];

        computeVertexColor(colorMap, i, j, k, offsets[2], color);
        renderBlocks.colorRedBottomRight = bottomRight * color[0];
        renderBlocks.colorGreenBottomRight = bottomRight * color[1];
        renderBlocks.colorBlueBottomRight = bottomRight * color[2];

        computeVertexColor(colorMap, i, j, k, offsets[3], color);
        renderBlocks.colorRedTopRight = topRight * color[0];
        renderBlocks.colorGreenTopRight = topRight * color[1];
        renderBlocks.colorBlueTopRight = topRight * color[2];

        return true;
    }
}
