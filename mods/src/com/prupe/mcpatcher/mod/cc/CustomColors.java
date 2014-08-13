package com.prupe.mcpatcher.mod.cc;

import com.prupe.mcpatcher.*;
import com.prupe.mcpatcher.basemod.*;
import com.prupe.mcpatcher.mal.BlockAPIMod;
import com.prupe.mcpatcher.mal.TexturePackAPIMod;
import javassist.bytecode.AccessFlag;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.*;
import static javassist.bytecode.Opcode.*;

public class CustomColors extends Mod {
    static final MethodRef computeRedstoneWireColor = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "computeRedstoneWireColor", "(I)Z");

    static final MethodRef setColorF = new MethodRef(MCPatcherUtils.COLORIZER_CLASS, "setColorF", "(I)V");
    static final FieldRef setColor = new FieldRef(MCPatcherUtils.COLORIZER_CLASS, "setColor", "[F");

    static final MethodRef getColorFromDamage = new MethodRef("Item", "getColorFromDamage", "(LItemStack;I)I");
    private static final FieldRef fleeceColorTable = new FieldRef("EntitySheep", "fleeceColorTable", "[[F");

    public CustomColors() {
        name = MCPatcherUtils.CUSTOM_COLORS;
        author = "MCPatcher";
        description = "Gives texture packs control over hardcoded colors in the game.";
        version = "1.9";

        addDependency(MCPatcherUtils.TEXTURE_PACK_API_MOD);
        addDependency(MCPatcherUtils.BLOCK_API_MOD);
        addDependency(MCPatcherUtils.BIOME_API_MOD);

        configPanel = new ConfigPanel();

        addClassMod(new MinecraftMod(this).mapWorldClient());
        addClassMod(new IBlockAccessMod(this));
        addClassMod(new TessellatorMod(this));
        if (!ResourceLocationMod.haveClass()) {
            addClassMod(new IconMod(this));
        }
        ResourceLocationMod.setup(this);
        PositionMod.setup(this);
        RenderUtilsMod.setup(this);

        if (!IBlockStateMod.haveClass()) {
            CC_Block.setup(this);
        }

        addClassMod(new BiomeGenBaseMod(this));
        addClassMod(new ItemMod());

        addClassMod(new PotionMod());
        addClassMod(new PotionHelperMod());

        addClassMod(new WorldMod());
        addClassMod(new WorldClientMod(this));
        addClassMod(new WorldProviderMod());
        addClassMod(new WorldProviderHellMod());
        addClassMod(new WorldProviderEndMod());
        addClassMod(new WorldChunkManagerMod());
        addClassMod(new EntityMod());
        addClassMod(new EntityFXMod());
        addClassMod(new EntityRainFXMod());
        addClassMod(new EntityDropParticleFXMod());
        addClassMod(new EntitySplashFXMod());
        addClassMod(new EntityBubbleFXMod());
        addClassMod(new EntitySuspendFXMod());
        addClassMod(new EntityPortalFXMod());
        addClassMod(new EntityAuraFXMod());

        // This patch enables custom potion particle effects around players in SMP.
        // Removed because it causes beacon effect particles to become opaque for some reason.
        //addClassMod(new EntityLivingBaseMod());
        addClassMod(new EntityRendererMod());

        addClassMod(new EntityReddustFXMod());

        addClassMod(new RenderGlobalMod());

        addClassMod(new MapColorMod());

        addClassMod(new ItemDyeMod());
        addClassMod(new EntitySheepMod());

        addClassMod(new ItemArmorMod());
        addClassMod(new RenderWolfMod());
        addClassMod(new RecipesDyedArmorMod());

        addClassMod(new EntityListMod());
        addClassMod(new ItemSpawnerEggMod());

        addClassMod(new FontRendererMod());
        addClassMod(new TileEntitySignRendererMod());

        addClassMod(new RenderXPOrbMod());

        addClassFiles("com.prupe.mcpatcher.cc.*");
        if (IBlockStateMod.haveClass()) {
            removeAddedClassFile(MCPatcherUtils.COLORIZE_BLOCK_CLASS);
        } else {
            addClassFiles("com.prupe.mcpatcher.colormap.*");
        }

        TexturePackAPIMod.earlyInitialize(3, MCPatcherUtils.COLORIZER_CLASS, "init");
    }

    private class ConfigPanel extends ModConfigPanel {
        private JCheckBox waterCheckBox;
        private JCheckBox swampCheckBox;
        private JCheckBox treeCheckBox;
        private JCheckBox potionCheckBox;
        private JCheckBox particleCheckBox;
        private JPanel panel;
        private JCheckBox lightmapCheckBox;
        private JCheckBox redstoneCheckBox;
        private JCheckBox stemCheckBox;
        private JCheckBox otherBlockCheckBox;
        private JCheckBox eggCheckBox;
        private JCheckBox fogCheckBox;
        private JCheckBox cloudsCheckBox;
        private JCheckBox mapCheckBox;
        private JCheckBox dyeCheckBox;
        private JSpinner fogBlendRadiusSpinner;
        private JSpinner blockBlendRadiusSpinner;
        private JCheckBox textCheckBox;
        private JCheckBox xpOrbCheckBox;
        private JCheckBox smoothBiomesCheckBox;
        private JCheckBox testBiomeColorsCheckBox;
        private JSpinner yVarianceSpinner;

        ConfigPanel() {
            waterCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "water", waterCheckBox.isSelected());
                }
            });

            swampCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "swamp", swampCheckBox.isSelected());
                }
            });

            treeCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "tree", treeCheckBox.isSelected());
                }
            });

            potionCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "potion", potionCheckBox.isSelected());
                }
            });

            particleCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "particle", particleCheckBox.isSelected());
                }
            });

            lightmapCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "lightmaps", lightmapCheckBox.isSelected());
                }
            });

            cloudsCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "clouds", cloudsCheckBox.isSelected());
                }
            });

            redstoneCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "redstone", redstoneCheckBox.isSelected());
                }
            });

            stemCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "stem", stemCheckBox.isSelected());
                }
            });

            eggCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "egg", eggCheckBox.isSelected());
                }
            });

            mapCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "map", mapCheckBox.isSelected());
                }
            });

            dyeCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "dye", dyeCheckBox.isSelected());
                }
            });

            fogCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "fog", fogCheckBox.isSelected());
                }
            });

            otherBlockCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "otherBlocks", otherBlockCheckBox.isSelected());
                }
            });

            textCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "text", textCheckBox.isSelected());
                }
            });

            xpOrbCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "xporb", xpOrbCheckBox.isSelected());
                }
            });

            fogBlendRadiusSpinner.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    int value = 7;
                    try {
                        value = Integer.parseInt(fogBlendRadiusSpinner.getValue().toString());
                        value = Math.min(Math.max(0, value), 99);
                    } catch (NumberFormatException e1) {
                    }
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "fogBlendRadius", value);
                    fogBlendRadiusSpinner.setValue(value);
                }
            });

            blockBlendRadiusSpinner.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    int value = 1;
                    try {
                        value = Integer.parseInt(blockBlendRadiusSpinner.getValue().toString());
                        value = Math.min(Math.max(0, value), 99);
                    } catch (NumberFormatException e1) {
                    }
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "blockBlendRadius2", value);
                    Config.remove(MCPatcherUtils.CUSTOM_COLORS, "blockBlendRadius");
                    blockBlendRadiusSpinner.setValue(value);
                }
            });

            yVarianceSpinner.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    int value = 0;
                    try {
                        value = Integer.parseInt(yVarianceSpinner.getValue().toString());
                        value = Math.min(Math.max(0, value), 255);
                    } catch (NumberFormatException e1) {
                    }
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "yVariance", value);
                    yVarianceSpinner.setValue(value);
                }
            });

            smoothBiomesCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "smoothBiomes", smoothBiomesCheckBox.isSelected());
                }
            });

            testBiomeColorsCheckBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Config.set(MCPatcherUtils.CUSTOM_COLORS, "testColorSmoothing", testBiomeColorsCheckBox.isSelected());
                }
            });
        }

        @Override
        public JPanel getPanel() {
            return panel;
        }

        @Override
        public void load() {
            waterCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "water", true));
            swampCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "swamp", true));
            treeCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "tree", true));
            potionCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "potion", true));
            particleCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "particle", true));
            lightmapCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "lightmaps", true));
            cloudsCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "clouds", true));
            redstoneCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "redstone", true));
            stemCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "stem", true));
            eggCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "egg", true));
            mapCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "map", true));
            dyeCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "dye", true));
            fogCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "fog", true));
            otherBlockCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "otherBlocks", true));
            textCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "text", true));
            xpOrbCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "xporb", true));
            fogBlendRadiusSpinner.setValue(Config.getInt(MCPatcherUtils.CUSTOM_COLORS, "fogBlendRadius", 7));
            blockBlendRadiusSpinner.setValue(Config.getInt(MCPatcherUtils.CUSTOM_COLORS, "blockBlendRadius2", 4));
            yVarianceSpinner.setValue(Config.getInt(MCPatcherUtils.CUSTOM_COLORS, "yVariance", 0));
            smoothBiomesCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "smoothBiomes", true));
            testBiomeColorsCheckBox.setSelected(Config.getBoolean(MCPatcherUtils.CUSTOM_COLORS, "testColorSmoothing", false));
            showAdvancedOption(testBiomeColorsCheckBox);
        }

        @Override
        public void save() {
        }
    }

    private class ItemMod extends com.prupe.mcpatcher.basemod.ItemMod {
        ItemMod() {
            super(CustomColors.this);

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        push(0xffffff),
                        IRETURN,
                        end()
                    );
                }
            }.setMethod(getColorFromDamage));
        }
    }

    private class PotionMod extends ClassMod {
        PotionMod() {
            final FieldRef potionID = new FieldRef(getDeobfClass(), "id", "I");
            final FieldRef color = new FieldRef(getDeobfClass(), "color", "I");
            final FieldRef origColor = new FieldRef(getDeobfClass(), "origColor", "I");
            final FieldRef potionName = new FieldRef(getDeobfClass(), "name", "Ljava/lang/String;");
            final MethodRef setPotionName = new MethodRef(getDeobfClass(), "setPotionName", "(Ljava/lang/String;)LPotion;");
            final MethodRef setupPotion = new MethodRef(MCPatcherUtils.COLORIZE_ITEM_CLASS, "setupPotion", "(LPotion;)V");

            addClassSignature(new ConstSignature("potion.moveSpeed"));
            addClassSignature(new ConstSignature("potion.moveSlowdown"));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(setPotionName);
                    addXref(1, potionName);
                }

                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().getDescriptor().startsWith("(Ljava/lang/String;)")) {
                        return buildExpression(
                            begin(),
                            ALOAD_0,
                            ALOAD_1,
                            captureReference(PUTFIELD),
                            ALOAD_0,
                            ARETURN,
                            end()
                        );
                    } else {
                        return null;
                    }
                }
            });

            addClassSignature(new BytecodeSignature() {
                {
                    matchConstructorOnly(true);
                    addXref(1, potionID);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ILOAD_1,
                        captureReference(PUTFIELD)
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                {
                    matchConstructorOnly(true);
                    addXref(1, color);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        or(build(ILOAD_3), build(ILOAD, 4)),
                        captureReference(PUTFIELD)
                    );
                }
            });

            addPatch(new MakeMemberPublicPatch(potionName));
            addPatch(new AddFieldPatch(origColor));

            addPatch(new MakeMemberPublicPatch(color) {
                @Override
                public int getNewFlags(int oldFlags) {
                    return super.getNewFlags(oldFlags) & ~AccessFlag.FINAL;
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(setPotionName);
                }

                @Override
                public String getDescription() {
                    return "map potions by name";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ARETURN,
                        end()
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        reference(INVOKESTATIC, setupPotion)
                    );
                }
            });
        }
    }

    private class PotionHelperMod extends ClassMod {
        private static final int MAGIC = 0x385dc6;

        PotionHelperMod() {
            final MethodRef getPotionColor = new MethodRef(getDeobfClass(), "getPotionColor", "(IZ)I");
            final MethodRef integerValueOf = new MethodRef("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
            final MethodRef getPotionColorCache = new MethodRef(getDeobfClass(), "getPotionColorCache", "()Ljava/util/Map;");
            final MethodRef getWaterBottleColor = new MethodRef(MCPatcherUtils.COLORIZE_ITEM_CLASS, "getWaterBottleColor", "()I");

            addClassSignature(new ConstSignature("potion.prefix.mundane"));
            addClassSignature(new ConstSignature(MAGIC));

            final int mapOpcode;
            final JavaRef mapContains;
            final FieldRef potionColorCache;

            if (getMinecraftVersion().compareTo("14w02a") >= 0) {
                mapOpcode = INVOKEINTERFACE;
                mapContains = new InterfaceMethodRef("java/util/Map", "containsKey", "(Ljava/lang/Object;)Z");
                potionColorCache = new FieldRef(getDeobfClass(), "potionColorCache", "Ljava/util/Map;");
            } else {
                mapOpcode = INVOKEVIRTUAL;
                mapContains = new MethodRef("java/util/HashMap", "containsKey", "(Ljava/lang/Object;)Z");
                potionColorCache = new FieldRef(getDeobfClass(), "potionColorCache", "Ljava/util/HashMap;");
            }

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(getPotionColor);
                    addXref(1, potionColorCache);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // PotionHelper.potionColorCache.containsKey(id)
                        captureReference(GETSTATIC),
                        ILOAD_0,
                        reference(INVOKESTATIC, integerValueOf),
                        reference(mapOpcode, mapContains)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override water bottle color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // int i = 0x385dc6;
                        begin(),
                        push(MAGIC),
                        ISTORE_1
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // int i = ColorizeItem.getWaterBottleColor();
                        reference(INVOKESTATIC, getWaterBottleColor),
                        ISTORE_1
                    );
                }
            });

            addPatch(new AddMethodPatch(getPotionColorCache, AccessFlag.PUBLIC | AccessFlag.STATIC) {
                @Override
                public byte[] generateMethod() {
                    return buildCode(
                        reference(GETSTATIC, potionColorCache),
                        ARETURN
                    );
                }
            });
        }
    }

    private class WorldMod extends com.prupe.mcpatcher.basemod.WorldMod {
        WorldMod() {
            super(CustomColors.this);
            setInterfaces("IBlockAccess");
            mapLightningFlash();

            final MethodRef getWorldChunkManager = new MethodRef(getDeobfClass(), "getWorldChunkManager", "()LWorldChunkManager;");
            final MethodRef computeSkyColor = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "computeSkyColor", "(LWorld;F)Z");
            final MethodRef setupForFog = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "setupForFog", "(LEntity;)V");

            addMemberMapper(new MethodMapper(getWorldChunkManager));

            addPatch(new BytecodePatch() {
                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                // f8 = (f4 * 0.3f + f5 * 0.59f + f6 * 0.11f) * 0.6f;
                                FLOAD, any(),
                                push(0.3f),
                                FMUL,
                                FLOAD, any(),
                                push(0.59f),
                                FMUL,
                                FADD,
                                FLOAD, any(),
                                push(0.11f),
                                FMUL,
                                FADD,
                                push(0.6f),
                                FMUL,
                                FSTORE, any()
                            );
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "override sky color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // f4 = (float) (k >> 16 & 0xff) / 255.0f;
                        ILOAD, capture(any()),
                        push(16),
                        ISHR,
                        push(255),
                        IAND,
                        I2F,
                        push(255.0f),
                        FDIV,
                        FSTORE, capture(any()),

                        // f5 = (float) (k >> 8 & 0xff) / 255.0f;
                        ILOAD, backReference(1),
                        push(8),
                        ISHR,
                        push(255),
                        IAND,
                        I2F,
                        push(255.0f),
                        FDIV,
                        FSTORE, capture(any()),

                        // f6 = (float) (k & 0xff) / 255.0f;
                        ILOAD, backReference(1),
                        push(255),
                        IAND,
                        I2F,
                        push(255.0f),
                        FDIV,
                        FSTORE, capture(any())
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeWorld.setupForFog(entity);
                        ALOAD_1,
                        reference(INVOKESTATIC, setupForFog),

                        // if (ColorizeWorld.computeSkyColor(this, f)) {
                        ALOAD_0,
                        FLOAD_2,
                        reference(INVOKESTATIC, computeSkyColor),
                        IFEQ, branch("A"),

                        // f4 = Colorizer.setColor[0];
                        reference(GETSTATIC, setColor),
                        ICONST_0,
                        FALOAD,
                        FSTORE, getCaptureGroup(2),

                        // f5 = Colorizer.setColor[1];
                        reference(GETSTATIC, setColor),
                        ICONST_1,
                        FALOAD,
                        FSTORE, getCaptureGroup(3),

                        // f5 = Colorizer.setColor[2];
                        reference(GETSTATIC, setColor),
                        ICONST_2,
                        FALOAD,
                        FSTORE, getCaptureGroup(4),

                        // } else {
                        GOTO, branch("B"),
                        label("A"),

                        // ... original code ...
                        getMatch(),

                        // }
                        label("B")
                    );
                }
            });
        }
    }

    private class WorldProviderMod extends com.prupe.mcpatcher.basemod.WorldProviderMod {
        WorldProviderMod() {
            super(CustomColors.this);

            addClassSignature(new ConstSignature(0.06f));
            addClassSignature(new ConstSignature(0.09f));
            addClassSignature(new ConstSignature(0.91f));
            addClassSignature(new ConstSignature(0.94f));

            final FieldRef worldObj = new FieldRef(getDeobfClass(), "worldObj", "LWorld;");
            final MethodRef getFogColor = new MethodRef(getDeobfClass(), "getFogColor", "(FF)LVec3D;");
            final MethodRef computeFogColor = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "computeFogColor", "(LWorldProvider;F)Z");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        FLOAD, capture(any()),
                        FLOAD_3,
                        push(0.94f),
                        FMUL,
                        push(0.06f),
                        FADD,
                        FMUL,
                        FSTORE, backReference(1)
                    );
                }
            }.setMethod(getFogColor));

            addMemberMapper(new FieldMapper(worldObj));

            addPatch(new MakeMemberPublicPatch(worldObj));

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(getFogColor);
                }

                @Override
                public String getDescription() {
                    return "override fog color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // r = 0.7529412f;
                        // g = 0.84705883f;
                        // b = 1.0F;
                        anyLDC,
                        capture(anyFSTORE),
                        anyLDC,
                        capture(anyFSTORE),
                        push(1.0f),
                        capture(anyFSTORE)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (ColorizeWorld.computeFogColor(this, partialTick)) {
                        ALOAD_0,
                        FLOAD_1,
                        reference(INVOKESTATIC, computeFogColor),
                        IFEQ, branch("A"),

                        // r = Colorizer.setColor[0];
                        reference(GETSTATIC, setColor),
                        push(0),
                        FALOAD,
                        getCaptureGroup(1),

                        // g = Colorizer.setColor[1];
                        reference(GETSTATIC, setColor),
                        push(1),
                        FALOAD,
                        getCaptureGroup(2),

                        // b = Colorizer.setColor[2];
                        reference(GETSTATIC, setColor),
                        push(2),
                        FALOAD,
                        getCaptureGroup(3),

                        // }
                        label("A")
                    );
                }
            });
        }
    }

    private class WorldProviderHellMod extends ClassMod {
        private static final double MAGIC1 = 0.20000000298023224;
        private static final double MAGIC2 = 0.029999999329447746;

        WorldProviderHellMod() {
            setParentClass("WorldProvider");

            final MethodRef getFogColor = new MethodRef(getDeobfClass(), "getFogColor", "(FF)LVec3D;");
            final FieldRef netherFogColor = new FieldRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "netherFogColor", "[F");

            addClassSignature(new ConstSignature(MAGIC1));
            addClassSignature(new ConstSignature(MAGIC2));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(MAGIC1),
                        push(MAGIC2),
                        push(MAGIC2)
                    );
                }
            }.setMethod(getFogColor));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override nether fog color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(MAGIC1),
                        push(MAGIC2),
                        push(MAGIC2)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeWorld.netherFogColor[0], ColorizeWorld.netherFogColor[1], ColorizeWorld.netherFogColor[2]
                        reference(GETSTATIC, netherFogColor),
                        ICONST_0,
                        FALOAD,
                        F2D,

                        reference(GETSTATIC, netherFogColor),
                        ICONST_1,
                        FALOAD,
                        F2D,

                        reference(GETSTATIC, netherFogColor),
                        ICONST_2,
                        FALOAD,
                        F2D
                    );
                }
            }.targetMethod(getFogColor));
        }
    }

    private class WorldProviderEndMod extends ClassMod {
        WorldProviderEndMod() {
            setParentClass("WorldProvider");

            addClassSignature(new OrSignature(
                new ConstSignature(0x8080a0), // pre 12w23a
                new ConstSignature(0xa080a0)  // 12w23a+
            ));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        anyFLOAD,
                        F2D,
                        anyFLOAD,
                        F2D,
                        anyFLOAD,
                        F2D
                    );
                }
            });

            final MethodRef getFogColor = new MethodRef(getDeobfClass(), "getFogColor", "(FF)LVec3D;");
            final FieldRef endFogColor = new FieldRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "endFogColor", "[F");

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override end fog color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        anyFLOAD,
                        F2D,
                        anyFLOAD,
                        F2D,
                        anyFLOAD,
                        F2D
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(GETSTATIC, endFogColor),
                        ICONST_0,
                        FALOAD,
                        F2D,
                        reference(GETSTATIC, endFogColor),
                        ICONST_1,
                        FALOAD,
                        F2D,
                        reference(GETSTATIC, endFogColor),
                        ICONST_2,
                        FALOAD,
                        F2D
                    );
                }
            }.targetMethod(getFogColor));
        }
    }

    private class WorldChunkManagerMod extends ClassMod {
        WorldChunkManagerMod() {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ILOAD, 4,
                        ILOAD, 5,
                        IMUL,
                        NEWARRAY, T_FLOAT,
                        ASTORE_1
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        optional(anyReference(INVOKESTATIC)),
                        ILOAD_1,
                        ILOAD_3,
                        ISUB,
                        ICONST_2,
                        ISHR,
                        ISTORE, 6
                    );
                }
            });

            addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "getBiomeGenAt", "(" + PositionMod.getDescriptorIKOnly() + ")LBiomeGenBase;")));
        }
    }

    private class EntityMod extends ClassMod {
        EntityMod() {
            addClassSignature(new ConstSignature("Pos"));
            addClassSignature(new ConstSignature("Motion"));
            addClassSignature(new ConstSignature("Rotation"));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(new MethodRef(getDeobfClass(), "setPositionAndRotation", "(DDDFF)V"));
                    addXref(1, new FieldRef(getDeobfClass(), "posX", "D"));
                    addXref(2, new FieldRef(getDeobfClass(), "prevPosX", "D"));
                    addXref(3, new FieldRef(getDeobfClass(), "posY", "D"));
                    addXref(4, new FieldRef(getDeobfClass(), "prevPosY", "D"));
                    addXref(5, new FieldRef(getDeobfClass(), "posZ", "D"));
                    addXref(6, new FieldRef(getDeobfClass(), "prevPosZ", "D"));
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),

                        // prevPosX = posX = d;
                        ALOAD_0,
                        ALOAD_0,
                        DLOAD_1,
                        DUP2_X1,
                        captureReference(PUTFIELD),
                        captureReference(PUTFIELD),

                        // prevPosY = posY = d1;
                        ALOAD_0,
                        ALOAD_0,
                        DLOAD_3,
                        DUP2_X1,
                        captureReference(PUTFIELD),
                        captureReference(PUTFIELD),

                        // prevPosZ = posZ = d2;
                        ALOAD_0,
                        ALOAD_0,
                        DLOAD, 5,
                        DUP2_X1,
                        captureReference(PUTFIELD),
                        captureReference(PUTFIELD)
                    );
                }
            });

            addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "worldObj", "LWorld;")));
        }
    }

    private class EntityFXMod extends ClassMod {
        EntityFXMod() {
            setParentClass("Entity");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // setSize(0.2f, 0.2f);
                        ALOAD_0,
                        push(0.2f),
                        push(0.2f),
                        anyReference(INVOKEVIRTUAL)
                    );
                }
            }.matchConstructorOnly(true));

            addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // particleRed = particleGreen = particleBlue = 1.0f;
                            ALOAD_0,
                            ALOAD_0,
                            ALOAD_0,
                            FCONST_1,
                            DUP_X1,
                            captureReference(PUTFIELD),
                            DUP_X1,
                            captureReference(PUTFIELD),
                            captureReference(PUTFIELD)
                        );
                    }
                }
                    .matchConstructorOnly(true)
                    .addXref(1, new FieldRef(getDeobfClass(), "particleBlue", "F"))
                    .addXref(2, new FieldRef(getDeobfClass(), "particleGreen", "F"))
                    .addXref(3, new FieldRef(getDeobfClass(), "particleRed", "F"))
            );
        }
    }

    abstract private class WaterFXMod extends ClassMod {
        void addWaterColorPatch(final String name, final boolean includeBaseColor, final float[] particleColors) {
            addWaterColorPatch(name, includeBaseColor, particleColors, particleColors);
        }

        void addWaterColorPatch(final String name, final boolean includeBaseColor, final float[] origColors, final float[] newColors) {
            final FieldRef particleRed = new FieldRef(getDeobfClass(), "particleRed", "F");
            final FieldRef particleGreen = new FieldRef(getDeobfClass(), "particleGreen", "F");
            final FieldRef particleBlue = new FieldRef(getDeobfClass(), "particleBlue", "F");
            final FieldRef posX = new FieldRef(getDeobfClass(), "posX", "D");
            final FieldRef posY = new FieldRef(getDeobfClass(), "posY", "D");
            final FieldRef posZ = new FieldRef(getDeobfClass(), "posZ", "D");
            final MethodRef computeWaterColor1 = new MethodRef(MCPatcherUtils.COLORIZE_BLOCK_CLASS, "computeWaterColor", "(ZIII)Z");

            addPatch(new BytecodePatch() {
                {
                    if (origColors == null) {
                        setInsertBefore(true);
                    }
                }

                @Override
                public String getDescription() {
                    return "override " + name + " color";
                }

                @Override
                public String getMatchExpression() {
                    if (origColors == null) {
                        return buildExpression(
                            RETURN
                        );
                    } else {
                        return buildExpression(
                            // particleRed = r;
                            ALOAD_0,
                            push(origColors[0]),
                            reference(PUTFIELD, particleRed),

                            // particleGreen = g;
                            ALOAD_0,
                            push(origColors[1]),
                            reference(PUTFIELD, particleGreen),

                            // particleBlue = b;
                            ALOAD_0,
                            push(origColors[2]),
                            reference(PUTFIELD, particleBlue)
                        );
                    }
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (ColorizeBlock.computeWaterColor(includeBaseColor, (int) this.posX, (int) this.posY, (int) this.posZ)) {
                        push(includeBaseColor),
                        ALOAD_0,
                        reference(GETFIELD, posX),
                        D2I,
                        ALOAD_0,
                        reference(GETFIELD, posY),
                        D2I,
                        ALOAD_0,
                        reference(GETFIELD, posZ),
                        D2I,
                        reference(INVOKESTATIC, computeWaterColor1),
                        IFEQ, branch("A"),

                        // particleRed = Colorizer.setColor[0];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_0,
                        FALOAD,
                        reference(PUTFIELD, particleRed),

                        // particleGreen = Colorizer.setColor[1];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_1,
                        FALOAD,
                        reference(PUTFIELD, particleGreen),

                        // particleBlue = Colorizer.setColor[2];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, particleBlue),
                        GOTO, branch("B"),

                        // } else {
                        label("A"),

                        newColors == null ? new byte[]{} : buildCode(
                            // particleRed = r;
                            ALOAD_0,
                            push(newColors[0]),
                            reference(PUTFIELD, particleRed),

                            // particleGreen = g;
                            ALOAD_0,
                            push(newColors[1]),
                            reference(PUTFIELD, particleGreen),

                            // particleBlue = b;
                            ALOAD_0,
                            push(newColors[2]),
                            reference(PUTFIELD, particleBlue)
                        ),

                        // }
                        label("B")
                    );
                }
            }.matchConstructorOnly(true));
        }
    }

    private class EntityRainFXMod extends WaterFXMod {
        EntityRainFXMod() {
            setParentClass("EntityFX");

            final MethodRef random = new MethodRef("java/lang/Math", "random", "()D");

            addClassSignature(new OrSignature(
                new ConstSignature(0.1f),
                new ConstSignature((double) 0.1f) // 14w02a+
            ));

            addClassSignature(new OrSignature(
                new ConstSignature(0.2f),
                new ConstSignature((double) 0.2f) // 14w02a+
            ));

            addClassSignature(new ConstSignature(0.30000001192092896));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (Math.random() < 0.5)
                        reference(INVOKESTATIC, random),
                        push(0.5),
                        DCMPG,
                        IFGE, any(2)
                    );
                }
            });

            addWaterColorPatch("rain drop", false, new float[]{1.0f, 1.0f, 1.0f}, new float[]{0.2f, 0.3f, 1.0f});
        }
    }

    private class EntityDropParticleFXMod extends WaterFXMod {
        EntityDropParticleFXMod() {
            setParentClass("EntityFX");

            final FieldRef particleRed = new FieldRef(getDeobfClass(), "particleRed", "F");
            final FieldRef particleGreen = new FieldRef(getDeobfClass(), "particleGreen", "F");
            final FieldRef particleBlue = new FieldRef(getDeobfClass(), "particleBlue", "F");
            final FieldRef timer = new FieldRef(getDeobfClass(), "timer", "I");
            final MethodRef onUpdate = new MethodRef(getDeobfClass(), "onUpdate", "()V");
            final MethodRef computeLavaDropColor = new MethodRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "computeLavaDropColor", "(I)Z");

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(onUpdate);
                    addXref(1, new FieldRef(getDeobfClass(), "timer", "I"));
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // particleRed = 0.2f;
                        ALOAD_0,
                        push(0.2f),
                        anyReference(PUTFIELD),

                        // particleGreen = 0.3f;
                        ALOAD_0,
                        push(0.3f),
                        anyReference(PUTFIELD),

                        // particleBlue = 1.0f;
                        ALOAD_0,
                        push(1.0f),
                        anyReference(PUTFIELD),

                        // ...
                        any(0, 30),

                        // 40 - age
                        push(40),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ISUB
                    );
                }
            });

            addWaterColorPatch("water drop", true, new float[]{0.0f, 0.0f, 1.0f}, new float[]{0.2f, 0.3f, 1.0f});

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "remove water drop color update";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // particleRed = 0.2f;
                        ALOAD_0,
                        push(0.2f),
                        reference(PUTFIELD, particleRed),

                        // particleGreen = 0.3f;
                        ALOAD_0,
                        push(0.3f),
                        reference(PUTFIELD, particleGreen),

                        // particleBlue = 1.0f;
                        ALOAD_0,
                        push(1.0f),
                        reference(PUTFIELD, particleBlue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode();
                }
            }.targetMethod(onUpdate));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override lava drop color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // particleRed = 1.0f;
                        ALOAD_0,
                        push(1.0f),
                        reference(PUTFIELD, particleRed),

                        // particleGreen = 16.0f / (float)((40 - timer) + 16);
                        ALOAD_0,
                        push(16.0f),
                        any(0, 20),
                        reference(PUTFIELD, particleGreen),

                        // particleBlue = 4.0f / (float)((40 - timer) + 8);
                        ALOAD_0,
                        push(4.0f),
                        any(0, 20),
                        reference(PUTFIELD, particleBlue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (Colorizer.computeLavaDropColor(40 - timer)) {
                        push(40),
                        ALOAD_0,
                        reference(GETFIELD, timer),
                        ISUB,
                        reference(INVOKESTATIC, computeLavaDropColor),
                        IFEQ, branch("A"),

                        // particleRed = Colorizer.setColor[0];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_0,
                        FALOAD,
                        reference(PUTFIELD, particleRed),

                        // particleGreen = Colorizer.setColor[1];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_1,
                        FALOAD,
                        reference(PUTFIELD, particleGreen),

                        // particleBlue = Colorizer.setColor[2];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, particleBlue),

                        // } else {
                        GOTO, branch("B"),

                        // ... original code ...
                        label("A"),
                        getMatch(),

                        // }
                        label("B")
                    );
                }
            }.targetMethod(onUpdate));
        }
    }

    private class EntitySplashFXMod extends WaterFXMod {
        EntitySplashFXMod() {
            setParentClass("EntityRainFX");

            addClassSignature(new ConstSignature(0.04f));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        DLOAD, 8,
                        anyReference(PUTFIELD),

                        ALOAD_0,
                        DLOAD, 10,
                        push(0.10000000000000001),
                        DADD,
                        anyReference(PUTFIELD),

                        ALOAD_0,
                        DLOAD, 12,
                        anyReference(PUTFIELD)
                    );
                }
            }.matchConstructorOnly(true));

            addWaterColorPatch("splash", false, null);
        }
    }

    private class EntityBubbleFXMod extends WaterFXMod {
        EntityBubbleFXMod() {
            setParentClass("EntityFX");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // setParticleTextureIndex(32);
                        ALOAD_0,
                        push(32),
                        anyReference(INVOKEVIRTUAL),

                        // setSize(0.02F, 0.02F);
                        ALOAD_0,
                        push(0.02f),
                        push(0.02f),
                        anyReference(INVOKEVIRTUAL)
                    );
                }
            }.matchConstructorOnly(true));

            addWaterColorPatch("bubble", false, new float[]{1.0f, 1.0f, 1.0f});
        }
    }

    private class EntitySuspendFXMod extends ClassMod {
        EntitySuspendFXMod() {
            setParentClass("EntityFX");

            final FieldRef particleRed = new FieldRef(getDeobfClass(), "particleRed", "F");
            final FieldRef particleGreen = new FieldRef(getDeobfClass(), "particleGreen", "F");
            final FieldRef particleBlue = new FieldRef(getDeobfClass(), "particleBlue", "F");

            addClassSignature(new ConstSignature(0.4f));
            addClassSignature(new ConstSignature(0.7f));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(0.01f),
                        push(0.01f),
                        anyReference(INVOKEVIRTUAL)
                    );
                }
            }.matchConstructorOnly(true));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override underwater suspend particle color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(0.4f),
                        reference(PUTFIELD, particleRed),

                        ALOAD_0,
                        push(0.4f),
                        reference(PUTFIELD, particleGreen),

                        ALOAD_0,
                        push(0.7f),
                        reference(PUTFIELD, particleBlue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeEntity.computeSuspendColor(0x6666b2, (int) x, (int) y, (int) z);
                        push(0x6666b2),
                        DLOAD_2,
                        D2I,
                        DLOAD, 4,
                        D2I,
                        DLOAD, 6,
                        D2I,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "computeSuspendColor", "(IIII)V")),

                        // this.particleRed = Colorizer.setColor[0];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(0),
                        FALOAD,
                        reference(PUTFIELD, particleRed),

                        // this.particleGreen = Colorizer.setColor[1];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(1),
                        FALOAD,
                        reference(PUTFIELD, particleGreen),

                        // this.particleBlue = Colorizer.setColor[2];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(2),
                        FALOAD,
                        reference(PUTFIELD, particleBlue)
                    );
                }
            }.matchConstructorOnly(true));
        }
    }

    private class EntityPortalFXMod extends ClassMod {
        private final FieldRef portalColor = new FieldRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "portalColor", "[F");

        EntityPortalFXMod() {
            setParentClass("EntityFX");

            final FieldRef particleBlue = new FieldRef(getDeobfClass(), "particleBlue", "F");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // particleGreen *= 0.3f;
                        ALOAD_0,
                        DUP,
                        GETFIELD, capture(any(2)),
                        push(0.3f),
                        FMUL,
                        PUTFIELD, backReference(1),

                        // particleBlue *= 0.9f;
                        ALOAD_0,
                        DUP,
                        GETFIELD, capture(any(2)),
                        push(0.9f),
                        FMUL,
                        PUTFIELD, backReference(2)
                    );
                }
            }.matchConstructorOnly(true));

            addPortalPatch(0.9f, 0, "red");
            addPortalPatch(0.3f, 1, "green");

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    matchConstructorOnly(true);
                }

                @Override
                public String getDescription() {
                    return "override portal particle color (blue)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        RETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        reference(GETSTATIC, portalColor),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, particleBlue)
                    );
                }
            });
        }

        private void addPortalPatch(final float origValue, final int index, final String color) {
            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override portal particle color (" + color + ")";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(origValue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(GETSTATIC, portalColor),
                        push(index),
                        FALOAD
                    );
                }
            }.matchConstructorOnly(true));
        }
    }

    private class EntityAuraFXMod extends ClassMod {
        EntityAuraFXMod() {
            setParentClass("EntityFX");

            final FieldRef particleRed = new FieldRef(getDeobfClass(), "particleRed", "F");
            final FieldRef particleGreen = new FieldRef(getDeobfClass(), "particleGreen", "F");
            final FieldRef particleBlue = new FieldRef(getDeobfClass(), "particleBlue", "F");
            final MethodRef computeMyceliumParticleColor = new MethodRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "computeMyceliumParticleColor", "()Z");

            addClassSignature(new ConstSignature(0.019999999552965164));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.setParticleTextureIndex(0);
                        ALOAD_0,
                        push(0),
                        anyReference(INVOKEVIRTUAL),

                        // this.setSize(0.02f, 0.02f);
                        ALOAD_0,
                        push(0.02f),
                        push(0.02f),
                        anyReference(INVOKEVIRTUAL)
                    );
                }
            }.matchConstructorOnly(true));

            addPatch(new AddMethodPatch(new MethodRef(getDeobfClass(), "colorize", "()LEntityAuraFX;")) {
                @Override
                public byte[] generateMethod() {
                    return buildCode(
                        reference(INVOKESTATIC, computeMyceliumParticleColor),
                        IFEQ, branch("A"),

                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_0,
                        FALOAD,
                        reference(PUTFIELD, particleRed),

                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_1,
                        FALOAD,
                        reference(PUTFIELD, particleGreen),

                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        ICONST_2,
                        FALOAD,
                        reference(PUTFIELD, particleBlue),

                        label("A"),
                        ALOAD_0,
                        ARETURN
                    );
                }
            });
        }
    }

    private class EntityLivingBaseMod extends com.prupe.mcpatcher.basemod.EntityLivingBaseMod {
        public EntityLivingBaseMod() {
            super(CustomColors.this);

            final FieldRef worldObj = new FieldRef(getDeobfClass(), "worldObj", "LWorld;");
            final FieldRef overridePotionColor = new FieldRef(getDeobfClass(), "overridePotionColor", "I");
            final MethodRef updatePotionEffects = new MethodRef(getDeobfClass(), "updatePotionEffects", "()V");
            final MethodRef integerValueOf = new MethodRef("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(600),
                        IREM
                    );
                }
            }.setMethod(updatePotionEffects));

            addPatch(new AddFieldPatch(overridePotionColor));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override potion effect colors around players (part 1)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (this.potionsNeedUpdate) {
                        lookBehind(build(
                            ALOAD_0,
                            GETFIELD, capture(any(2)),
                            IFEQ, any(2)
                        ), true),

                        // if (!this.worldObj.isRemote) {
                        ALOAD_0,
                        reference(GETFIELD, worldObj),
                        captureReference(GETFIELD),
                        IFNE, any(2)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                    );
                }
            }.targetMethod(updatePotionEffects));

            addPatch(new BytecodePatch() {
                {
                    targetMethod(updatePotionEffects);
                    setInsertAfter(true);
                }

                @Override
                public String getDescription() {
                    return "override potion effect colors around players (part 2)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.dataWatcher.updateObject(7, Integer.valueOf(...));
                        ALOAD_0,
                        anyReference(GETFIELD),
                        push(7),
                        capture(any(1, 3)),
                        reference(INVOKESTATIC, integerValueOf),
                        anyReference(INVOKEVIRTUAL)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // this.overridePotionColor = ...;
                        ALOAD_0,
                        getCaptureGroup(1),
                        reference(PUTFIELD, overridePotionColor)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(updatePotionEffects);
                }

                @Override
                public String getDescription() {
                    return "override potion effect colors around players (part 3)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // this.dataWatcher.getWatchableObjectInt(7)
                        ALOAD_0,
                        anyReference(GETFIELD),
                        push(7),
                        anyReference(INVOKEVIRTUAL)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // ColorizeEntity.getPotionEffectColor(..., this)
                        ALOAD_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "getPotionEffectColor", "(ILEntityLivingBase;)I"))
                    );
                }
            });
        }
    }

    private class EntityRendererMod extends ClassMod {
        EntityRendererMod() {
            final MethodRef updateLightmap = new MethodRef(getDeobfClass(), "updateLightmap", "(F)V");
            final FieldRef mc = new FieldRef(getDeobfClass(), "mc", "LMinecraft;");
            final MethodRef updateFogColor = new MethodRef(getDeobfClass(), "updateFogColor", "(F)V");
            final FieldRef fogColorRed = new FieldRef(getDeobfClass(), "fogColorRed", "F");
            final FieldRef fogColorGreen = new FieldRef(getDeobfClass(), "fogColorGreen", "F");
            final FieldRef fogColorBlue = new FieldRef(getDeobfClass(), "fogColorBlue", "F");
            final FieldRef lightmapColors = new FieldRef(getDeobfClass(), "lightmapColors", "[I");
            final FieldRef lightmapTexture = new FieldRef(getDeobfClass(), "lightmapTexture", ResourceLocationMod.select("I", "LDynamicTexture;"));
            final FieldRef needLightmapUpdate = new FieldRef(getDeobfClass(), "needLightmapUpdate", "Z");
            final FieldRef renderEngine = new FieldRef("Minecraft", "renderEngine", "LRenderEngine;");
            final MethodRef createTextureFromBytes = new MethodRef("RenderEngine", "createTextureFromBytes", "([IIII)V");
            final FieldRef thePlayer = new FieldRef("Minecraft", "thePlayer", "LEntityClientPlayerMP;");
            final FieldRef nightVision = new FieldRef("Potion", "nightVision", "LPotion;");
            final MethodRef isPotionActive = new MethodRef("EntityClientPlayerMP", "isPotionActive", "(LPotion;)Z");
            final String nvEntity = getMinecraftVersion().compareTo("14w06a") >= 0 ? "LEntityLivingBase;" : "LEntityPlayer;";
            final MethodRef getNightVisionStrength1 = new MethodRef(getDeobfClass(), "getNightVisionStrength1", "(" + nvEntity + "F)F");
            final MethodRef getNightVisionStrength = new MethodRef(getDeobfClass(), "getNightVisionStrength", "(F)F");
            final MethodRef reloadTexture = new MethodRef("DynamicTexture", "reload", "()V");
            final MethodRef computeUnderwaterColor = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "computeUnderwaterColor", "()Z");

            addClassSignature(new ConstSignature("ambient.weather.rain"));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(updateLightmap);
                    addXref(1, new MethodRef("World", "getSunAngle", "(F)F"));
                    addXref(2, new FieldRef("World", "worldProvider", "LWorldProvider;"));
                    addXref(3, new FieldRef(getDeobfClass(), "torchFlickerX", "F"));
                    addXref(4, WorldMod.getLightningFlashRef());
                    addXref(5, WorldProviderMod.getWorldTypeRef());
                    addXref(6, mc);
                    addXref(7, new FieldRef("Minecraft", "gameSettings", "LGameSettings;"));
                    addXref(8, new FieldRef("GameSettings", "gammaSetting", "F"));
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // sun = world.func_35464_b(1.0F) * 0.95F + 0.05F;
                        ALOAD_2,
                        push(1.0f),
                        captureReference(INVOKEVIRTUAL),
                        push(0.95f),
                        FMUL,
                        push(0.05f),
                        FADD,
                        FSTORE, 4,

                        // older: lightsun = world.worldProvider.lightBrightnessTable[i / 16] * sun;
                        // 14w02a+: lightsun = world.worldProvider.getLightBrightnessTable()[i / 16] * sun;
                        ALOAD_2,
                        captureReference(GETFIELD),
                        or(anyReference(GETFIELD), anyReference(INVOKEVIRTUAL)),
                        ILOAD_3,
                        BIPUSH, 16,
                        IDIV,
                        FALOAD,
                        FLOAD, 4,
                        FMUL,
                        FSTORE, 5,

                        // older: lighttorch = world.worldProvider.lightBrightnessTable[i % 16] * (torchFlickerX * 0.1f + 1.5f);
                        // 14w02a+: lighttorch = world.worldProvider.getLightBrightnessTable()[i % 16] * (torchFlickerX * 0.1f + 1.5f);
                        any(0, 20),
                        ILOAD_3,
                        BIPUSH, 16,
                        IREM,
                        FALOAD,
                        ALOAD_0,
                        captureReference(GETFIELD),

                        // ...
                        any(0, 200),

                        // older: if (world.lightningFlash > 0)
                        // 14w02a+: if (world.getLightningFlash() > 0)
                        ALOAD_2,
                        captureReference(WorldMod.getLightningFlashOpcode()),
                        IFLE, any(2),

                        // ...
                        any(0, 300),

                        // older: if (world.worldProvider.worldType == 1) {
                        // 14w02a+: if (world.worldProvider.getWorldType() == 1) {
                        ALOAD_2,
                        backReference(2),
                        captureReference(WorldProviderMod.getWorldTypeOpcode()),
                        ICONST_1,
                        IF_ICMPNE, any(2),

                        // ...
                        any(0, 200),

                        // gamma = mc.gameSettings.gammaSetting;
                        ALOAD_0,
                        captureReference(GETFIELD),
                        captureReference(GETFIELD),
                        captureReference(GETFIELD),
                        FSTORE, 16,

                        // ...
                        any(0, 300),

                        ResourceLocationMod.haveClass() ? getSubExpression16() : getSubExpression15(),
                        RETURN
                    );
                }

                private String getSubExpression15() {
                    addXref(9, renderEngine);
                    addXref(10, lightmapColors);
                    addXref(11, lightmapTexture);
                    addXref(12, createTextureFromBytes);
                    return buildExpression(
                        // this.mc.renderEngine.createTextureFromBytes(this.lightmapColors, 16, 16, this.lightmapTexture);
                        ALOAD_0,
                        backReference(6),
                        captureReference(GETFIELD),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        push(16),
                        push(16),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        captureReference(INVOKEVIRTUAL)
                    );
                }

                private String getSubExpression16() {
                    addXref(9, lightmapColors);
                    addXref(10, lightmapTexture);
                    addXref(11, reloadTexture);
                    addXref(12, needLightmapUpdate);
                    return buildExpression(
                        // this.lightmapColors[i] = ...;
                        ALOAD_0,
                        captureReference(GETFIELD),
                        any(0, 50),
                        IASTORE,

                        // ...
                        any(0, 20),

                        // this.lightmapTexture.load();
                        // this.needLightmapUpdate = false;
                        ALOAD_0,
                        captureReference(GETFIELD),
                        captureReference(INVOKEVIRTUAL),
                        ALOAD_0,
                        push(0),
                        captureReference(PUTFIELD),

                        // ...
                        any(0, 20)
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // fogColorRed = 0.02f;
                            ALOAD_0,
                            push(0.02f),
                            capture(optional(build( // 13w16a+
                                anyFLOAD,
                                FADD
                            ))),
                            captureReference(PUTFIELD),

                            // fogColorGreen = 0.02f;
                            ALOAD_0,
                            push(0.02f),
                            backReference(1),
                            captureReference(PUTFIELD),

                            // fogColorBlue = 0.2f;
                            ALOAD_0,
                            push(0.2f),
                            backReference(1),
                            captureReference(PUTFIELD)
                        );
                    }
                }
                    .setMethod(updateFogColor)
                    .addXref(2, fogColorRed)
                    .addXref(3, fogColorGreen)
                    .addXref(4, fogColorBlue)
            );

            addClassSignature(new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // if (mc.thePlayer.isPotionActive(Potion.nightVision)) {
                            capture(build(
                                ALOAD_0,
                                captureReference(GETFIELD),
                                captureReference(GETFIELD),
                                captureReference(GETSTATIC),
                                captureReference(INVOKEVIRTUAL)
                            )),
                            IFEQ, any(2),

                            // var16 = getNightVisionStrength1(mc.thePlayer, var1);
                            capture(build(
                                ALOAD_0,
                                ALOAD_0,
                                backReference(2),
                                backReference(3),
                                FLOAD_1,
                                captureReference(INVOKESPECIAL)
                            )),
                            FSTORE, any()
                        );
                    }
                }
                    .setMethod(updateLightmap)
                    .addXref(2, mc)
                    .addXref(3, thePlayer)
                    .addXref(4, nightVision)
                    .addXref(5, isPotionActive)
                    .addXref(7, getNightVisionStrength1)
            );

            addPatch(new AddMethodPatch(getNightVisionStrength) {
                @Override
                public byte[] generateMethod() {
                    return buildCode(
                        ALOAD_0,
                        reference(GETFIELD, mc),
                        reference(GETFIELD, thePlayer),
                        reference(GETSTATIC, nightVision),
                        reference(INVOKEVIRTUAL, isPotionActive),
                        IFEQ, branch("A"),

                        ALOAD_0,
                        ALOAD_0,
                        reference(GETFIELD, mc),
                        reference(GETFIELD, thePlayer),
                        FLOAD_1,
                        reference(INVOKESPECIAL, getNightVisionStrength1),
                        FRETURN,

                        label("A"),
                        push(0.0f),
                        FRETURN
                    );
                }
            });

            addPatch(new MakeMemberPublicPatch(new FieldRef(getDeobfClass(), "torchFlickerX", "F")));

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(updateLightmap);
                }

                @Override
                public String getDescription() {
                    return "override lightmap";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ASTORE_2
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (Lightmap.computeLightmap(this, world, this.lightmapColors, partialTick)) {
                        ALOAD_0,
                        ALOAD_2,
                        ALOAD_0,
                        reference(GETFIELD, lightmapColors),
                        FLOAD_1,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.LIGHTMAP_CLASS, "computeLightmap", "(LEntityRenderer;LWorld;[IF)Z")),
                        IFEQ, branch("A"),

                        ResourceLocationMod.haveClass() ? loadTexture16() : loadTexture15(),

                        // return;
                        RETURN,

                        // }
                        label("A")
                    );
                }

                private byte[] loadTexture15() {
                    return buildCode(
                        // this.mc.renderEngine.createTextureFromBytes(this.lightmapColors, 16, 16, this.lightmapTexture);
                        ALOAD_0,
                        reference(GETFIELD, mc),
                        reference(GETFIELD, renderEngine),
                        ALOAD_0,
                        reference(GETFIELD, lightmapColors),
                        push(16),
                        push(16),
                        ALOAD_0,
                        reference(GETFIELD, lightmapTexture),
                        reference(INVOKEVIRTUAL, createTextureFromBytes)
                    );
                }

                private byte[] loadTexture16() {
                    return buildCode(
                        // this.lightmapTexture.load();
                        // this.needLightmapUpdate = false;
                        ALOAD_0,
                        reference(GETFIELD, lightmapTexture),
                        reference(INVOKEVIRTUAL, reloadTexture),
                        ALOAD_0,
                        push(0),
                        reference(PUTFIELD, needLightmapUpdate)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(updateFogColor);
                }

                @Override
                public String getDescription() {
                    return "override underwater ambient color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // fogColorRed = 0.02f;
                        ALOAD_0,
                        push(0.02f),
                        capture(optional(build( // 13w16a+
                            anyFLOAD,
                            FADD
                        ))),
                        reference(PUTFIELD, fogColorRed),

                        // fogColorGreen = 0.02f;
                        ALOAD_0,
                        push(0.02f),
                        backReference(1),
                        reference(PUTFIELD, fogColorGreen),

                        // fogColorBlue = 0.2f;
                        ALOAD_0,
                        push(0.2f),
                        backReference(1),
                        reference(PUTFIELD, fogColorBlue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // if (ColorizeWorld.computeUnderwaterColor()) {
                        reference(INVOKESTATIC, computeUnderwaterColor),
                        IFEQ, branch("A"),

                        // fogColorRed = Colorizer.setColor[0];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(0),
                        FALOAD,
                        getCaptureGroup(1),
                        reference(PUTFIELD, fogColorRed),

                        // fogColorGreen = Colorizer.setColor[1];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(1),
                        FALOAD,
                        getCaptureGroup(1),
                        reference(PUTFIELD, fogColorGreen),

                        // fogColorBlue = Colorizer.setColor[2];
                        ALOAD_0,
                        reference(GETSTATIC, setColor),
                        push(2),
                        FALOAD,
                        getCaptureGroup(1),
                        reference(PUTFIELD, fogColorBlue),

                        // }
                        label("A")
                    );
                }
            });
        }
    }

    private class EntityReddustFXMod extends ClassMod {
        EntityReddustFXMod() {
            final MethodRef random = new MethodRef("java/lang/Math", "random", "()D");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        reference(INVOKESTATIC, random),
                        push(0.20000000298023224),
                        DMUL,
                        D2F,
                        push(0.8f),
                        FADD,
                        anyFLOAD,
                        FMUL,
                        anyFLOAD,
                        FMUL,
                        anyReference(PUTFIELD)
                    );
                }
            }.matchConstructorOnly(true));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override redstone particle color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(1.0f),
                        FSTORE, 9,
                        reference(INVOKESTATIC, random)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        push(1.0f),
                        FSTORE, 9,

                        push(15),
                        reference(INVOKESTATIC, computeRedstoneWireColor),
                        IFEQ, branch("A"),

                        reference(GETSTATIC, setColor),
                        push(0),
                        FALOAD,
                        FSTORE, 9,
                        reference(GETSTATIC, setColor),
                        push(1),
                        FALOAD,
                        FSTORE, 10,
                        reference(GETSTATIC, setColor),
                        push(2),
                        FALOAD,
                        FSTORE, 11,

                        label("A"),
                        reference(INVOKESTATIC, random)
                    );
                }
            }.matchConstructorOnly(true));
        }
    }

    private class RenderGlobalMod extends ClassMod {
        RenderGlobalMod() {
            final FieldRef clouds;
            final FieldRef mc = new FieldRef(getDeobfClass(), "mc", "LMinecraft;");
            final FieldRef gameSettings = new FieldRef("Minecraft", "gameSettings", "LGameSettings;");
            final FieldRef fancyGraphics = new FieldRef("GameSettings", "fancyGraphics", "Z");
            final boolean intParam = getMinecraftVersion().compareTo("14w25a") >= 0;
            final MethodRef renderClouds = new MethodRef(getDeobfClass(), "renderClouds", "(F" + (intParam ? "I" : "") + ")V");
            final MethodRef renderCloudsFancy = new MethodRef(getDeobfClass(), "renderCloudsFancy", renderClouds.getType());
            final MethodRef drawFancyClouds = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "drawFancyClouds", "(Z)Z");
            final FieldRef endSkyColor = new FieldRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "endSkyColor", "I");

            RenderUtilsMod.setup(this);

            if (ResourceLocationMod.haveClass()) {
                clouds = new FieldRef(getDeobfClass(), "clouds", "LResourceLocation;");
                addClassSignature(new ResourceLocationSignature(this, clouds, "textures/environment/clouds.png"));
            } else {
                addClassSignature(new ConstSignature("/environment/clouds.png"));
                clouds = null;
            }

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(renderClouds);
                    addXref(1, mc);
                    addXref(2, gameSettings);
                    addXref(3, fancyGraphics);
                    addXref(4, renderCloudsFancy);
                    if (clouds != null) {
                        addXref(5, clouds);
                    }
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (mc.gameSettings.fancyGraphics) {
                        ALOAD_0,
                        captureReference(GETFIELD),
                        captureReference(GETFIELD),
                        captureReference(GETFIELD),
                        IFEQ, any(2),

                        // this.renderCloudsFancy(...);
                        ALOAD_0,
                        FLOAD_1,
                        intParam ? build(ILOAD_2) : "",
                        captureReference(INVOKESPECIAL),
                        or(build(GOTO, any(2)), build(RETURN)),

                        // ...
                        any(0, 150),

                        // ...(RenderGlobal.clouds);
                        // ...("/environment/clouds.png");
                        clouds == null ? push("/environment/clouds.png") : captureReference(GETSTATIC)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override cloud type";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        capture(build(
                            ALOAD_0,
                            reference(GETFIELD, mc),
                            reference(GETFIELD, gameSettings),
                            reference(GETFIELD, fancyGraphics)
                        )),
                        capture(build(
                            IFEQ, any(2)
                        ))
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, drawFancyClouds),
                        getCaptureGroup(2)
                    );
                }
            }.targetMethod(renderClouds));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override end sky color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(or(
                        build(push(0x181818)), // pre-12w23a
                        build(push(0x282828))  // 12w23a+
                    ));
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(GETSTATIC, endSkyColor)
                    );
                }
            });
        }
    }

    private class EntityListMod extends ClassMod {
        EntityListMod() {
            addClassSignature(new ConstSignature("Skipping Entity with id "));

            final MethodRef addMapping = new MethodRef(getDeobfClass(), "addMapping", "(Ljava/lang/Class;Ljava/lang/String;III)V");
            final MethodRef setupSpawnerEgg = new MethodRef(MCPatcherUtils.COLORIZE_ITEM_CLASS, "setupSpawnerEgg", "(Ljava/lang/String;III)V");

            addMemberMapper(new MethodMapper(addMapping).accessFlag(AccessFlag.STATIC, true));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set up mapping for spawnable entities";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_1,
                        ILOAD_2,
                        ILOAD_3,
                        ILOAD, 4,
                        reference(INVOKESTATIC, setupSpawnerEgg)
                    );
                }
            }.targetMethod(addMapping));
        }
    }

    private class ItemSpawnerEggMod extends ClassMod {
        ItemSpawnerEggMod() {
            final MethodRef getColorFromDamage2 = new MethodRef(getDeobfClass(), getColorFromDamage.getName(), getColorFromDamage.getType());
            final MethodRef getItemNameIS = new MethodRef(getDeobfClass(), "getItemNameIS", "(LItemStack;)Ljava/lang/String;");
            final MethodRef getItemDamage = new MethodRef("ItemStack", "getItemDamage", "()I");
            final MethodRef getEntityString = new MethodRef("EntityList", "getEntityString", "(I)Ljava/lang/String;");
            final MethodRef colorizeSpawnerEgg = new MethodRef(MCPatcherUtils.COLORIZE_ITEM_CLASS, "colorizeSpawnerEgg", "(III)I");

            setParentClass("Item");

            addClassSignature(new ConstSignature(".name"));
            addClassSignature(new ConstSignature("entity."));

            addClassSignature(new BytecodeSignature() {
                {
                    setMethod(getItemNameIS);
                    addXref(1, getItemDamage);
                    addXref(2, getEntityString);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // s1 = EntityList.getEntityString(itemStack.getItemDamage());
                        ALOAD_1,
                        captureReference(INVOKEVIRTUAL),
                        captureReference(INVOKESTATIC),
                        ASTORE_3,
                        ALOAD_3
                    );
                }
            });

            addClassSignature(new OrSignature(
                new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            // 64 + (i * 0x24faef & 0xc0)
                            BIPUSH, 64,
                            ILOAD_1,
                            push(0x24faef),
                            IMUL,
                            push(0xc0),
                            IAND,
                            IADD
                        );
                    }
                }.setMethod(getColorFromDamage),

                new BytecodeSignature() {
                    @Override
                    public String getMatchExpression() {
                        return buildExpression(
                            push(0xffffff),
                            IRETURN
                        );
                    }
                }.setMethod(getColorFromDamage)
            ));

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(getColorFromDamage2);
                }

                @Override
                public String getDescription() {
                    return "override spawner egg color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        IRETURN
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_1,
                        reference(INVOKEVIRTUAL, getItemDamage),
                        ILOAD_2,
                        reference(INVOKESTATIC, colorizeSpawnerEgg)
                    );
                }
            });
        }
    }

    private class MapColorMod extends ClassMod {
        MapColorMod() {
            final FieldRef mapColorArray = new FieldRef(getDeobfClass(), "mapColorArray", "[LMapColor;");
            final FieldRef colorValue = new FieldRef(getDeobfClass(), "colorValue", "I");
            final FieldRef colorIndex = new FieldRef(getDeobfClass(), "colorIndex", "I");
            final FieldRef origColorValue = new FieldRef(getDeobfClass(), "origColorValue", "I");

            addClassSignature(new ConstSignature(0x7fb238));
            addClassSignature(new ConstSignature(0xf7e9a3));
            addClassSignature(new ConstSignature(0xa7a7a7));
            addClassSignature(new ConstSignature(0xff0000));
            addClassSignature(new ConstSignature(0xa0a0ff));

            addMemberMapper(new FieldMapper(mapColorArray).accessFlag(AccessFlag.STATIC, true));
            addMemberMapper(new FieldMapper(colorValue, colorIndex).accessFlag(AccessFlag.STATIC, false));

            addPatch(new AddFieldPatch(origColorValue));

            addPatch(new MakeMemberPublicPatch(colorValue) {
                @Override
                public int getNewFlags(int oldFlags) {
                    return oldFlags & ~AccessFlag.FINAL;
                }
            });

            addPatch(new BytecodePatch() {
                {
                    setInsertAfter(true);
                    targetMethod(new MethodRef(getDeobfClass(), "<init>", "(II)V"));
                }

                @Override
                public String getDescription() {
                    return "set map origColorValue";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        ILOAD_2,
                        reference(PUTFIELD, colorValue)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        ILOAD_2,
                        reference(PUTFIELD, origColorValue)
                    );
                }
            });
        }
    }

    private class ItemDyeMod extends ClassMod {
        private final FieldRef dyeColorNames = new FieldRef(getDeobfClass(), "dyeColorNames", "[Ljava/lang/String;");
        private final FieldRef dyeColors = new FieldRef(getDeobfClass(), "dyeColors", "[I");

        ItemDyeMod() {
            addClassSignature(new ConstSignature("black"));
            addClassSignature(new ConstSignature("purple"));
            addClassSignature(new ConstSignature("cyan"));

            if (IBlockStateMod.haveClass()) {
                setup18();
            } else {
                setup17();
            }
        }

        private void setup17() {
            setParentClass("Item");

            addClassSignature(new ConstSignature(0x1e1b1b));

            addMemberMapper(new FieldMapper(dyeColorNames)
                    .accessFlag(AccessFlag.PUBLIC, true)
                    .accessFlag(AccessFlag.STATIC, true)
            );
            addMemberMapper(new FieldMapper(dyeColors)
                    .accessFlag(AccessFlag.PUBLIC, true)
                    .accessFlag(AccessFlag.STATIC, true)
            );
        }

        // TODO
        private void setup18() {
            setInterfaces("INamed");

            addPatch(new AddFieldPatch(dyeColorNames, AccessFlag.PUBLIC | AccessFlag.STATIC));
            addPatch(new AddFieldPatch(dyeColors, AccessFlag.PUBLIC | AccessFlag.STATIC));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "initialize arrays";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // dyeColorNames = new String[16];
                        push(16),
                        reference(ANEWARRAY, new ClassRef("java/lang/String")),
                        reference(PUTSTATIC, dyeColorNames),

                        // dyeColors = new int[16];
                        push(16),
                        NEWARRAY, T_INT,
                        reference(PUTSTATIC, dyeColors)
                    );
                }
            }.matchStaticInitializerOnly(true));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "set color names";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // dyeColorNames[ordinal] = name;
                        reference(GETSTATIC, dyeColorNames),
                        ILOAD_2,
                        ALOAD, 6,
                        AASTORE
                    );
                }
            }.matchConstructorOnly(true));
        }
    }

    private class ItemArmorMod extends ClassMod {
        private final int DEFAULT_LEATHER_COLOR = 0xa06540;

        ItemArmorMod() {
            final FieldRef undyedLeatherColor = new FieldRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "undyedLeatherColor", "I");

            addClassSignature(new ConstSignature("display"));
            addClassSignature(new ConstSignature("color"));
            addClassSignature(new ConstSignature(DEFAULT_LEATHER_COLOR));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override default leather armor color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(DEFAULT_LEATHER_COLOR)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(GETSTATIC, undyedLeatherColor)
                    );
                }
            });
        }
    }

    private class EntitySheepMod extends ClassMod {
        EntitySheepMod() {
            addClassSignature(new ConstSignature("mob.sheep.say"));

            if (IBlockStateMod.haveClass()) {
                setup18();
            } else {
                setup17();
            }
        }

        private void setup17() {
            addMemberMapper(new FieldMapper(fleeceColorTable)
                    .accessFlag(AccessFlag.PUBLIC, true)
                    .accessFlag(AccessFlag.STATIC, true)
            );
        }

        // TODO
        private void setup18() {
            addPatch(new AddFieldPatch(fleeceColorTable, AccessFlag.PUBLIC | AccessFlag.STATIC));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "initialize array";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin()
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // fleeceColorTable = new float[3][16];
                        push(3),
                        push(16),
                        reference(MULTIANEWARRAY, new ClassRef("[[F")), 2,
                        reference(PUTSTATIC, fleeceColorTable)
                    );
                }
            }.matchStaticInitializerOnly(true));
        }
    }

    private class RenderWolfMod extends ClassMod {
        private final FieldRef collarColors = new FieldRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "collarColors", "[[F");

        RenderWolfMod() {
            setParentClass("RenderLivingEntity");
            RenderUtilsMod.setup(this);

            addClassSignature(new ConstSignature(ResourceLocationMod.select("/mob/wolf_collar.png", "textures/entity/wolf/wolf_collar.png")));

            if (IBlockStateMod.haveClass()) {
                setup18();
            } else {
                setup17();
            }
        }

        private void setup17() {
            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override wolf collar colors";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(GETSTATIC, fleeceColorTable)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(GETSTATIC, collarColors)
                    );
                }
            });
        }

        private void setup18() {
            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override wolf collar colors";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // rgb = colorEnum.getRGB();
                        capture(anyALOAD),
                        anyReference(INVOKESTATIC),
                        capture(anyASTORE),

                        // GL11.glColor3f(rgb[0], rgb[1], rgb[2]);
                        lookAhead(build(
                            capture(anyALOAD),
                            push(0),
                            FALOAD,
                            backReference(3),
                            push(1),
                            FALOAD,
                            backReference(3),
                            push(2),
                            FALOAD,
                            RenderUtilsMod.glColor3f(this)
                        ), true)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        // rgb = ColorizeEntity.collarColors[colorEnum.ordinal()];
                        reference(GETSTATIC, collarColors),
                        getCaptureGroup(1),
                        reference(INVOKEVIRTUAL, new MethodRef("java/lang/Enum", "ordinal", "()I")),
                        AALOAD,
                        getCaptureGroup(2)
                    );
                }
            });
        }
    }

    private class RecipesDyedArmorMod extends ClassMod {
        RecipesDyedArmorMod() {
            final FieldRef armorColors = new FieldRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "armorColors", "[[F");

            addClassSignature(new ConstSignature(255.0f));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // var7 = (int)((float)var7 * var10 / var11);
                        ILOAD, capture(any()),
                        I2F,
                        FLOAD, capture(any()),
                        FMUL,
                        FLOAD, capture(any()),
                        FDIV,
                        F2I,
                        ISTORE, backReference(1),

                        // var8 = (int)((float)var8 * var10 / var11);
                        ILOAD, capture(any()),
                        I2F,
                        FLOAD, backReference(2),
                        FMUL,
                        FLOAD, backReference(3),
                        FDIV,
                        F2I,
                        ISTORE, backReference(4),

                        // var9 = (int)((float)var9 * var10 / var11);
                        ILOAD, capture(any()),
                        I2F,
                        FLOAD, backReference(2),
                        FMUL,
                        FLOAD, backReference(3),
                        FDIV,
                        F2I,
                        ISTORE, backReference(5)
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override armor dye colors";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        reference(GETSTATIC, fleeceColorTable)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        reference(GETSTATIC, armorColors)
                    );
                }
            });
        }
    }

    private class FontRendererMod extends com.prupe.mcpatcher.basemod.FontRendererMod {
        FontRendererMod() {
            super(CustomColors.this);
            RenderUtilsMod.setup(this);

            final String renderStringArgs = IBlockStateMod.haveClass() ? "FF" : "II";
            final MethodRef renderString = new MethodRef(getDeobfClass(), "renderString", "(Ljava/lang/String;" + renderStringArgs + "IZ)I");
            final FieldRef colorCode = new FieldRef(getDeobfClass(), "colorCode", "[I");
            final MethodRef colorizeText1 = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "colorizeText", "(I)I");
            final MethodRef colorizeText2 = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "colorizeText", "(II)I");

            addClassSignature(new BytecodeSignature() {
                {
                    matchConstructorOnly(true);
                    addXref(1, colorCode);
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(32),
                        NEWARRAY, T_INT,
                        captureReference(PUTFIELD)
                    );
                }
            });

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0xff000000),
                        any(0, 100),
                        RenderUtilsMod.glColor4f(this)
                    );
                }
            }.setMethod(renderString));

            addPatch(new BytecodePatch() {
                {
                    setInsertBefore(true);
                    targetMethod(renderString);
                }

                @Override
                public String getDescription() {
                    return "override text color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ILOAD, 4,
                        push(0xfc000000),
                        IAND
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ILOAD, 4,
                        reference(INVOKESTATIC, colorizeText1),
                        ISTORE, 4
                    );
                }
            });

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override text color codes";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        reference(GETFIELD, colorCode),
                        capture(anyILOAD),
                        IALOAD
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, colorizeText2)
                    );
                }
            }.setInsertAfter(true));
        }
    }

    private class TileEntitySignRendererMod extends ClassMod {
        TileEntitySignRendererMod() {
            RenderUtilsMod.setup(this);

            final FieldRef sign;
            final MethodRef colorizeSignText = new MethodRef(MCPatcherUtils.COLORIZE_WORLD_CLASS, "colorizeSignText", "()I");

            if (ResourceLocationMod.haveClass()) {
                sign = new FieldRef(getDeobfClass(), "sign", "LResourceLocation;");
                addClassSignature(new ResourceLocationSignature(this, sign, "textures/entity/sign.png"));
            } else {
                sign = null;
                addClassSignature(new ConstSignature("/item/sign.png"));
            }

            addPatch(new BytecodePatch() {
                {
                    addPreMatchSignature(new BytecodeSignature() {
                        @Override
                        public String getMatchExpression() {
                            return buildExpression(
                                sign == null ? push("/item/sign.png") : reference(GETSTATIC, sign)
                            );
                        }
                    });
                }

                @Override
                public String getDescription() {
                    return "override sign text color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0),
                        RenderUtilsMod.glDepthMask(this),
                        push(0),
                        capture(anyISTORE)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        push(0),
                        RenderUtilsMod.glDepthMask(this),
                        reference(INVOKESTATIC, colorizeSignText),
                        getCaptureGroup(1)
                    );
                }
            });
        }
    }

    private class RenderXPOrbMod extends ClassMod {
        RenderXPOrbMod() {
            final MethodRef colorizeXPOrb = new MethodRef(MCPatcherUtils.COLORIZE_ENTITY_CLASS, "colorizeXPOrb", "(IF)I");

            addClassSignature(new ConstSignature(ResourceLocationMod.select("/item/xporb.png", "textures/entity/experience_orb.png")));

            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "override xp orb color";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        lookBehind(build(
                            // MathHelper.sin(f8 + 0.0F)
                            capture(anyFLOAD),
                            push(0.0f),
                            FADD,
                            anyReference(INVOKESTATIC),

                            // ...
                            any(0, 200)
                        ), true),

                        // tessellator.setColorRGBA_I(i1, 128);
                        capture(anyILOAD),
                        lookAhead(build(
                            push(128),
                            anyReference(INVOKEVIRTUAL)
                        ), true)
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        getCaptureGroup(2),
                        getCaptureGroup(1),
                        reference(INVOKESTATIC, colorizeXPOrb)
                    );
                }
            });
        }
    }
}
