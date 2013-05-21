package com.prupe.mcpatcher.mob;

import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.TexturePackAPI;
import net.minecraft.src.Tessellator;

import java.util.Properties;

public class LineRenderer {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.RANDOM_MOBS);

    private static final String LINE_PROPERTIES = MCPatcherUtils.TEXTURE_PACK_PREFIX + "item/line.properties";

    private static final boolean enable = Config.getBoolean(MCPatcherUtils.RANDOM_MOBS, "leashLine", true);
    private static final LineRenderer[] renderers = new LineRenderer[2];

    private final String texture;
    private final double width;
    private final double a;
    private final double b;
    private final int segments;
    private final boolean active;

    public static boolean renderLine(int type, double x, double y, double z, double dx, double dy, double dz) {
        LineRenderer renderer = renderers[type];
        return renderer != null && renderer.render(x, y, z, dx, dy, dz);
    }

    static void reset() {
        if (enable) {
            setup(0, "fishingLine", 0.0075, 0.0, 0.0, 16);
            setup(1, "lead", 0.025, 4.0 / 3.0, 0.125, 24);
        }
    }

    private static void setup(int type, String name, double defaultWidth, double a, double b, int segments) {
        LineRenderer renderer = new LineRenderer(name, defaultWidth, a, b, segments);
        if (renderer.active) {
            logger.fine("using %s", renderer);
            renderers[type] = renderer;
        } else {
            logger.fine("%s not found", renderer);
            renderers[type] = null;
        }
    }

    LineRenderer(String name, double width, double a, double b, int segments) {
        texture = MCPatcherUtils.TEXTURE_PACK_PREFIX + "item/" + name.toLowerCase() + ".png";
        active = TexturePackAPI.hasResource(texture);
        Properties properties = TexturePackAPI.getProperties(LINE_PROPERTIES);
        this.width = MCPatcherUtils.getDoubleProperty(properties, name + ".width", width);
        this.a = MCPatcherUtils.getDoubleProperty(properties, name + ".a", a);
        this.b = MCPatcherUtils.getDoubleProperty(properties, name + ".b", b);
        this.segments = MCPatcherUtils.getIntProperty(properties, name + ".segments", segments);
    }

    boolean render(double x, double y, double z, double dx, double dy, double dz) {
        TexturePackAPI.bindTexture(texture);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        double x0 = x;
        double y0 = y;
        double z0 = z;
        for (int i = 1; i <= segments; i++) {
            double s = i / (double) segments;
            double x1 = x + s * dx;
            double y1 = y + (s * s + s) * dy * 0.5 + a * (1.0 - s) + b;
            double z1 = z + s * dz;

            tessellator.addVertexWithUV(x0, y0, z0, 0.0, 0.0);
            tessellator.addVertexWithUV(x1, y1, z1, 1.0, 0.0);
            tessellator.addVertexWithUV(x1, y1 + width, z1, 1.0, 1.0);
            tessellator.addVertexWithUV(x0, y0 + width, z0, 0.0, 1.0);

            tessellator.addVertexWithUV(x0, y0 + width, z0, 0.0, 1.0);
            tessellator.addVertexWithUV(x1, y1 + width, z1, 1.0, 1.0);
            tessellator.addVertexWithUV(x1, y1, z1, 1.0, 0.0);
            tessellator.addVertexWithUV(x0, y0, z0, 0.0, 0.0);

            x0 = x1;
            y0 = y1;
            z0 = z1;

        }
        tessellator.draw();
        return true;
    }

    @Override
    public String toString() {
        return "LineRenderer{" + texture + ", " + width + "}";
    }
}
