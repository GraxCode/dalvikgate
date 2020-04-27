package me.nov.dalvikgate.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public class ASMCommons implements Opcodes {

	public static AbstractInsnNode makeNullPush(Type type) {
		switch (type.getSort()) {
		case Type.OBJECT:
		case Type.ARRAY:
			return new InsnNode(ACONST_NULL);
		case Type.VOID:
			return new InsnNode(NOP);
		case Type.DOUBLE:
			return new InsnNode(DCONST_0);
		case Type.FLOAT:
			return new InsnNode(FCONST_0);
		case Type.LONG:
			return new InsnNode(LCONST_0);
		default:
			return new InsnNode(ICONST_0);
		}
	}

	public static AbstractInsnNode makeIntPush(int i) {
		if (i >= -1 && i <= 5) {
			return new InsnNode(i + 3); // iconst_i
		}
		if (i >= -128 && i <= 127) {
			return new IntInsnNode(BIPUSH, i);
		}

		if (i >= -32768 && i <= 32767) {
			return new IntInsnNode(SIPUSH, i);
		}
		return new LdcInsnNode(i);
	}
}
