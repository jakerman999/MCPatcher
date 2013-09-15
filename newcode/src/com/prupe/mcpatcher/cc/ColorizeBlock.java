package com.prupe.mcpatcher.cc;

import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import com.prupe.mcpatcher.mal.biome.BiomeAPI;
import com.prupe.mcpatcher.mal.block.BlockAPI;
import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ColorizeBlock {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_COLORS);

    private static final ResourceLocation REDSTONE_COLORS = TexturePackAPI.newMCPatcherResourceLocation("colormap/redstone.png");
    private static final ResourceLocation STEM_COLORS = TexturePackAPI.newMCPatcherResourceLocation("colormap/stem.png");
    private static final ResourceLocation PUMPKIN_STEM_COLORS = TexturePackAPI.newMCPatcherResourceLocation("colormap/pumpkinstem.png");
    private static final ResourceLocation MELON_STEM_COLORS = TexturePackAPI.newMCPatcherResourceLocation("colormap/melonstem.png");
    private static final ResourceLocation SWAMPGRASSCOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/swampgrass.png");
    private static final ResourceLocation SWAMPFOLIAGECOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/swampfoliage.png");
    private static final ResourceLocation FOLIAGECOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/foliage.png");
    private static final ResourceLocation PINECOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/pine.png");
    private static final ResourceLocation BIRCHCOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/birch.png");
    private static final ResourceLocation JUNGLECOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/jungle.png");
    private static final ResourceLocation WATERCOLOR = TexturePackAPI.newMCPatcherResourceLocation("colormap/water.png");

    private static final String PALETTE_BLOCK_KEY = "palette.block.";

    private static final int BLOCK_ID_WATER = 8;
    private static final int BLOCK_ID_WATER_STATIC = 9;
    private static final int BLOCK_ID_GRASS = 2;
    private static final int BLOCK_ID_LEAVES = 18;
    private static final int BLOCK_ID_PUMPKIN_STEM = 104;
    private static final int BLOCK_ID_MELON_STEM = 105;

    private static final ColorMap[] blockColorMaps = new ColorMap[BlockAPI.getNumBlocks()]; // bitmaps from palette.block.*
    private static final Map<Integer, ColorMap> blockMetaColorMaps = new HashMap<Integer, ColorMap>(); // bitmaps from palette.block.*
    private static ColorMap swampGrassColorMap;
    private static ColorMap swampFoliageColorMap;
    private static int lilypadColor; // lilypad
    private static float[][] redstoneColor; // colormap/redstone.png
    private static int[] pumpkinStemColors; // colormap/pumpkinstem.png
    private static int[] melonStemColors; // colormap/melonstem.png

    private static BiomeGenBase swampBiome;

    private static final int blockBlendRadius = Config.getInt(MCPatcherUtils.CUSTOM_COLORS, "blockBlendRadius", 1);

    public static float[] waterColor;

    static {
        try {
            reset();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    static void reset() {
        swampBiome = BiomeAPI.findBiomeByName("Swampland");

        Arrays.fill(blockColorMaps, null);
        blockMetaColorMaps.clear();
        swampGrassColorMap = null;
        swampFoliageColorMap = null;

        lilypadColor = 0x208030;
        waterColor = new float[]{0.2f, 0.3f, 1.0f};
        redstoneColor = null;
        pumpkinStemColors = null;
        melonStemColors = null;
    }

    static void reloadColorMaps(Properties properties) {
        registerColorMap(FOLIAGECOLOR, BLOCK_ID_LEAVES, 0);
        registerColorMap(PINECOLOR, BLOCK_ID_LEAVES, 1);
        registerColorMap(BIRCHCOLOR, BLOCK_ID_LEAVES, 2);
        registerColorMap(JUNGLECOLOR, BLOCK_ID_LEAVES, 3);
        registerColorMap(WATERCOLOR, BLOCK_ID_WATER + " " + BLOCK_ID_WATER_STATIC);

        swampGrassColorMap = ColorMap.loadColorMap(Colorizer.useSwampColors, SWAMPGRASSCOLOR);
        swampFoliageColorMap = ColorMap.loadColorMap(Colorizer.useSwampColors, SWAMPFOLIAGECOLOR);
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

    private static void registerColorMap(ResourceLocation resource, int blockId, int metadata) {
        registerColorMap(resource, blockId + ":" + metadata);
    }

    private static void registerColorMap(ResourceLocation resource, String idList) {
        ColorMap colorMap = ColorMap.loadColorMap(true, resource);
        if (colorMap == null) {
            return;
        }
        for (String idString : idList.split("\\s+")) {
            String[] tokens = idString.split(":");
            int[] tokensInt = new int[tokens.length];
            try {
                for (int i = 0; i < tokens.length; i++) {
                    tokensInt[i] = Integer.parseInt(tokens[i]);
                }
            } catch (NumberFormatException e) {
                continue;
            }
            switch (tokensInt.length) {
                case 1:
                    if (tokensInt[0] < 0 || tokensInt[0] >= blockColorMaps.length) {
                        continue;
                    }
                    blockColorMaps[tokensInt[0]] = colorMap;
                    break;

                case 2:
                    blockMetaColorMaps.put(ColorMap.getBlockMetaKey(tokensInt[0], tokensInt[1]), colorMap);
                    break;

                default:
                    continue;
            }
            logger.finer("using %s for block %s, default color %06x", resource, idString, colorMap.getColorMultiplier());
        }
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

    private static ColorMap findColorMap(int blockId, int metadata) {
        ColorMap colorMap = blockMetaColorMaps.get(ColorMap.getBlockMetaKey(blockId, metadata));
        if (colorMap != null) {
            return colorMap;
        }
        return blockColorMaps[blockId];
    }

    private static ColorMap findColorMap(int blockId, int metadata, BiomeGenBase biome) {
        if (biome == swampBiome) {
            switch (blockId) {
                case BLOCK_ID_GRASS:
                    if (swampGrassColorMap != null) {
                        return swampGrassColorMap;
                    }

                case BLOCK_ID_LEAVES:
                    if (swampFoliageColorMap != null) {
                        return swampFoliageColorMap;
                    }

                default:
                    break;
            }
        }
        return findColorMap(blockId, metadata);
    }

    private static int postProcessColorMultiplier(int blockId, int rgb) {
        if (blockId == BLOCK_ID_WATER || blockId == BLOCK_ID_WATER_STATIC) {
            Colorizer.intToFloat3(rgb, waterColor);
            waterColor[0] *= ColorizeEntity.waterBaseColor[0];
            waterColor[1] *= ColorizeEntity.waterBaseColor[1];
            waterColor[2] *= ColorizeEntity.waterBaseColor[2];
            return Colorizer.float3ToInt(waterColor);
        } else {
            return rgb;
        }
    }

    public static int getColorMultiplier(Block block) {
        ColorMap colorMap = blockColorMaps[BlockAPI.getBlockId(block)];
        if (colorMap == null) {
            return block.getBlockColor();
        } else {
            return colorMap.getColorMultiplier();
        }
    }

    public static int getColorMultiplier(Block block, int metadata) {
        int blockId = BlockAPI.getBlockId(block);
        ColorMap colorMap = findColorMap(blockId, metadata);
        if (colorMap == null) {
            return block.getBlockColor();
        } else {
            return postProcessColorMultiplier(blockId, colorMap.getColorMultiplier());
        }
    }

    public static int getColorMultiplier(Block block, IBlockAccess blockAccess, int i, int j, int k) {
        int metadata = blockAccess.getBlockMetadata(i, j, k);
        int blockId = BlockAPI.getBlockId(block);
        ColorMap colorMap = findColorMap(blockId, metadata, BiomeAPI.getBiomeGenAt(i, j, k));
        if (colorMap == null) {
            return block.getBlockColor();
        } else {
            return postProcessColorMultiplier(blockId, colorMap.getColorMultiplier(i, j, k, blockBlendRadius));
        }
    }

    public static int colorizeStem(int defaultColor, Block block, int blockMetadata) {
        int[] colors;
        switch (BlockAPI.getBlockId(block)) {
            case BLOCK_ID_PUMPKIN_STEM:
                colors = pumpkinStemColors;
                break;

            case BLOCK_ID_MELON_STEM:
                colors = melonStemColors;
                break;

            default:
                return defaultColor;
        }
        return colors == null ? defaultColor : colors[blockMetadata & 0x7];
    }

    public static int getLilyPadColor() {
        return lilypadColor;
    }

    public static int getItemColorFromDamage(int defaultColor, int blockId, int damage) {
        if (blockId == BLOCK_ID_WATER || blockId == BLOCK_ID_WATER_STATIC) {
            return getColorMultiplier(BlockAPI.getBlockById(blockId), damage);
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

    public static void colorizeWaterBlockGL(int blockID) {
        if (blockID == BLOCK_ID_WATER || blockID == BLOCK_ID_WATER_STATIC) {
            getColorMultiplier(BlockAPI.getBlockById(blockID));
            GL11.glColor4f(waterColor[0], waterColor[1], waterColor[2], 1.0f);
        }
    }
}
