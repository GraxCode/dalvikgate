package me.nov.dalvikgate.transform.methods;

import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import me.nov.dalvikgate.transform.ITransformer;

public class MethodTransfomer implements ITransformer<MethodNode>, Opcodes {

	private final DexBackedMethod method;
	private MethodNode mn;

	public MethodTransfomer(DexBackedMethod method) {
		this.method = method;
	}

	@Override
	public void build() {
		mn = new MethodNode(method.getAccessFlags(), method.getName(),
				Type.getMethodDescriptor(Type.getType(method.getReturnType()), method.getParameterTypes().stream().map(s -> Type.getType(s)).toArray(Type[]::new)), null, null);
		// TODO localVariables
		// TODO tryCatch
		if (method.getImplementation() != null) {
			rewriteCode();
		}
	}

	@Override
	public MethodNode get() {
		return mn;
	}

	private void rewriteCode() {
		MutableMethodImplementation builder = new MutableMethodImplementation(method.getImplementation());
		InstructionTransformer it = new InstructionTransformer(mn, method, builder);
		try {
			it.build();
		} catch (Exception e) {
			e.printStackTrace();
			mn.instructions = tranlationException(e);
			return;
		}
		mn.instructions = it.get();
	}

	private InsnList tranlationException(Exception e) {
		InsnList il = new InsnList();
		il.add(new TypeInsnNode(NEW, "java/lang/IllegalArgumentException"));
		il.add(new InsnNode(DUP));
		il.add(new LdcInsnNode("dalvikgate error: " + e.toString()));
		il.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V"));
		il.add(new InsnNode(ATHROW));
		return il;
	}

}
