package com.pclewis.mcpatcher;

import org.lwjgl.opengl.GL11;

public class BlendMethod {
    public static final BlendMethod ALPHA = new BlendMethod("alpha", GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, true, false, true);
    public static final BlendMethod ADD = new BlendMethod("add", GL11.GL_SRC_ALPHA, GL11.GL_ONE, true, false, true);
    public static final BlendMethod SUBTRACT = new BlendMethod("subtract", GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ZERO, true, true, false);
    public static final BlendMethod MULTIPLY = new BlendMethod("multiply", GL11.GL_DST_COLOR, GL11.GL_ONE_MINUS_SRC_ALPHA, true, true, true);
    public static final BlendMethod DODGE = new BlendMethod("dodge", GL11.GL_ONE, GL11.GL_ONE, true, true, false);
    public static final BlendMethod BURN = new BlendMethod("burn", GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR, true, true, false);
    public static final BlendMethod SCREEN = new BlendMethod("screen", GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_COLOR, true, true, false);
    public static final BlendMethod OVERLAY = new BlendMethod("overlay", GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR, true, true, false);
    public static final BlendMethod REPLACE = new BlendMethod("replace", 0, 0, false, false, true);

    private final int srcBlend;
    private final int dstBlend;
    private final String name;
    private final boolean blend;
    private final boolean fadeRGB;
    private final boolean fadeAlpha;
    
    public static BlendMethod parse(String text) {
        text = text.toLowerCase().trim();
        if (text.equals("alpha")) {
            return ALPHA;
        } else if (text.equals("add")) {
            return ADD;
        } else if (text.equals("subtract")) {
            return SUBTRACT;
        } else if (text.equals("multiply")) {
            return MULTIPLY;
        } else if (text.equals("dodge")) {
            return DODGE;
        } else if (text.equals("burn")) {
            return BURN;
        } else if (text.equals("screen")) {
            return SCREEN;
        } else if (text.equals("overlay") || text.equals("color")) {
            return OVERLAY;
        } else if (text.equals("replace")) {
            return REPLACE;
        } else {
            String[] tokens = text.split("\\s+");
            if (tokens.length >= 2) {
                try {
                    int srcBlend = Integer.parseInt(tokens[0]);
                    int dstBlend = Integer.parseInt(tokens[1]);
                    return new BlendMethod("custom(" + srcBlend + "," + dstBlend + ")", srcBlend, dstBlend, true, true, false);
                } catch (NumberFormatException e) {
                }
            }
        }
        return null;
    }
    
    private BlendMethod(String name, int srcBlend, int dstBlend, boolean blend, boolean fadeRGB, boolean fadeAlpha) {
        this.name = name;
        this.srcBlend = srcBlend;
        this.dstBlend = dstBlend;
        this.blend = blend;
        this.fadeRGB = fadeRGB;
        this.fadeAlpha = fadeAlpha;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    public void applyFade(float fade) {
        if (fadeRGB && fadeAlpha) {
            GL11.glColor4f(fade, fade, fade, fade);
        } else if (fadeRGB) {
            GL11.glColor4f(fade, fade, fade, 1.0f);
        } else if (fadeAlpha) {
            GL11.glColor4f(1.0f, 1.0f, 1.0f, fade);
        }
    }

    public void applyAlphaTest() {
        if (blend) {
            GL11.glDisable(GL11.GL_ALPHA_TEST);
        } else {
            GL11.glEnable(GL11.GL_ALPHA_TEST);
        }
    }
    
    public void applyBlending() {
        if (blend) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(srcBlend, dstBlend);
        } else {
            GL11.glDisable(GL11.GL_BLEND);
        }
    }

    public boolean isColorBased() {
        return fadeRGB;
    }
}
