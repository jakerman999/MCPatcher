package com.prupe.mcpatcher;

import javassist.bytecode.AccessFlag;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;

import static com.prupe.mcpatcher.BinaryRegex.*;
import static com.prupe.mcpatcher.BytecodeMatcher.anyReference;
import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static javassist.bytecode.Opcode.*;

/**
 * Internal mod required by the patcher.  Responsible for injecting MCPatcherUtils classes
 * into minecraft.jar.
 * <p/>
 * Also provides a collection of commonly used ClassMods as public static inner classes that
 * can be instantiated or extended as needed.
 */
public final class BaseMod extends Mod {
    public static final String NAME = "__Base";

    BaseMod() {
        name = NAME;
        author = "MCPatcher";
        description = "Internal mod required by the patcher.";
        version = "1.1";
        configPanel = new ConfigPanel();
        dependencies.clear();

        addClassMod(new XMinecraftMod());

        addClassFile(MCPatcherUtils.UTILS_CLASS);
        addClassFile(MCPatcherUtils.LOGGER_CLASS);
        addClassFile(MCPatcherUtils.LOGGER_CLASS + "$1");
        addClassFile(MCPatcherUtils.LOGGER_CLASS + "$1$1");
        addClassFile(MCPatcherUtils.LOGGER_CLASS + "$ErrorLevel");
        addClassFile(MCPatcherUtils.CONFIG_CLASS);
        addClassFile(MCPatcherUtils.TILE_MAPPING_CLASS);
    }

    @Override
    public String[] getLoggingCategories() {
        return null;
    }

    class ConfigPanel extends ModConfigPanel {
        private JPanel panel;
        private JTextField heapSizeText;
        private JTextField directSizeText;
        private JCheckBox autoRefreshTexturesCheckBox;
        private JTable logTable;

        ConfigPanel() {
            autoRefreshTexturesCheckBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Config.set("autoRefreshTextures", autoRefreshTexturesCheckBox.isSelected());
                }
            });

            logTable.setModel(new TableModel() {
                private Vector<String> getCategories() {
                    Vector<String> allCategories = new Vector<String>();
                    for (Mod mod : MCPatcher.modList.getAll()) {
                        String[] categories = mod.getLoggingCategories();
                        if (categories != null) {
                            for (String category : categories) {
                                if (category != null) {
                                    allCategories.add(category);
                                }
                            }
                        }
                    }
                    return allCategories;
                }

                private String getCategory(int rowIndex) {
                    Vector<String> categories = getCategories();
                    return rowIndex >= 0 && rowIndex < categories.size() ? categories.elementAt(rowIndex) : null;
                }

                public int getRowCount() {
                    return getCategories().size();
                }

                public int getColumnCount() {
                    return 2;
                }

                public String getColumnName(int columnIndex) {
                    return null;
                }

                public Class<?> getColumnClass(int columnIndex) {
                    return String.class;
                }

                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return columnIndex == 1;
                }

                public Object getValueAt(int rowIndex, int columnIndex) {
                    String category = getCategory(rowIndex);
                    if (category == null) {
                        return null;
                    }
                    return columnIndex == 0 ? category : Config.getLogLevel(category);
                }

                public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                    String category = getCategory(rowIndex);
                    if (columnIndex != 1 || category == null) {
                        return;
                    }
                    try {
                        Config.setLogLevel(category, Level.parse(aValue.toString()));
                    } catch (IllegalArgumentException e) {
                    }
                }

                public void addTableModelListener(TableModelListener l) {
                }

                public void removeTableModelListener(TableModelListener l) {
                }
            });

            JComboBox combo = new JComboBox();
            combo.addItem(Level.OFF);
            combo.addItem(Level.SEVERE);
            combo.addItem(Level.WARNING);
            combo.addItem(Level.INFO);
            combo.addItem(Level.CONFIG);
            combo.addItem(Level.FINE);
            combo.addItem(Level.FINER);
            combo.addItem(Level.FINEST);
            combo.addItem(Level.ALL);

            logTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(combo));
        }

        @Override
        public JPanel getPanel() {
            return panel;
        }

        @Override
        public String getPanelName() {
            return "General options";
        }

        @Override
        public void load() {
            loadIntConfig(Config.TAG_JAVA_HEAP_SIZE, heapSizeText, 1024);
            loadIntConfig(Config.TAG_DIRECT_MEMORY_SIZE, directSizeText, 0);
            autoRefreshTexturesCheckBox.setSelected(Config.getBoolean("autoRefreshTextures", false));
        }

        @Override
        public void save() {
            saveIntConfig(Config.TAG_JAVA_HEAP_SIZE, heapSizeText);
            saveIntConfig(Config.TAG_DIRECT_MEMORY_SIZE, directSizeText);
        }

        private void loadIntConfig(String tag, JTextField field, int defaultValue) {
            int value = Config.getInt(tag, defaultValue);
            if (value > 0) {
                field.setText("" + value);
            } else {
                field.setText("");
            }
        }

        private void saveIntConfig(String tag, JTextField field) {
            String value = field.getText().trim();
            int num = 0;
            if (!value.isEmpty()) {
                try {
                    num = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                }
            }
            Config.set(tag, num);
        }
    }

    private class XMinecraftMod extends MinecraftMod {
        XMinecraftMod() {
            addPatch(new BytecodePatch() {
                @Override
                public String getDescription() {
                    return "MCPatcherUtils.setMinecraft(this)";
                }

                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        reference(INVOKESPECIAL, new MethodRef("java.lang.Object", "<init>", "()V"))
                    );
                }

                @Override
                public byte[] getReplacementBytes() {
                    return buildCode(
                        ALOAD_0,
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.UTILS_CLASS, "setMinecraft", "(LMinecraft;)V")),
                        push(MCPatcher.minecraft.getVersion().getVersionString()),
                        push(MCPatcher.VERSION_STRING),
                        reference(INVOKESTATIC, new MethodRef(MCPatcherUtils.UTILS_CLASS, "setVersions", "(Ljava/lang/String;Ljava/lang/String;)V"))
                    );
                }
            }
                .setInsertAfter(true)
                .matchConstructorOnly(true)
            );
        }

        @Override
        public String getDeobfClass() {
            return "Minecraft";
        }
    }

    /**
     * Matches Minecraft class and maps the texturePackList field.
     */
    public static class MinecraftMod extends ClassMod {
        public MinecraftMod() {
            addClassSignature(new FilenameSignature("net/minecraft/client/Minecraft.class"));
        }

        public MinecraftMod mapTexturePackList() {
            addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "texturePackList", "LTexturePackList;")));
            return this;
        }

        public MinecraftMod mapWorldClient() {
            addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "theWorld", "LWorldClient;")));
            return this;
        }

        public MinecraftMod mapPlayer() {
            addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "thePlayer", "LEntityClientPlayerMP;")));
            return this;
        }
    }

    /**
     * Matches GLAllocation class and maps createDirectByteBuffer method.
     */
    public static class GLAllocationMod extends ClassMod {
        public GLAllocationMod() {
            addClassSignature(new ConstSignature(new MethodRef(MCPatcherUtils.GL11_CLASS, "glDeleteLists", "(II)V")));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    if (getMethodInfo().getDescriptor().equals("(I)Ljava/nio/ByteBuffer;")) {
                        return buildExpression(
                            reference(INVOKESTATIC, new MethodRef("java.nio.ByteBuffer", "allocateDirect", "(I)Ljava/nio/ByteBuffer;"))
                        );
                    } else {
                        return null;
                    }
                }
            }.setMethodName("createDirectByteBuffer"));
        }
    }

    /**
     * Matches Tessellator class and instance and maps several commonly used rendering methods.
     */
    public static class TessellatorMod extends ClassMod {
        protected final MethodRef draw = new MethodRef(getDeobfClass(), "draw", "()I");
        protected final MethodRef startDrawingQuads = new MethodRef(getDeobfClass(), "startDrawingQuads", "()V");
        protected final MethodRef startDrawing = new MethodRef(getDeobfClass(), "startDrawing", "(I)V");
        protected final MethodRef addVertexWithUV = new MethodRef(getDeobfClass(), "addVertexWithUV", "(DDDDD)V");
        protected final MethodRef addVertex = new MethodRef(getDeobfClass(), "addVertex", "(DDD)V");
        protected final MethodRef setTextureUV = new MethodRef(getDeobfClass(), "setTextureUV", "(DD)V");
        protected final FieldRef instance = new FieldRef(getDeobfClass(), "instance", "LTessellator;");

        public TessellatorMod() {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("Not tesselating!")
                    );
                }
            }.setMethod(draw));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(7),
                        captureReference(INVOKEVIRTUAL),
                        RETURN
                    );
                }
            }
                .setMethod(startDrawingQuads)
                .addXref(1, startDrawing)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        DLOAD, 7,
                        DLOAD, 9,
                        captureReference(INVOKEVIRTUAL),

                        ALOAD_0,
                        DLOAD_1,
                        DLOAD_3,
                        DLOAD, 5,
                        captureReference(INVOKEVIRTUAL),

                        RETURN
                    );
                }
            }
                .setMethod(addVertexWithUV)
                .addXref(1, setTextureUV)
                .addXref(2, addVertex)
            );

            addMemberMapper(new FieldMapper(instance).accessFlag(AccessFlag.STATIC, true));
        }
    }

    /**
     * Matches IBlockAccess interface and maps getBlockId, getBlockMetadata methods.
     */
    public static class IBlockAccessMod extends ClassMod {
        public IBlockAccessMod() {
            addClassSignature(new InterfaceSignature(
                new InterfaceMethodRef(getDeobfClass(), "getBlockId", "(III)I"),
                new InterfaceMethodRef(getDeobfClass(), "getBlockTileEntity", "(III)LTileEntity;"),
                new InterfaceMethodRef(getDeobfClass(), "getLightBrightnessForSkyBlocks", "(IIII)I"),
                new InterfaceMethodRef(getDeobfClass(), "getBrightness", "(IIII)F"),
                new InterfaceMethodRef(getDeobfClass(), "getLightBrightness", "(III)F"),
                new InterfaceMethodRef(getDeobfClass(), "getBlockMetadata", "(III)I"),
                new InterfaceMethodRef(getDeobfClass(), "getBlockMaterial", "(III)LMaterial;"),
                new InterfaceMethodRef(getDeobfClass(), "isBlockOpaqueCube", "(III)Z"),
                new InterfaceMethodRef(getDeobfClass(), "isBlockNormalCube", "(III)Z"),
                new InterfaceMethodRef(getDeobfClass(), "isAirBlock", "(III)Z"),
                new InterfaceMethodRef(getDeobfClass(), "getBiomeGenAt", "(II)LBiomeGenBase;"),
                new InterfaceMethodRef(getDeobfClass(), "getHeight", "()I"),
                new InterfaceMethodRef(getDeobfClass(), "extendedLevelsInChunkCache", "()Z"),
                new InterfaceMethodRef(getDeobfClass(), "doesBlockHaveSolidTopSurface", "(III)Z"),
                new InterfaceMethodRef(getDeobfClass(), "getWorldVec3Pool", "()LVec3Pool;"),
                new InterfaceMethodRef(getDeobfClass(), "isBlockProvidingPowerTo", "(IIII)I")
            ).setInterfaceOnly(true));
        }
    }

    /**
     * Matches Block class and maps blockID and blockList fields.
     */
    public static class BlockMod extends ClassMod {
        private static final ArrayList<BlockSubclassEntry> subclasses = new ArrayList<BlockSubclassEntry>() {
            {
                // autogenerated by blockids.pl -- do not edit
                // (block id, field class, field name, field subclass, block name)
                add(new BlockSubclassEntry(1, "Block", "stone", "BlockStone", "stone"));
                add(new BlockSubclassEntry(2, "BlockGrass", "grass", "BlockGrass", "grass"));
                add(new BlockSubclassEntry(3, "Block", "dirt", "BlockDirt", "dirt"));
                add(new BlockSubclassEntry(4, "Block", "cobblestone", "Block", "stonebrick"));
                add(new BlockSubclassEntry(5, "Block", "planks", "BlockWood", "wood"));
                add(new BlockSubclassEntry(6, "Block", "sapling", "BlockSapling", "sapling"));
                add(new BlockSubclassEntry(7, "Block", "bedrock", "Block", "bedrock"));
                add(new BlockSubclassEntry(8, "BlockFluid", "waterMoving", "BlockFlowing", "water"));
                add(new BlockSubclassEntry(9, "Block", "waterStill", "BlockStationary", "water"));
                add(new BlockSubclassEntry(10, "BlockFluid", "lavaMoving", "BlockFlowing", "lava"));
                add(new BlockSubclassEntry(11, "Block", "lavaStill", "BlockStationary", "lava"));
                add(new BlockSubclassEntry(12, "Block", "sand", "BlockSand", "sand"));
                add(new BlockSubclassEntry(13, "Block", "gravel", "BlockGravel", "gravel"));
                add(new BlockSubclassEntry(14, "Block", "oreGold", "BlockOre", "oreGold"));
                add(new BlockSubclassEntry(15, "Block", "oreIron", "BlockOre", "oreIron"));
                add(new BlockSubclassEntry(16, "Block", "oreCoal", "BlockOre", "oreCoal"));
                add(new BlockSubclassEntry(17, "Block", "wood", "BlockLog", "log"));
                add(new BlockSubclassEntry(18, "BlockLeaves", "leaves", "BlockLeaves", "leaves"));
                add(new BlockSubclassEntry(19, "Block", "sponge", "BlockSponge", "sponge"));
                add(new BlockSubclassEntry(20, "Block", "glass", "BlockGlass", "glass"));
                add(new BlockSubclassEntry(21, "Block", "oreLapis", "BlockOre", "oreLapis"));
                add(new BlockSubclassEntry(22, "Block", "blockLapis", "Block", "blockLapis"));
                add(new BlockSubclassEntry(23, "Block", "dispenser", "BlockDispenser", "dispenser"));
                add(new BlockSubclassEntry(24, "Block", "sandStone", "BlockSandStone", "sandStone"));
                add(new BlockSubclassEntry(25, "Block", "music", "BlockNote", "musicBlock"));
                add(new BlockSubclassEntry(26, "Block", "bed", "BlockBed", "bed"));
                add(new BlockSubclassEntry(27, "Block", "railPowered", "BlockRailPowered", "goldenRail"));
                add(new BlockSubclassEntry(28, "Block", "railDetector", "BlockDetectorRail", "detectorRail"));
                add(new BlockSubclassEntry(29, "BlockPistonBase", "pistonStickyBase", "BlockPistonBase", "pistonStickyBase"));
                add(new BlockSubclassEntry(30, "Block", "web", "BlockWeb", "web"));
                add(new BlockSubclassEntry(31, "BlockTallGrass", "tallGrass", "BlockTallGrass", "tallgrass"));
                add(new BlockSubclassEntry(32, "BlockDeadBush", "deadBush", "BlockDeadBush", "deadbush"));
                add(new BlockSubclassEntry(33, "BlockPistonBase", "pistonBase", "BlockPistonBase", "pistonBase"));
                add(new BlockSubclassEntry(34, "BlockPistonExtension", "pistonExtension", "BlockPistonExtension", "unnamedBlock34"));
                add(new BlockSubclassEntry(35, "Block", "cloth", "BlockCloth", "cloth"));
                add(new BlockSubclassEntry(36, "BlockPistonMoving", "pistonMoving", "BlockPistonMoving", "unnamedBlock36"));
                add(new BlockSubclassEntry(37, "BlockFlower", "plantYellow", "BlockFlower", "flower"));
                add(new BlockSubclassEntry(38, "BlockFlower", "plantRed", "BlockFlower", "rose"));
                add(new BlockSubclassEntry(39, "BlockFlower", "mushroomBrown", "BlockMushroom", "mushroom"));
                add(new BlockSubclassEntry(40, "BlockFlower", "mushroomRed", "BlockMushroom", "mushroom"));
                add(new BlockSubclassEntry(41, "Block", "blockGold", "BlockOreStorage", "blockGold"));
                add(new BlockSubclassEntry(42, "Block", "blockSteel", "BlockOreStorage", "blockIron"));
                add(new BlockSubclassEntry(43, "BlockHalfSlab", "stoneDoubleSlab", "BlockStep", "stoneSlab"));
                add(new BlockSubclassEntry(44, "BlockHalfSlab", "stoneSingleSlab", "BlockStep", "stoneSlab"));
                add(new BlockSubclassEntry(45, "Block", "brick", "Block", "brick"));
                add(new BlockSubclassEntry(46, "Block", "tnt", "BlockTNT", "tnt"));
                add(new BlockSubclassEntry(47, "Block", "bookShelf", "BlockBookshelf", "bookshelf"));
                add(new BlockSubclassEntry(48, "Block", "cobblestoneMossy", "Block", "stoneMoss"));
                add(new BlockSubclassEntry(49, "Block", "obsidian", "BlockObsidian", "obsidian"));
                add(new BlockSubclassEntry(50, "Block", "torchWood", "BlockTorch", "torch"));
                add(new BlockSubclassEntry(51, "BlockFire", "fire", "BlockFire", "fire"));
                add(new BlockSubclassEntry(52, "Block", "mobSpawner", "BlockMobSpawner", "mobSpawner"));
                add(new BlockSubclassEntry(53, "Block", "stairCompactPlanks", "BlockStairs", "stairsWood"));
                add(new BlockSubclassEntry(54, "BlockChest", "chest", "BlockChest", "chest"));
                add(new BlockSubclassEntry(55, "BlockRedstoneWire", "redstoneWire", "BlockRedstoneWire", "redstoneDust"));
                add(new BlockSubclassEntry(56, "Block", "oreDiamond", "BlockOre", "oreDiamond"));
                add(new BlockSubclassEntry(57, "Block", "blockDiamond", "BlockOreStorage", "blockDiamond"));
                add(new BlockSubclassEntry(58, "Block", "workbench", "BlockWorkbench", "workbench"));
                add(new BlockSubclassEntry(59, "Block", "crops", "BlockCrops", "crops"));
                add(new BlockSubclassEntry(60, "Block", "tilledField", "BlockFarmland", "farmland"));
                add(new BlockSubclassEntry(61, "Block", "stoneOvenIdle", "BlockFurnace", "furnace"));
                add(new BlockSubclassEntry(62, "Block", "stoneOvenActive", "BlockFurnace", "furnace"));
                add(new BlockSubclassEntry(63, "Block", "signPost", "BlockSign", "sign"));
                add(new BlockSubclassEntry(64, "Block", "doorWood", "BlockDoor", "doorWood"));
                add(new BlockSubclassEntry(65, "Block", "ladder", "BlockLadder", "ladder"));
                add(new BlockSubclassEntry(66, "Block", "rail", "BlockRail", "rail"));
                add(new BlockSubclassEntry(67, "Block", "stairCompactCobblestone", "BlockStairs", "stairsStone"));
                add(new BlockSubclassEntry(68, "Block", "signWall", "BlockSign", "sign"));
                add(new BlockSubclassEntry(69, "Block", "lever", "BlockLever", "lever"));
                add(new BlockSubclassEntry(70, "Block", "pressurePlateStone", "BlockPressurePlate", "pressurePlate"));
                add(new BlockSubclassEntry(71, "Block", "doorSteel", "BlockDoor", "doorIron"));
                add(new BlockSubclassEntry(72, "Block", "pressurePlatePlanks", "BlockPressurePlate", "pressurePlate"));
                add(new BlockSubclassEntry(73, "Block", "oreRedstone", "BlockRedstoneOre", "oreRedstone"));
                add(new BlockSubclassEntry(74, "Block", "oreRedstoneGlowing", "BlockRedstoneOre", "oreRedstone"));
                add(new BlockSubclassEntry(75, "Block", "torchRedstoneIdle", "BlockRedstoneTorch", "notGate"));
                add(new BlockSubclassEntry(76, "Block", "torchRedstoneActive", "BlockRedstoneTorch", "notGate"));
                add(new BlockSubclassEntry(77, "Block", "stoneButton", "BlockButtonStone", "button"));
                add(new BlockSubclassEntry(78, "Block", "snow", "BlockSnow", "snow"));
                add(new BlockSubclassEntry(79, "Block", "ice", "BlockIce", "ice"));
                add(new BlockSubclassEntry(80, "Block", "blockSnow", "BlockSnowBlock", "snow"));
                add(new BlockSubclassEntry(81, "Block", "cactus", "BlockCactus", "cactus"));
                add(new BlockSubclassEntry(82, "Block", "blockClay", "BlockClay", "clay"));
                add(new BlockSubclassEntry(83, "Block", "reed", "BlockReed", "reeds"));
                add(new BlockSubclassEntry(84, "Block", "jukebox", "BlockJukeBox", "jukebox"));
                add(new BlockSubclassEntry(85, "Block", "fence", "BlockFence", "fence"));
                add(new BlockSubclassEntry(86, "Block", "pumpkin", "BlockPumpkin", "pumpkin"));
                add(new BlockSubclassEntry(87, "Block", "netherrack", "BlockNetherrack", "hellrock"));
                add(new BlockSubclassEntry(88, "Block", "slowSand", "BlockSoulSand", "hellsand"));
                add(new BlockSubclassEntry(89, "Block", "glowStone", "BlockGlowStone", "lightgem"));
                add(new BlockSubclassEntry(90, "BlockPortal", "portal", "BlockPortal", "portal"));
                add(new BlockSubclassEntry(91, "Block", "pumpkinLantern", "BlockPumpkin", "litpumpkin"));
                add(new BlockSubclassEntry(92, "Block", "cake", "BlockCake", "cake"));
                add(new BlockSubclassEntry(93, "BlockRedstoneRepeater", "redstoneRepeaterIdle", "BlockRedstoneRepeater", "diode"));
                add(new BlockSubclassEntry(94, "BlockRedstoneRepeater", "redstoneRepeaterActive", "BlockRedstoneRepeater", "diode"));
                add(new BlockSubclassEntry(95, "Block", "lockedChest", "BlockLockedChest", "lockedchest"));
                add(new BlockSubclassEntry(96, "Block", "trapdoor", "BlockTrapDoor", "trapdoor"));
                add(new BlockSubclassEntry(97, "Block", "silverfish", "BlockSilverfish", "monsterStoneEgg"));
                add(new BlockSubclassEntry(98, "Block", "stoneBrick", "BlockStoneBrick", "stonebricksmooth"));
                add(new BlockSubclassEntry(99, "Block", "mushroomCapBrown", "BlockMushroomCap", "mushroom"));
                add(new BlockSubclassEntry(100, "Block", "mushroomCapRed", "BlockMushroomCap", "mushroom"));
                add(new BlockSubclassEntry(101, "Block", "fenceIron", "BlockPane", "fenceIron"));
                add(new BlockSubclassEntry(102, "Block", "thinGlass", "BlockPane", "thinGlass"));
                add(new BlockSubclassEntry(103, "Block", "melon", "BlockMelon", "melon"));
                add(new BlockSubclassEntry(104, "Block", "pumpkinStem", "BlockStem", "pumpkinStem"));
                add(new BlockSubclassEntry(105, "Block", "melonStem", "BlockStem", "pumpkinStem"));
                add(new BlockSubclassEntry(106, "Block", "vine", "BlockVine", "vine"));
                add(new BlockSubclassEntry(107, "Block", "fenceGate", "BlockFenceGate", "fenceGate"));
                add(new BlockSubclassEntry(108, "Block", "stairsBrick", "BlockStairs", "stairsBrick"));
                add(new BlockSubclassEntry(109, "Block", "stairsStoneBrickSmooth", "BlockStairs", "stairsStoneBrickSmooth"));
                add(new BlockSubclassEntry(110, "BlockMycelium", "mycelium", "BlockMycelium", "mycel"));
                add(new BlockSubclassEntry(111, "Block", "waterlily", "BlockLilyPad", "waterlily"));
                add(new BlockSubclassEntry(112, "Block", "netherBrick", "Block", "netherBrick"));
                add(new BlockSubclassEntry(113, "Block", "netherFence", "BlockFence", "netherFence"));
                add(new BlockSubclassEntry(114, "Block", "stairsNetherBrick", "BlockStairs", "stairsNetherBrick"));
                add(new BlockSubclassEntry(115, "Block", "netherStalk", "BlockNetherStalk", "netherStalk"));
                add(new BlockSubclassEntry(116, "Block", "enchantmentTable", "BlockEnchantmentTable", "enchantmentTable"));
                add(new BlockSubclassEntry(117, "Block", "brewingStand", "BlockBrewingStand", "brewingStand"));
                add(new BlockSubclassEntry(118, "BlockCauldron", "cauldron", "BlockCauldron", "cauldron"));
                add(new BlockSubclassEntry(119, "Block", "endPortal", "BlockEndPortal", "unnamedBlock119"));
                add(new BlockSubclassEntry(120, "Block", "endPortalFrame", "BlockEndPortalFrame", "endPortalFrame"));
                add(new BlockSubclassEntry(121, "Block", "whiteStone", "Block", "whiteStone"));
                add(new BlockSubclassEntry(122, "Block", "dragonEgg", "BlockDragonEgg", "dragonEgg"));
                add(new BlockSubclassEntry(123, "Block", "redstoneLampIdle", "BlockRedstoneLight", "redstoneLight"));
                add(new BlockSubclassEntry(124, "Block", "redstoneLampActive", "BlockRedstoneLight", "redstoneLight"));
                add(new BlockSubclassEntry(125, "BlockHalfSlab", "woodDoubleSlab", "BlockWoodSlab", "woodSlab"));
                add(new BlockSubclassEntry(126, "BlockHalfSlab", "woodSingleSlab", "BlockWoodSlab", "woodSlab"));
                add(new BlockSubclassEntry(127, "Block", "cocoaPlant", "BlockCocoa", "cocoa"));
                add(new BlockSubclassEntry(128, "Block", "stairsSandStone", "BlockStairs", "stairsSandStone"));
                add(new BlockSubclassEntry(129, "Block", "oreEmerald", "BlockOre", "oreEmerald"));
                add(new BlockSubclassEntry(130, "Block", "enderChest", "BlockEnderChest", "enderChest"));
                add(new BlockSubclassEntry(131, "BlockTripWireSource", "tripWireSource", "BlockTripWireSource", "tripWireSource"));
                add(new BlockSubclassEntry(132, "Block", "tripWire", "BlockTripWire", "tripWire"));
                add(new BlockSubclassEntry(133, "Block", "blockEmerald", "BlockOreStorage", "blockEmerald"));
                add(new BlockSubclassEntry(134, "Block", "stairsWoodSpruce", "BlockStairs", "stairsWoodSpruce"));
                add(new BlockSubclassEntry(135, "Block", "stairsWoodBirch", "BlockStairs", "stairsWoodBirch"));
                add(new BlockSubclassEntry(136, "Block", "stairsWoodJungle", "BlockStairs", "stairsWoodJungle"));
                add(new BlockSubclassEntry(137, "Block", "commandBlock", "BlockCommandBlock", "commandBlock"));
                add(new BlockSubclassEntry(138, "BlockBeacon", "beacon", "BlockBeacon", "beacon"));
                add(new BlockSubclassEntry(139, "Block", "cobblestoneWall", "BlockWall", "cobbleWall"));
                add(new BlockSubclassEntry(140, "Block", "flowerPot", "BlockFlowerPot", "flowerPot"));
                add(new BlockSubclassEntry(141, "Block", "carrot", "BlockCarrot", "carrots"));
                add(new BlockSubclassEntry(142, "Block", "potato", "BlockPotato", "potatoes"));
                add(new BlockSubclassEntry(143, "Block", "woodenButton", "BlockButtonWood", "button"));
                add(new BlockSubclassEntry(144, "Block", "skull", "BlockSkull", "skull"));
                add(new BlockSubclassEntry(145, "Block", "anvil", "BlockAnvil", "anvil"));
                add(new BlockSubclassEntry(146, "Block", "chestTrap", "BlockChest", "chestTrap"));
                add(new BlockSubclassEntry(147, "Block", "weightedPlate_light", "BlockPressurePlateWeighted", "weightedPlate_light"));
                add(new BlockSubclassEntry(148, "Block", "weightedPlate_heavy", "BlockPressurePlateWeighted", "weightedPlate_heavy"));
                add(new BlockSubclassEntry(149, "BlockComparator", "comparator1", "BlockComparator", "comparator"));
                add(new BlockSubclassEntry(150, "BlockComparator", "comparator2", "BlockComparator", "comparator"));
                add(new BlockSubclassEntry(151, "BlockDaylightDetector", "daylightDetector", "BlockDaylightDetector", "daylightDetector"));
                add(new BlockSubclassEntry(152, "Block", "blockRedstone", "BlockPoweredOre", "blockRedstone"));
                add(new BlockSubclassEntry(153, "Block", "netherquartz", "BlockOre", "netherquartz"));
                add(new BlockSubclassEntry(154, "BlockHopper", "hopper", "BlockHopper", "hopper"));
                add(new BlockSubclassEntry(155, "Block", "quartzBlock", "BlockQuartz", "quartzBlock"));
                add(new BlockSubclassEntry(156, "Block", "stairsQuartz", "BlockStairs", "stairsQuartz"));
                add(new BlockSubclassEntry(157, "Block", "activatorRail", "BlockRailPowered", "activatorRail"));
            }
        };

        public BlockMod() {
            addClassSignature(new ConstSignature(" is already occupied by "));

            addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "blockID", "I"))
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, false)
                .accessFlag(AccessFlag.FINAL, true)
            );

            addMemberMapper(new FieldMapper(new FieldRef(getDeobfClass(), "blocksList", "[LBlock;"))
                .accessFlag(AccessFlag.PUBLIC, true)
                .accessFlag(AccessFlag.STATIC, true)
                .accessFlag(AccessFlag.FINAL, true)
            );
        }

        protected void addBlockSignatures() {
            for (BlockSubclassEntry entry : subclasses) {
                addBlockSignature(entry.blockID, entry.fieldClass, entry.fieldName, entry.className, entry.blockName);
            }
        }

        protected void addBlockSignature(String name) {
            for (BlockSubclassEntry entry : subclasses) {
                if (entry.className.equals(name) || entry.blockName.equals(name) || entry.fieldName.equals(name)) {
                    addBlockSignature(entry.blockID, entry.fieldClass, entry.fieldName, entry.className, entry.blockName);
                    return;
                }
            }
            throw new RuntimeException("unknown Block subclass: " + name);
        }

        protected void addBlockSignature(int blockID) {
            for (BlockSubclassEntry entry : subclasses) {
                if (entry.blockID == blockID) {
                    addBlockSignature(entry.blockID, entry.fieldClass, entry.fieldName, entry.className, entry.blockName);
                    return;
                }
            }
            throw new RuntimeException("unknown Block subclass: block ID" + blockID);
        }

        protected void addBlockSignature(final int blockID, final String fieldClass, final String fieldName, final String className, final String blockName) {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        captureReference(NEW),
                        DUP,
                        blockID == 35 ? "" : push(blockID),
                        nonGreedy(any(0, 60)),
                        blockName.startsWith("unnamedBlock") ? "" : build(
                            push(blockName),
                            anyReference(INVOKEVIRTUAL)
                        ),
                        nonGreedy(any(0, 20)),
                        captureReference(PUTSTATIC)
                    );
                }
            }
                .matchStaticInitializerOnly(true)
                .addXref(1, new ClassRef(className))
                .addXref(2, new FieldRef(getDeobfClass(), fieldName, "L" + fieldClass + ";"))
            );
        }

        private static class BlockSubclassEntry {
            final int blockID;
            final String fieldClass;
            final String fieldName;
            final String className;
            final String blockName;

            BlockSubclassEntry(int blockID, String fieldClass, String fieldName, String className, String blockName) {
                this.blockID = blockID;
                this.fieldClass = fieldClass;
                this.fieldName = fieldName;
                this.className = className;
                this.blockName = blockName;
            }
        }
    }

    /**
     * Matches Item class.
     */
    public static class ItemMod extends ClassMod {
        public ItemMod() {
            addClassSignature(new ConstSignature("CONFLICT @ "));
            addClassSignature(new ConstSignature("coal"));
        }
    }

    /**
     * Matches World class.
     */
    public static class WorldMod extends ClassMod {
        public WorldMod() {
            interfaces = new String[]{"IBlockAccess"};

            addClassSignature(new ConstSignature("ambient.cave.cave"));
            addClassSignature(new ConstSignature(0x3c6ef35f));
        }
    }

    /**
     * Matches WorldServer class and maps world field.
     */
    public static class WorldServerMod extends ClassMod {
        public WorldServerMod() {
            addClassSignature(new ConstSignature("Saving level"));
            addClassSignature(new ConstSignature("Saving chunks"));
        }
    }

    public static class WorldClientMod extends ClassMod {
        public WorldClientMod() {
            parentClass = "World";

            addClassSignature(new ConstSignature("MpServer"));
        }
    }

    /*
     * Matches FontRenderer class and maps charWidth, fontTextureName, and spaceWidth fields.
     */
    public static class FontRendererMod extends ClassMod {
        public FontRendererMod() {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        begin(),
                        ALOAD_0,
                        anyReference(INVOKESPECIAL),
                        ALOAD_0,
                        push(256),
                        NEWARRAY, T_INT,
                        captureReference(PUTFIELD)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, new FieldRef(getDeobfClass(), "charWidth", "[I"))
            );

            addClassSignature(new OrSignature(
                new ConstSignature("0123456789abcdef"),
                new ConstSignature("0123456789abcdefk"),
                new ConstSignature("/font/glyph_sizes.bin")
            ));
        }
    }

    /**
     * Matches RenderBlocks class.
     */
    public static class RenderBlocksMod extends ClassMod {
        protected final MethodRef renderStandardBlockWithAmbientOcclusion = new MethodRef(getDeobfClass(), "renderStandardBlockWithAmbientOcclusion", "(LBlock;IIIFFF)Z");
        protected final FieldRef renderAllFaces = new FieldRef(getDeobfClass(), "renderAllFaces", "Z");
        protected final FieldRef blockAccess = new FieldRef(getDeobfClass(), "blockAccess", "LIBlockAccess;");
        protected final MethodRef shouldSideBeRendered = new MethodRef("Block", "shouldSideBeRendered", "(LIBlockAccess;IIII)Z");

        public RenderBlocksMod() {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push(0x0f000f)
                    );
                }
            }.setMethod(renderStandardBlockWithAmbientOcclusion));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        IFNE, any(2),
                        ALOAD_1,
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ILOAD_2,
                        ILOAD_3,
                        push(1),
                        ISUB,
                        ILOAD, 4,
                        push(0),
                        captureReference(INVOKEVIRTUAL),
                        IFEQ, any(2)
                    );
                }
            }
                .setMethod(renderStandardBlockWithAmbientOcclusion)
                .addXref(1, renderAllFaces)
                .addXref(2, blockAccess)
                .addXref(3, shouldSideBeRendered)
            );

            addClassSignature(new ConstSignature(0.1875));
            addClassSignature(new ConstSignature(0.01));
        }
    }

    /**
     * Maps RenderEngine class.
     */
    public static class RenderEngineMod extends ClassMod {
        protected final FieldRef terrain = new FieldRef(getDeobfClass(), "terrain", "LTextureMap;");
        protected final FieldRef items = new FieldRef(getDeobfClass(), "items", "LTextureMap;");
        protected final MethodRef updateDynamicTextures = new MethodRef(getDeobfClass(), "updateDynamicTextures", "()V");
        protected final MethodRef refreshTextureMaps = new MethodRef(getDeobfClass(), "refreshTextureMaps", "()V");
        protected final MethodRef glTexSubImage2DByte = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexSubImage2D", "(IIIIIIIILjava/nio/ByteBuffer;)V");
        protected final MethodRef glTexSubImage2DInt = new MethodRef(MCPatcherUtils.GL11_CLASS, "glTexSubImage2D", "(IIIIIIIILjava/nio/IntBuffer;)V");
        protected final MethodRef refreshTextures = new MethodRef(getDeobfClass(), "refreshTextures", "()V");
        protected final FieldRef imageData = new FieldRef(getDeobfClass(), "imageData", "Ljava/nio/IntBuffer;");

        private String updateAnimationsMapped;

        public RenderEngineMod() {
            addClassSignature(new ConstSignature("%clamp%"));
            addClassSignature(new ConstSignature("%blur%"));
            addClassSignature(new OrSignature(
                new ConstSignature(glTexSubImage2DByte),
                new ConstSignature(glTexSubImage2DInt)
            ));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        push("%blur%")
                    );
                }
            }.setMethod(refreshTextures));

            // updateAnimations and refreshTextureMaps are identical up to obfuscation:
            // public void xxx() {
            //   this.terrain.yyy();
            //   this.items.yyy();
            // }
            // They're even called from similar methods, runTick() and startGame() in Minecraft.java.
            // Normal descriptor and bytecode matching is insufficient here, so we rely on the fact
            // that updateAnimations is defined first.
            addClassSignature(new VoidSignature(updateDynamicTextures, "updateAnimations") {
                @Override
                public boolean afterMatch() {
                    updateAnimationsMapped = getMethodInfo().getName();
                    return true;
                }
            });

            addClassSignature(new VoidSignature(refreshTextureMaps, "refresh") {
                @Override
                public boolean filterMethod() {
                    return updateAnimationsMapped != null && getMethodInfo().getName().compareTo(updateAnimationsMapped) > 0;
                }
            });

            addMemberMapper(new FieldMapper(imageData));
        }

        private class VoidSignature extends BytecodeSignature {
            VoidSignature(MethodRef method, String textureMethod) {
                setMethod(method);
                addXref(1, terrain);
                addXref(2, new MethodRef("TextureMap", textureMethod, "()V"));
                addXref(3, items);
            }

            @Override
            public String getMatchExpression() {
                return buildExpression(
                    begin(),
                    ALOAD_0,
                    captureReference(GETFIELD),
                    captureReference(INVOKEVIRTUAL),
                    ALOAD_0,
                    captureReference(GETFIELD),
                    backReference(2),
                    RETURN,
                    end()
                );
            }
        }
    }

    /**
     * Maps GameSettings class.
     */
    public static class GameSettingsMod extends ClassMod {
        public GameSettingsMod() {
            addClassSignature(new ConstSignature("options.txt"));
            addClassSignature(new OrSignature(
                new ConstSignature("key.forward"),
                new ConstSignature("Forward")
            ));
        }

        /**
         * Map any GameSettings field stored in options.txt.
         *
         * @param option     name in options.txt
         * @param field      name of field in GameSettings class
         * @param descriptor type descriptor
         */
        protected void mapOption(final String option, final String field, final String descriptor) {
            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // if (as[0].equals(option)) {
                        ALOAD_3,
                        ICONST_0,
                        AALOAD,
                        push(option),
                        reference(INVOKEVIRTUAL, new MethodRef("java/lang/String", "equals", "(Ljava/lang/Object;)Z")),
                        IFEQ, any(2),

                        // field = ...;
                        nonGreedy(any(0, 20)),
                        captureReference(PUTFIELD)
                    );
                }
            }.addXref(1, new FieldRef(getDeobfClass(), field, descriptor)));
        }
    }

    /**
     * Maps Profiler class and start/endSection methods.
     */
    public static class ProfilerMod extends ClassMod {
        public ProfilerMod() {
            addClassSignature(new ConstSignature("[UNKNOWN]"));
            addClassSignature(new ConstSignature(100.0));

            final MethodRef startSection = new MethodRef(getDeobfClass(), "startSection", "(Ljava/lang/String;)V");
            final MethodRef endSection = new MethodRef(getDeobfClass(), "endSection", "()V");
            final MethodRef endStartSection = new MethodRef(getDeobfClass(), "endStartSection", "(Ljava/lang/String;)V");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(INVOKEVIRTUAL),
                        ALOAD_0,
                        ALOAD_1,
                        captureReference(INVOKEVIRTUAL)
                    );
                }
            }
                .setMethod(endStartSection)
                .addXref(1, endSection)
                .addXref(2, startSection)
            );
        }
    }

    /**
     * Maps Texture class and various fields and methods.
     */
    public static class TextureMod extends ClassMod {
        protected final FieldRef glTextureTarget = new FieldRef(getDeobfClass(), "glTextureTarget", "I");
        protected final FieldRef glTexture = new FieldRef(getDeobfClass(), "glTexture", "I");
        protected final FieldRef loaded = new FieldRef(getDeobfClass(), "loaded", "Z");
        protected final MethodRef getIndex = new MethodRef(getDeobfClass(), "getIndex", "()I");
        protected final MethodRef getGLTexture = new MethodRef(getDeobfClass(), "getGLTexture", "()I");
        protected final MethodRef getWidth = new MethodRef(getDeobfClass(), "getWidth", "()I");
        protected final MethodRef getHeight = new MethodRef(getDeobfClass(), "getHeight", "()I");
        protected final MethodRef getByteBuffer = new MethodRef(getDeobfClass(), "getByteBuffer", "()Ljava/nio/ByteBuffer;");
        protected final MethodRef getName = new MethodRef(getDeobfClass(), "getName", "()Ljava/lang/String;");
        protected final MethodRef loadGLTexture = new MethodRef(getDeobfClass(), "loadGLTexture", "()V");
        protected final MethodRef transferFromImage = new MethodRef(getDeobfClass(), "transferFromImage", "(Ljava/awt/image/BufferedImage;)V");
        protected final MethodRef glBindTexture = new MethodRef(MCPatcherUtils.GL11_CLASS, "glBindTexture", "(II)V");

        public TextureMod() {
            addClassSignature(new ConstSignature("png"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_0,
                        captureReference(GETFIELD),
                        reference(INVOKESTATIC, glBindTexture)
                    );
                }
            }
                .matchConstructorOnly(true)
                .addXref(1, glTextureTarget)
                .addXref(2, glTexture)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        push(1),
                        captureReference(PUTFIELD),
                        RETURN,
                        end()
                    );
                }
            }
                .setMethod(loadGLTexture)
                .addXref(1, loaded)
            );

            addMemberMapper(new MethodMapper(getIndex, getGLTexture, getWidth, getHeight));
            addMemberMapper(new MethodMapper(getByteBuffer));
            addMemberMapper(new MethodMapper(getName));
            addMemberMapper(new MethodMapper(transferFromImage));
        }
    }

    public static class IconMod extends ClassMod {
        public IconMod() {
            final InterfaceMethodRef getX0 = new InterfaceMethodRef(getDeobfClass(), "getX0", "()I");
            final InterfaceMethodRef getY0 = new InterfaceMethodRef(getDeobfClass(), "getY0", "()I");
            final InterfaceMethodRef getNormalizedX0 = new InterfaceMethodRef(getDeobfClass(), "getNormalizedX0", "()F");
            final InterfaceMethodRef getNormalizedX1 = new InterfaceMethodRef(getDeobfClass(), "getNormalizedX1", "()F");
            final InterfaceMethodRef interpolateX = new InterfaceMethodRef(getDeobfClass(), "interpolateX", "(D)F");
            final InterfaceMethodRef getNormalizedY0 = new InterfaceMethodRef(getDeobfClass(), "getNormalizedY0", "()F");
            final InterfaceMethodRef getNormalizedY1 = new InterfaceMethodRef(getDeobfClass(), "getNormalizedY1", "()F");
            final InterfaceMethodRef interpolateY = new InterfaceMethodRef(getDeobfClass(), "interpolateY", "(D)F");
            final InterfaceMethodRef getName = new InterfaceMethodRef(getDeobfClass(), "getName", "()Ljava/lang/String;");
            final InterfaceMethodRef getWidth = new InterfaceMethodRef(getDeobfClass(), "getTextureWidth", "()I");
            final InterfaceMethodRef getHeight = new InterfaceMethodRef(getDeobfClass(), "getTextureHeight", "()I");

            addClassSignature(new InterfaceSignature(
                getX0,
                getY0,
                getNormalizedX0,
                getNormalizedX1,
                interpolateX,
                getNormalizedY0,
                getNormalizedY1,
                interpolateY,
                getName,
                getWidth,
                getHeight
            ).setInterfaceOnly(true));
        }
    }

    public static class NBTTagCompoundMod extends ClassMod {
        private final InterfaceMethodRef containsKey = new InterfaceMethodRef("java/util/Map", "containsKey", "(Ljava/lang/Object;)Z");
        private final InterfaceMethodRef mapRemove = new InterfaceMethodRef("java/util/Map", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;");
        private final InterfaceMethodRef mapGet = new InterfaceMethodRef("java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
        private final InterfaceMethodRef mapPut = new InterfaceMethodRef("java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        private final FieldRef tagMap = new FieldRef(getDeobfClass(), "tagMap", "Ljava/util/Map;");

        public NBTTagCompoundMod() {
            setParentClass("NBTBase");

            addClassSignature(new ConstSignature(new ClassRef("java.util.HashMap")));
            addClassSignature(new ConstSignature(":["));
            addClassSignature(new ConstSignature(":"));
            addClassSignature(new ConstSignature(","));
            addClassSignature(new ConstSignature("]"));

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_1,
                        reference(INVOKEINTERFACE, containsKey),
                        IRETURN
                    );
                }
            }
                .setMethod(new MethodRef(getDeobfClass(), "hasKey", "(Ljava/lang/String;)Z"))
                .addXref(1, tagMap)
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_1,
                        reference(INVOKEINTERFACE, mapRemove)
                    );
                }
            }
                .setMethod(new MethodRef(getDeobfClass(), "removeTag", "(Ljava/lang/String;)V"))
                .addXref(1, tagMap)
            );

            mapNBTMethod("Byte", "B");
            mapNBTMethod("ByteArray", "[B");
            mapNBTMethod("Double", "D");
            mapNBTMethod("Float", "F");
            mapNBTMethod("IntArray", "[I");
            mapNBTMethod("Integer", "I");
            mapNBTMethod("Long", "J");
            mapNBTMethod("Short", "S");
            mapNBTMethod("String", "Ljava/lang/String;");

            addMemberMapper(new MethodMapper(null, new MethodRef(getDeobfClass(), "getBoolean", "(Ljava/lang/String;)Z")));
            addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "setBoolean", "(Ljava/lang/String;Z)V")));
            addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "getCompoundTag", "(Ljava/lang/String;)L" + getDeobfClass() + ";")));
            addMemberMapper(new MethodMapper(new MethodRef(getDeobfClass(), "setCompoundTag", "(Ljava/lang/String;L" + getDeobfClass() + ";)V")));
        }

        protected void mapNBTMethod(String type, String desc) {
            final MethodRef get = new MethodRef(getDeobfClass(), "get" + type, "(Ljava/lang/String;)" + desc);
            final MethodRef set = new MethodRef(getDeobfClass(), "set" + type, "(Ljava/lang/String;" + desc + ")V");
            final String nbtTagType = "NBTTag" + type;

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_1,
                        reference(INVOKEINTERFACE, mapGet),
                        captureReference(CHECKCAST),
                        captureReference(GETFIELD)
                    );
                }

                @Override
                public boolean afterMatch() {
                    getClassMap().addInheritance("NBTBase", nbtTagType);
                    return true;
                }
            }
                .setMethod(get)
                .addXref(1, tagMap)
                .addXref(2, new ClassRef(nbtTagType))
                .addXref(3, new FieldRef(nbtTagType, "data", desc))
            );

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        ALOAD_0,
                        captureReference(GETFIELD),
                        ALOAD_1,
                        captureReference(NEW),
                        DUP,
                        ALOAD_1,
                        subset(new int[]{ILOAD_2, ALOAD_2, LLOAD_2, FLOAD_2, DLOAD_2}, true),
                        captureReference(INVOKESPECIAL),
                        reference(INVOKEINTERFACE, mapPut)
                    );
                }
            }
                .setMethod(set)
                .addXref(1, tagMap)
                .addXref(2, new ClassRef(nbtTagType))
                .addXref(3, new MethodRef(nbtTagType, "<init>", "(Ljava/lang/String;" + desc + ")V"))
            );
        }
    }
}