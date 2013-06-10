package com.prupe.mcpatcher.hd;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import net.minecraft.src.TextureStitched;

public class BorderedTexture extends TextureStitched {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.MIPMAP);

    private float minU;
    private float maxU;
    private float minV;
    private float maxV;
    private float scaledWidth;
    private float scaledHeight;

    private int tilesheetWidth;
    private int tilesheetHeight;
    private int x0;
    private int y0;

    private String tilesheet;
    int border;

    public static TextureStitched create(String tilesheet, String name) {
        if (AAHelper.useAAForTexture(tilesheet)) {
            return new BorderedTexture(tilesheet, name);
        } else {
            return new TextureStitched(name);
        }
    }

    private BorderedTexture(String tilesheet, String name) {
        super(name);
        this.tilesheet = tilesheet;
    }

    @Override
    public void init(int tilesheetWidth, int tilesheetHeight, int x0, int y0, boolean flipped) {
        super.init(tilesheetWidth, tilesheetHeight, x0, y0, flipped);
        this.tilesheetWidth = tilesheetWidth;
        this.tilesheetHeight = tilesheetHeight;
        this.x0 = x0;
        this.y0 = y0;
        setBorderWidth(border);
    }

    @Override
    public float getMinU() {
        return minU;
    }

    @Override
    public float getMaxU() {
        return maxU;
    }

    @Override
    public float getInterpolatedU(double u) {
        return border > 0 ? minU + (float) u * scaledWidth : super.getInterpolatedU(u);
    }

    @Override
    public float getMinV() {
        return minV;
    }

    @Override
    public float getMaxV() {
        return maxV;
    }

    @Override
    public float getInterpolatedV(double v) {
        return border > 0 ? minV + (float) v * scaledHeight : super.getInterpolatedV(v);
    }

    @Override
    public void copy(TextureStitched stitched) {
        if (stitched instanceof BorderedTexture) {
            BorderedTexture bordered = (BorderedTexture) stitched;
            tilesheetWidth = bordered.tilesheetWidth;
            tilesheetHeight = bordered.tilesheetHeight;
            x0 = bordered.x0;
            y0 = bordered.y0;
            tilesheet = bordered.tilesheet;
            border = bordered.border;
        }
    }

    void setBorderWidth(int border) {
        this.border = border;
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            x0 = y0 = 0;
            minU = maxU = minV = maxV = 0.0f;
            scaledWidth = scaledHeight = 0.0f;
            return;
        }
        logger.finer("setBorderWidth(%s, %s, %d): %dx%d -> %dx%d",
            tilesheet, getIconName(), border, width - 2 * border, height - 2 * border, width, height
        );
        if (border > 0) {
            x0 += border;
            y0 += border;
            width -= 2 * border;
            height -= 2 * border;
            minU = (float) x0 / (float) tilesheetWidth;
            maxU = (float) (x0 + width) / (float) tilesheetWidth;
            minV = (float) y0 / (float) tilesheetHeight;
            maxV = (float) (y0 + height) / (float) tilesheetHeight;
        } else {
            minU = super.getMinU();
            maxU = super.getMaxU();
            minV = super.getMinV();
            maxV = super.getMaxV();
        }
        scaledWidth = (maxU - minU) / 16.0f;
        scaledHeight = (maxV - minV) / 16.0f;
    }
}