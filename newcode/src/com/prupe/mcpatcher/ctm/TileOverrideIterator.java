package com.prupe.mcpatcher.ctm;

import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.Block;
import net.minecraft.src.Icon;

import java.util.*;

abstract class TileOverrideIterator implements Iterator<ITileOverride> {
    private static final int MAX_RECURSION = Config.getInt(MCPatcherUtils.CONNECTED_TEXTURES, "maxRecursion", 4);

    private final Map<Block, List<ITileOverride>> allBlockOverrides;
    private final Map<String, List<ITileOverride>> allTileOverrides;

    protected Icon currentIcon;

    private List<ITileOverride> blockOverrides;
    private List<ITileOverride> tileOverrides;
    private final Set<ITileOverride> skipOverrides = new HashSet<ITileOverride>();

    private int blockPos;
    private int iconPos;
    private boolean foundNext;
    private ITileOverride nextOverride;
    private ITileOverride lastMatchedOverride;

    protected TileOverrideIterator(Map<Block, List<ITileOverride>> allBlockOverrides, Map<String, List<ITileOverride>> allTileOverrides) {
        this.allBlockOverrides = allBlockOverrides;
        this.allTileOverrides = allTileOverrides;
    }

    void clear() {
        currentIcon = null;
        blockOverrides = null;
        tileOverrides = null;
        nextOverride = null;
        lastMatchedOverride = null;
        skipOverrides.clear();
    }

    private void resetForNextPass() {
        blockOverrides = null;
        tileOverrides = allTileOverrides.get(currentIcon.getIconName());
        blockPos = 0;
        iconPos = 0;
        foundNext = false;
    }

    @Override
    public boolean hasNext() {
        if (foundNext) {
            return true;
        }
        if (tileOverrides != null) {
            while (iconPos < tileOverrides.size()) {
                if (checkOverride(tileOverrides.get(iconPos++))) {
                    return true;
                }
            }
        }
        if (blockOverrides != null) {
            while (blockPos < blockOverrides.size()) {
                if (checkOverride(blockOverrides.get(blockPos++))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ITileOverride next() {
        if (!foundNext) {
            throw new IllegalStateException("next called before hasNext() == true");
        }
        foundNext = false;
        return nextOverride;
    }

    @Override
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

    ITileOverride go(BlockOrientation blockOrientation, Icon origIcon) {
        currentIcon = origIcon;
        blockOverrides = allBlockOverrides.get(blockOrientation.block);
        tileOverrides = allTileOverrides.get(origIcon.getIconName());
        blockPos = 0;
        iconPos = 0;
        foundNext = false;
        nextOverride = null;
        lastMatchedOverride = null;
        skipOverrides.clear();

        pass:
        for (int pass = 0; pass < MAX_RECURSION; pass++) {
            while (hasNext()) {
                ITileOverride override = next();
                Icon newIcon = getTile(override, blockOrientation, currentIcon);
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

    abstract Icon getTile(ITileOverride override, BlockOrientation blockOrientation, Icon currentIcon);

    static final class IJK extends TileOverrideIterator {
        IJK(Map<Block, List<ITileOverride>> blockOverrides, Map<String, List<ITileOverride>> tileOverrides) {
            super(blockOverrides, tileOverrides);
        }

        @Override
        Icon getTile(ITileOverride override, BlockOrientation blockOrientation, Icon currentIcon) {
            return override.getTileWorld(blockOrientation, currentIcon);
        }
    }

    static final class Metadata extends TileOverrideIterator {
        Metadata(Map<Block, List<ITileOverride>> blockOverrides, Map<String, List<ITileOverride>> tileOverrides) {
            super(blockOverrides, tileOverrides);
        }

        @Override
        Icon getTile(ITileOverride override, BlockOrientation blockOrientation, Icon currentIcon) {
            return override.getTileHeld(blockOrientation, currentIcon);
        }
    }
}
