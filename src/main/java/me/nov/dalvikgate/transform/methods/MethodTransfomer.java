package me.nov.dalvikgate.transform.methods;

import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

import me.nov.dalvikgate.asm.ASMCommons;
import me.nov.dalvikgate.transform.ITransformer;

public class MethodTransfomer implements ITransformer<MethodNode>, Opcodes {

  private final DexBackedMethod method;
  private MethodNode mn;

  public MethodTransfomer(DexBackedMethod method) {
    this.method = method;
  }

  @Override
  public void build() {
    String name = method.getName();
    int flags = method.getAccessFlags();
    if (name.startsWith("$$") || name.startsWith("_$_") || name.startsWith("access$")) {
      flags |= ACC_BRIDGE | ACC_SYNTHETIC;
    }
    mn = new MethodNode(flags, name, ASMCommons.buildMethodDesc(method.getParameterTypes(), method.getReturnType()), null, null);
    if (method.getImplementation() != null) {
      rewriteImplementation();
    }
  }

  @Override
  public MethodNode get() {
    return mn;
  }

  private void rewriteImplementation() {
    MutableMethodImplementation builder = new MutableMethodImplementation(method.getImplementation());
    InstructionTransformer it = new InstructionTransformer(mn, method, builder);
    try {
      it.build();
    } catch (Exception e) {
//      if (!e.toString().contains("unsupported instruction"))
//        e.printStackTrace();
      mn.instructions = ASMCommons.makeExceptionThrow("java/lang/IllegalArgumentException", "dalvikgate error: " + e.toString() + " / " + e.getStackTrace()[0].toString());
      return;
    }
    mn.instructions = it.get();
  }

}
