package com.prupe.mcpatcher.cit;

import com.prupe.mcpatcher.MCLogger;
import com.prupe.mcpatcher.MCPatcherUtils;
import com.prupe.mcpatcher.mal.resource.TexturePackAPI;
import com.prupe.mcpatcher.mal.tile.FaceInfo;
import net.minecraft.src.*;

public class CITUtils18 {
    private static final MCLogger logger = MCLogger.getLogger(MCPatcherUtils.CUSTOM_ITEM_TEXTURES, "CIT");

    private static ItemStack currentItem;
    private static int currentColor;
    private static ItemOverride itemOverride;
    private static ArmorOverride armorOverride;

    private static boolean renderingEnchantment;
    private static EnchantmentList enchantments;

    public static void preRender(ItemStack itemStack, int color) {
        currentColor = color;
        if (itemStack == null) {
            // rendering enchantment -- keep current state
        } else if (itemStack.getItem() instanceof ItemBlock) {
            clear();
        } else {
            currentItem = itemStack;
            itemOverride = CITUtils.findItemOverride(itemStack);
            armorOverride = CITUtils.findArmorOverride(itemStack);
            enchantments = CITUtils.findEnchantments(itemStack);
            renderingEnchantment = false;
            if (logger.logEvery(5000L)) {
                logger.info("preRender(%s, %08x) -> %s %s %s",
                    currentItem, currentColor, itemOverride, armorOverride, enchantments
                );
            }
        }
    }

    public static ModelFace getModelFace(ModelFace origFace) {
        if (renderingEnchantment) {
            return FaceInfo.getFaceInfo(origFace).getUnscaledFace();
        } else if (itemOverride == null) {
            return origFace;
        } else {
            FaceInfo faceInfo = FaceInfo.getFaceInfo(origFace);
            TextureAtlasSprite newIcon = (TextureAtlasSprite) itemOverride.getReplacementIcon(faceInfo.getSprite());
            return faceInfo.getAltFace(newIcon);
        }
    }

    public static boolean renderEnchantments3D(RenderItemCustom renderItem, IModel model) {
        if (enchantments != null && !enchantments.isEmpty()) {
            renderingEnchantment = true;
            Enchantment.beginOuter3D();
            for (int i = 0; i < enchantments.size(); i++) {
                Enchantment enchantment = enchantments.getEnchantment(i);
                float intensity = enchantments.getIntensity(i);
                if (intensity > 0.0f && enchantment.bindTexture(null)) {
                    enchantment.begin(intensity);
                    renderItem.renderItem1(model, -1, null);
                    enchantment.end();
                }
            }
            Enchantment.endOuter3D();
            TexturePackAPI.bindTexture(TexturePackAPI.ITEMS_PNG);
            renderingEnchantment = false;
        }
        return !CITUtils.useGlint;
    }

    static void clear() {
        currentItem = null;
        itemOverride = null;
        armorOverride = null;
        enchantments = null;
        renderingEnchantment = false;
    }
}
