package com.prupe.mcpatcher.basemod;

import com.prupe.mcpatcher.*;
import javassist.bytecode.AccessFlag;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.prupe.mcpatcher.BytecodeMatcher.anyReference;
import static com.prupe.mcpatcher.BytecodeMatcher.captureReference;
import static com.prupe.mcpatcher.BytecodeMatcher.registerLoadStore;
import static javassist.bytecode.Opcode.*;

public class PositionMod extends com.prupe.mcpatcher.ClassMod {
    private static FieldRef i = new FieldRef("Position", "i", "I");
    private static FieldRef j = new FieldRef("Position", "j", "I");
    private static FieldRef k = new FieldRef("Position", "k", "I");
    private static MethodRef getI = new MethodRef("Position", "getI", "()I");
    private static MethodRef getJ = new MethodRef("Position", "getJ", "()I");
    private static MethodRef getK = new MethodRef("Position", "getK", "()I");

    public static boolean setup(Mod mod) {
        if (havePositionClass()) {
            mod.addClassMod(new PositionMod(mod));
            if (Mod.getMinecraftVersion().compareTo("14w04a") >= 0) {
                mod.addClassMod(new PositionBaseMod(mod));
            }
            mod.addClassMod(new DirectionMod(mod));
            return true;
        } else {
            return false;
        }
    }

    public static boolean havePositionClass() {
        return Mod.getMinecraftVersion().compareTo("14w02a") >= 0;
    }

    public static String getDescriptor() {
        return havePositionClass() ? "LPosition;" : "III";
    }

    public static int getDescriptorLength() {
        return havePositionClass() ? 1 : 3;
    }

    public static String getDescriptorIKOnly() {
        return havePositionClass() ? "LPosition;" : "II";
    }

    public static int getDescriptorLengthIKOnly() {
        return havePositionClass() ? 1 : 2;
    }

    public static Object unpackArguments(PatchComponent patchComponent, int register) {
        if (havePositionClass()) {
            // position.getI(), position.getJ(), position.getK()
            return new Object[]{
                registerLoadStore(ALOAD, register),
                patchComponent.reference(INVOKEVIRTUAL, getI),
                registerLoadStore(ALOAD, register),
                patchComponent.reference(INVOKEVIRTUAL, getJ),
                registerLoadStore(ALOAD, register),
                patchComponent.reference(INVOKEVIRTUAL, getK)
            };
        } else {
            // i, j, k
            return passArguments(register);
        }
    }

    public static byte[] passArguments(int register) {
        if (havePositionClass()) {
            // position
            return registerLoadStore(ALOAD, register);
        } else {
            // i, j, k
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try {
                output.write(registerLoadStore(ILOAD, register));
                output.write(registerLoadStore(ILOAD, register + 1));
                output.write(registerLoadStore(ILOAD, register + 2));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return output.toByteArray();
        }
    }

    protected PositionMod(Mod mod) {
        super(mod);

        if (Mod.getMinecraftVersion().compareTo("14w04a") >= 0) {
            setParentClass("PositionBase");

            addClassSignature(new BytecodeSignature() {
                @Override
                public String getMatchExpression() {
                    return buildExpression(
                        // h = 64 - f - g;
                        push(64),
                        anyReference(GETSTATIC),
                        ISUB,
                        anyReference(GETSTATIC),
                        ISUB,
                        anyReference(PUTSTATIC)
                    );
                }
            }.matchStaticInitializerOnly(true));
        } else {
            addBaseSignatures(this);
        }
    }

    private static class PositionBaseMod extends com.prupe.mcpatcher.ClassMod {
        public PositionBaseMod(Mod mod) {
            super(mod);

            addBaseSignatures(this);
        }
    }

    private static void addBaseSignatures(ClassMod classMod) {
        String deobfClass = classMod.getDeobfClass();
        final MethodRef hashCode = new MethodRef(deobfClass, "hashCode", "()I");

        i = new FieldRef(deobfClass, "i", "I");
        j = new FieldRef(deobfClass, "j", "I");
        k = new FieldRef(deobfClass, "k", "I");
        getI = new MethodRef(deobfClass, "getI", "()I");
        getJ = new MethodRef(deobfClass, "getJ", "()I");
        getK = new MethodRef(deobfClass, "getK", "()I");

        classMod.addClassSignature(new com.prupe.mcpatcher.BytecodeSignature(classMod) {
            @Override
            public String getMatchExpression() {
                return buildExpression(
                    // (this.j + this.k * 31) * 31 + this.i
                    ALOAD_0,
                    captureReference(GETFIELD),
                    ALOAD_0,
                    captureReference(GETFIELD),
                    push(31),
                    IMUL,
                    IADD,
                    push(31),
                    IMUL,
                    ALOAD_0,
                    captureReference(GETFIELD),
                    IADD
                );
            }
        }
            .setMethod(hashCode)
            .addXref(1, j)
            .addXref(2, k)
            .addXref(3, i)
        );

        classMod.addMemberMapper(new com.prupe.mcpatcher.MethodMapper(classMod, null, getI, getJ, getK)
            .accessFlag(AccessFlag.PUBLIC, true)
            .accessFlag(AccessFlag.STATIC, false)
        );
    }
}