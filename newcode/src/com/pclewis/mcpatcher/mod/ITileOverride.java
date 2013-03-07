package com.pclewis.mcpatcher.mod;

import net.minecraft.src.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

interface ITileOverride {
    boolean isDisabled();

    int getTotalTextureSize();

    void registerIcons(TextureMap textureMap, Stitcher stitcher, Map<StitchHolder, List<Texture>> map);

    Set<Integer> getMatchingBlocks();

    Set<String> getMatchingTiles();

    int getRenderPass();

    Icon getTile(IBlockAccess blockAccess, Block block, Icon origIcon, int i, int j, int k, int face);

    Icon getTile(Block block, Icon origIcon, int face, int metadata);
}
