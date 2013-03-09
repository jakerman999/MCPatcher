package com.prupe.mcpatcher.mod;

import com.prupe.mcpatcher.*;
import net.minecraft.client.Minecraft;
import net.minecraft.src.*;

import java.awt.image.BufferedImage;
import java.util.*;

public class CTMUtils {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CONNECTED_TEXTURES, "CTM");

    private static final boolean enableStandard = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "standard", true);
    private static final boolean enableNonStandard = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "nonStandard", true);
    private static final boolean enableGrass = Config.getBoolean(MCPatcherUtils.CONNECTED_TEXTURES, "grass", false);
    private static final int splitTextures = Config.getInt(MCPatcherUtils.CONNECTED_TEXTURES, "splitTextures", 1);
    private static final int maxRecursion = Config.getInt(MCPatcherUtils.CONNECTED_TEXTURES, "maxRecursion", 4);

    static final int BLOCK_ID_LOG = 17;
    static final int BLOCK_ID_QUARTZ = 155;
    static final int BLOCK_ID_GLASS = 20;
    static final int BLOCK_ID_GLASS_PANE = 102;
    static final int BLOCK_ID_BOOKSHELF = 47;
    static final int BLOCK_ID_GRASS = 2;
    static final int BLOCK_ID_MYCELIUM = 110;
    static final int BLOCK_ID_SNOW = 78;
    static final int BLOCK_ID_CRAFTED_SNOW = 80;

    private static final int CTM_TEXTURE_MAP_INDEX = 2;
    private static final int MAX_CTM_TEXTURE_SIZE;

    private static final ITileOverride blockOverrides[][] = new ITileOverride[Block.blocksList.length][];
    private static final Map<String, ITileOverride[]> tileOverrides = new HashMap<String, ITileOverride[]>();
    private static final List<ITileOverride> overridesToRegister = new ArrayList<ITileOverride>();
    private static final TexturePackChangeHandler changeHandler;
    private static boolean changeHandlerCalled;
    private static boolean registerIconsCalled;

    static boolean active;
    static TextureMap terrainMap;
    private static BufferedImage missingTextureImage = TileOverride.generateDebugTexture("missing", 64, 64, false);
    private static List<TextureMap> ctmMaps = new ArrayList<TextureMap>();
    static String overrideTextureName;

    static ITileOverride lastOverride;

    static {
        try {
            Class.forName(MCPatcherUtils.RENDER_PASS_CLASS).getMethod("finish").invoke(null);
        } catch (Throwable e) {
        }

        int maxSize = Minecraft.getMaxTextureSize();
        logger.config("max texture size is %dx%d", maxSize, maxSize);
        MAX_CTM_TEXTURE_SIZE = (maxSize * maxSize) * 7 / 8;

        changeHandler = new TexturePackChangeHandler(MCPatcherUtils.CONNECTED_TEXTURES, 2) {
            @Override
            public void initialize() {
            }

            @Override
            public void beforeChange() {
                changeHandlerCalled = true;
                try {
                    for (TextureMap textureMap : ctmMaps) {
                        textureMap.getTexture().unloadGLTexture();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                ctmMaps.clear();
                RenderPassAPI.instance.clear();
                TessellatorUtils.clear(Tessellator.instance);
                Arrays.fill(blockOverrides, null);
                tileOverrides.clear();
                overridesToRegister.clear();

                if (enableStandard || enableNonStandard) {
                    List<String> fileList = new ArrayList<String>();
                    for (String dir : TexturePackAPI.listDirectories("/ctm")) {
                        for (String s : TexturePackAPI.listResources(dir, ".properties")) {
                            fileList.add(s);
                        }
                    }
                    Collections.sort(fileList, new Comparator<String>() {
                        public int compare(String o1, String o2) {
                            String f1 = o1.replaceAll(".*/", "").replaceFirst("\\.properties", "");
                            String f2 = o2.replaceAll(".*/", "").replaceFirst("\\.properties", "");
                            int result = f1.compareTo(f2);
                            if (result != 0) {
                                return result;
                            }
                            return o1.compareTo(o2);
                        }
                    });
                    for (String s : fileList) {
                        registerOverride(TileOverride.create(s));
                    }
                }
            }

            @Override
            public void afterChange() {
                if (splitTextures > 0 && (enableStandard || enableNonStandard)) {
                    for (int index = 0; !overridesToRegister.isEmpty(); index++) {
                        registerIconsCalled = false;
                        TextureMap ctmMap = new TextureMap(CTM_TEXTURE_MAP_INDEX, "ctm" + index, "not_used", missingTextureImage);
                        ctmMap.refresh();
                        if (!registerIconsCalled) {
                            logger.severe("CTMUtils.registerIcons was never called!  Possible conflict in TextureMap.class");
                            break;
                        }
                        ctmMaps.add(ctmMap);
                    }
                }
                overridesToRegister.clear();

                for (int i = 0; i < blockOverrides.length; i++) {
                    if (blockOverrides[i] != null && Block.blocksList[i] != null) {
                        for (ITileOverride override : blockOverrides[i]) {
                            if (override != null && !override.isDisabled() && override.getRenderPass() >= 0) {
                                RenderPassAPI.instance.setRenderPassForBlock(Block.blocksList[i], override.getRenderPass());
                            }
                        }
                    }
                }
                changeHandlerCalled = false;
            }
        };
        TexturePackChangeHandler.register(changeHandler);
    }

    public static void start() {
        lastOverride = null;
        if (terrainMap == null) {
            active = false;
            Tessellator.instance.textureMap = null;
        } else {
            active = true;
            Tessellator.instance.textureMap = terrainMap;
        }
    }

    public static Icon getTile(RenderBlocks renderBlocks, Block block, int i, int j, int k, Icon origIcon, Tessellator tessellator) {
        return getTile(renderBlocks, block, i, j, k, -1, origIcon, tessellator);
    }

    public static Icon getTile(RenderBlocks renderBlocks, Block block, final int i, final int j, final int k, final int face, Icon icon, Tessellator tessellator) {
        lastOverride = null;
        if (checkFace(face) && checkBlock(renderBlocks, block)) {
            final IBlockAccess blockAccess = renderBlocks.blockAccess;
            TileOverrideIterator iterator = new TileOverrideIterator(block, icon) {
                @Override
                Icon getTile(ITileOverride override, Block block, Icon currentIcon) {
                    return override.getTile(blockAccess, block, currentIcon, i, j, k, face);
                }
            };
            lastOverride = iterator.go();
            if (lastOverride != null) {
                icon = iterator.getIcon();
            }
        }
        return lastOverride == null && skipDefaultRendering(block) ? null : icon;
    }

    public static Icon getTile(RenderBlocks renderBlocks, Block block, int face, int metadata, Tessellator tessellator) {
        return getTile(renderBlocks, block, face, metadata, renderBlocks.getIconBySideAndMetadata(block, face, metadata), tessellator);
    }

    public static Icon getTile(RenderBlocks renderBlocks, Block block, int face, Tessellator tessellator) {
        return getTile(renderBlocks, block, face, 0, renderBlocks.getIconBySide(block, face), tessellator);
    }

    private static Icon getTile(RenderBlocks renderBlocks, Block block, final int face, final int metadata, Icon icon, Tessellator tessellator) {
        lastOverride = null;
        if (checkFace(face)) {
            TileOverrideIterator iterator = new TileOverrideIterator(block, icon) {
                @Override
                Icon getTile(ITileOverride override, Block block, Icon currentIcon) {
                    return override.getTile(block, currentIcon, face, metadata);
                }
            };
            lastOverride = iterator.go();
            if (lastOverride != null) {
                icon = iterator.getIcon();
            }
        }
        return icon;
    }

    public static Tessellator getTessellator(Icon icon) {
        return TessellatorUtils.getTessellator(Tessellator.instance, icon);
    }

    public static void registerIcons(TextureMap textureMap, Stitcher stitcher, String mapName, Map<StitchHolder, List<Texture>> map) {
        TessellatorUtils.registerTextureMap(textureMap, mapName);
        if (mapName == null || (!mapName.equals("terrain") && !mapName.matches("ctm\\d+"))) {
            return;
        }
        registerIconsCalled = true;
        if (!changeHandlerCalled) {
            logger.severe("beforeChange was not called, invoking directly");
            changeHandler.beforeChange();
        }

        int totalSize = 0;
        for (List<Texture> list : map.values()) {
            if (list != null && list.size() > 0) {
                Texture texture = list.get(0);
                totalSize += texture.getWidth() * texture.getHeight();
            }
        }

        logger.fine("begin registerIcons(%s): %.1fMB used, %d items to go",
            mapName, 4 * totalSize / 1048576.0f, overridesToRegister.size()
        );

        if (mapName.equals("terrain")) {
            terrainMap = textureMap;
            if (enableGrass) {
                registerOverride(new TileOverrideImpl.BetterGrass(textureMap, BLOCK_ID_GRASS, "grass"));
                registerOverride(new TileOverrideImpl.BetterGrass(textureMap, BLOCK_ID_MYCELIUM, "mycel"));
            }
            if (splitTextures > 1) {
                return;
            }
        }

        boolean warned = false;
        boolean progress = false;
        for (Iterator<ITileOverride> iterator = overridesToRegister.iterator(); iterator.hasNext(); ) {
            ITileOverride override = iterator.next();
            if (override == null || override.isDisabled()) {
                continue;
            }
            totalSize += override.getTotalTextureSize();
            if (totalSize > MAX_CTM_TEXTURE_SIZE) {
                float sizeMB = 4 * totalSize / 1048576.0f;
                if (splitTextures > 0) {
                    logger.info("%s map is nearly full (%.1fMB), starting a new one", mapName, sizeMB);
                    totalSize -= override.getTotalTextureSize();
                    break;
                } else if (!warned) {
                    logger.warning("%s map is nearly full (%.1fMB), crash may be imminent", mapName, sizeMB);
                    warned = true;
                }
            }
            override.registerIcons(textureMap, stitcher, map);
            iterator.remove();
            progress = true;
        }

        logger.fine("end registerIcons(%s): %.1fMB used, %d items to go",
            mapName, 4 * totalSize / 1048576.0f, overridesToRegister.size()
        );
        if (warned || (!progress && !mapName.equals("terrain"))) {
            logger.warning("clearing all remaining items");
            overridesToRegister.clear();
        }
    }

    public static String getOverridePath(String prefix, String name, String ext) {
        String path;
        if (name.startsWith("/")) {
            path = name.substring(1).replaceFirst("\\.[^.]+$", "") + ext;
        } else {
            path = prefix + name + ext;
        }
        logger.finer("getOverridePath(%s, %s, %s) -> %s", prefix, name, ext, path);
        return path;
    }

    public static String getOverrideTextureName(String name) {
        if (overrideTextureName == null) {
            if (name.matches("^\\d+$")) {
                logger.warning("no override set for %s", name);
            }
            return name;
        } else {
            logger.finer("getOverrideTextureName(%s) -> %s", name, overrideTextureName);
            return overrideTextureName;
        }
    }

    public static void updateAnimations() {
        for (TextureMap textureMap : ctmMaps) {
            textureMap.updateAnimations();
        }
    }

    public static void reset() {
    }

    public static void finish() {
        reset();
        RenderPassAPI.instance.finish();
        Tessellator.instance.textureMap = null;
        lastOverride = null;
        active = false;
    }

    private static boolean checkBlock(RenderBlocks renderBlocks, Block block) {
        return active && renderBlocks.blockAccess != null;
    }

    private static boolean checkFace(int face) {
        return active && (face < 0 ? enableNonStandard : enableStandard);
    }

    private static boolean skipDefaultRendering(Block block) {
        return RenderPassAPI.instance.skipDefaultRendering(block);
    }

    private static void registerOverride(ITileOverride override) {
        if (override != null && !override.isDisabled()) {
            if (override.getTotalTextureSize() > 0) {
                overridesToRegister.add(override);
            }
            if (override.getMatchingBlocks() != null) {
                for (int index : override.getMatchingBlocks()) {
                    String blockName = "";
                    if (index >= 0 && index < Block.blocksList.length && Block.blocksList[index] != null) {
                        blockName = Block.blocksList[index].getShortName();
                        if (blockName == null) {
                            blockName = "";
                        } else {
                            blockName = " (" + blockName + ")";
                        }
                    }
                    blockOverrides[index] = registerOverride(blockOverrides[index], override, "block " + index + blockName);
                }
            }
            if (override.getMatchingTiles() != null) {
                for (String name : override.getMatchingTiles()) {
                    tileOverrides.put(name, registerOverride(tileOverrides.get(name), override, "tile " + name));
                }
            }
        }
    }

    private static ITileOverride[] registerOverride(ITileOverride[] overrides, ITileOverride override, String description) {
        logger.fine("using %s for %s", override.toString(), description);
        if (overrides == null) {
            return new ITileOverride[]{override};
        } else {
            ITileOverride[] newList = new ITileOverride[overrides.length + 1];
            System.arraycopy(overrides, 0, newList, 0, overrides.length);
            newList[overrides.length] = override;
            return newList;
        }
    }

    abstract private static class TileOverrideIterator implements Iterator<ITileOverride> {
        private final Block block;
        private Icon currentIcon;
        private ITileOverride[] blockOverrides;
        private ITileOverride[] iconOverrides;
        private final Set<ITileOverride> skipOverrides = new HashSet<ITileOverride>();

        private int blockPos;
        private int iconPos;
        private boolean foundNext;
        private ITileOverride nextOverride;

        private ITileOverride lastMatchedOverride;

        TileOverrideIterator(Block block, Icon icon) {
            this.block = block;
            currentIcon = icon;
            blockOverrides = CTMUtils.blockOverrides[block.blockID];
            iconOverrides = CTMUtils.tileOverrides.get(this.currentIcon.getName());
        }

        private void resetForNextPass() {
            blockOverrides = null;
            iconOverrides = CTMUtils.tileOverrides.get(currentIcon.getName());
            blockPos = 0;
            iconPos = 0;
            foundNext = false;
        }

        public boolean hasNext() {
            if (foundNext) {
                return true;
            }
            if (iconOverrides != null) {
                while (iconPos < iconOverrides.length) {
                    if (checkOverride(iconOverrides[iconPos++])) {
                        return true;
                    }
                }
            }
            if (blockOverrides != null) {
                while (blockPos < blockOverrides.length) {
                    if (checkOverride(blockOverrides[blockPos++])) {
                        return true;
                    }
                }
            }
            return false;
        }

        public ITileOverride next() {
            if (!foundNext) {
                throw new IllegalStateException("next called before hasNext() == true");
            }
            foundNext = false;
            return nextOverride;
        }

        public void remove() {
            throw new UnsupportedOperationException("remove not supported");
        }

        private boolean checkOverride(ITileOverride override) {
            if (override != null && !override.isDisabled() && !skipOverrides.contains(override)) {
                foundNext = true;
                nextOverride = override;
                return true;
            } else {
                return false;
            }
        }

        ITileOverride go() {
            pass:
            for (int pass = 0; pass < maxRecursion; pass++) {
                while (hasNext()) {
                    ITileOverride override = next();
                    Icon newIcon = getTile(override, block, currentIcon);
                    if (newIcon != null) {
                        lastMatchedOverride = override;
                        skipOverrides.add(override);
                        currentIcon = newIcon;
                        resetForNextPass();
                        continue pass;
                    }
                }
                break;
            }
            return lastMatchedOverride;
        }

        Icon getIcon() {
            return currentIcon;
        }

        abstract Icon getTile(ITileOverride override, Block block, Icon currentIcon);
    }
}