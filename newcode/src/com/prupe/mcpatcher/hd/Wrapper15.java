package com.prupe.mcpatcher.hd;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import net.minecraft.src.ResourceLocation;
import net.minecraft.src.Texture;
import net.minecraft.src.TextureAtlas;
import net.minecraft.src.TextureAtlasSprite;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

// 1.5 only
public class Wrapper15 {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.MIPMAP);

    public static TextureAtlas currentAtlas;
    public static TextureAtlasSprite currentSprite;

    private static boolean flippedTextureLogged;

    public static void setupTexture(Texture texture, ResourceLocation textureName) {
        int width = texture.getWidth();
        int height = texture.getHeight();
        int[] rgb = new int[width * height];
        IntBuffer buffer = texture.getTextureData().asIntBuffer();
        buffer.position(0);
        buffer.get(rgb);
        MipmapHelper.setupTexture(rgb, width, height, 0, 0, false, false, textureName.getPath());
    }

    public static void setupTexture(Texture texture, BufferedImage image, int glTextureId, boolean blur, boolean clamp, ResourceLocation textureName) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] rgb = new int[width * height];
        image.getRGB(0, 0, width, height, rgb, 0, width);
        TexturePackAPI.bindTexture(glTextureId);
        MipmapHelper.setupTexture(rgb, width, height, 0, 0, blur, clamp, textureName.getPath());
    }

    public static void copySubTexture(Texture dst, Texture src, int x, int y, boolean flipped) {
        if (flipped && !flippedTextureLogged) {
            flippedTextureLogged = true;
            logger.warning("copySubTexture(%s, %s, %d, %d, %s): flipped texture not yet supported",
                dst.getTextureName(), src.getTextureName(), x, y, flipped
            );
        }
        copySubTexture(getMipmaps(src), x, y, src.getWidth(), src.getHeight());
    }

    public static void copySubTexture(Texture dst, ByteBuffer srcBuffer, int x, int y, int width, int height) {
        copySubTexture(getMipmaps(srcBuffer, width, height), x, y, width, height);
    }

    private static void copySubTexture(IntBuffer[] mipmaps, int x, int y, int width, int height) {
        for (int level = 0; level < mipmaps.length; level++) {
            IntBuffer mipmap = mipmaps[level];
            if (mipmap != null) {
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, level, x, y, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, mipmap);
            }
            if (level >= mipmaps.length) {
                break;
            }
            x >>= 1;
            y >>= 1;
            width >>= 1;
            height >>= 1;
        }
    }

    private static IntBuffer[] getMipmaps(ByteBuffer buffer, int width, int height) {
        int levels = MipmapHelper.getMipmapLevelsForCurrentTexture();
        IntBuffer[] mipmaps = new IntBuffer[levels + 1];
        mipmaps[0] = getDirectByteBuffer(buffer).asIntBuffer();
        for (int level = 1; level < mipmaps.length; level++) {
            mipmaps[level] = MipmapHelper.newIntBuffer(mipmaps[level - 1].capacity() >> 2);
            MipmapHelper.scaleHalf(mipmaps[level - 1], width, height, mipmaps[level], 0);
            width >>= 1;
            height >>= 1;
        }
        return mipmaps;
    }

    private static IntBuffer[] getMipmaps(Texture texture) {
        if (texture.mipmapData == null) {
            texture.mipmapData = getMipmaps(texture.getTextureData(), texture.getWidth(), texture.getHeight());
        }
        return texture.mipmapData;
    }

    private static ByteBuffer getDirectByteBuffer(ByteBuffer buffer) {
        if (buffer.isDirect()) {
            return buffer;
        } else {
            ByteBuffer newBuffer = ByteBuffer.allocateDirect(buffer.capacity());
            newBuffer.order(buffer.order());
            newBuffer.put(buffer);
            newBuffer.flip();
            return newBuffer;
        }
    }

    public static BufferedImage addAABorder(String name, BufferedImage input) {
        if (currentSprite == null) {
            return input;
        } else {
            return AAHelper.addBorder(currentSprite, new ResourceLocation(name), input);
        }
    }

    public static TextureAtlasSprite createSprite(String name) {
        if (currentAtlas == null) {
            return new TextureAtlasSprite(name);
        } else {
            return BorderedTexture.create(currentAtlas.basePath, name);
        }
    }
}
