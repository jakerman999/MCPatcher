package net.minecraft.src;

public class TextureMap extends TextureBase implements IStitchedTexture, IconRegister {
    public static ResourceAddress blocksAtlas;
    public static ResourceAddress itemsAtlas;

    public TextureMap(int type, String basePath) {
    }

    public Icon registerIcon(String name) {
        return null;
    }

    public void refreshTextures1(IResourceBundle resources) {
    }

    public void updateAnimations() {
    }
}
