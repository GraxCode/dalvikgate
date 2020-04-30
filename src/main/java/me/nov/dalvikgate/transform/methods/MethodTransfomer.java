package me.nov.dalvikgate.transform.methods;

import java.util.Objects;

import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

import me.nov.dalvikgate.asm.ASMCommons;
import me.nov.dalvikgate.transform.ITransformer;
import me.nov.dalvikgate.transform.instruction.InstructionTransformer;

public class MethodTransfomer implements ITransformer<DexBackedMethod, MethodNode>, Opcodes {
  private MethodNode mn;

  @Override
  public void build(DexBackedMethod method) {
    String name = method.getName();
    int flags = method.getAccessFlags();
    if (name.startsWith("$$") || name.startsWith("_$_") || name.startsWith("access$")) {
      flags |= ACC_BRIDGE | ACC_SYNTHETIC;
    }
    mn = new MethodNode(flags, name, ASMCommons.buildMethodDesc(method.getParameterTypes(), method.getReturnType()), null, null);
    rewriteImplementation(method);
  }

  @Override
  public MethodNode getTransformed() {
    return Objects.requireNonNull(mn);
  }

  private void rewriteImplementation(DexBackedMethod method) {
    // If no implementation, do nothing
    if (method.getImplementation() == null) {
      return;
    }
    MutableMethodImplementation builder = new MutableMethodImplementation(method.getImplementation());
    InstructionTransformer it = new InstructionTransformer(mn, method, builder);
    try {
      it.visit(method);
    } catch (Exception e) {
      e.printStackTrace();
      mn.instructions = ASMCommons.makeExceptionThrow("java/lang/IllegalArgumentException",
              "dalvikgate error: " + e.toString() + " / " + (e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : " no stack trace"));
      return;
    }
    mn.instructions = it.getTransformed();
  }

}
