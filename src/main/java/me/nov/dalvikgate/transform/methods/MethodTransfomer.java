package me.nov.dalvikgate.transform.methods;

import java.util.Objects;

import me.nov.dalvikgate.transform.instruction.exception.UnsupportedInsnException;
import me.nov.dalvikgate.transform.instruction.post.PostDupInserter;
import me.nov.dalvikgate.utils.TextUtils;

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
    mn.maxLocals = mn.maxStack = builder.getRegisterCount(); //we need this because some decompilers crash if this is zero
    InstructionTransformer it = new InstructionTransformer(mn, method, builder);
    try {
      it.visit(method);
    } catch (Exception e) {
      if (e instanceof UnsupportedInsnException) {
        System.err.println(e.getStackTrace()[0] + " ::: " + e.getMessage());
      } else {
        e.printStackTrace();
      }
      mn.instructions = ASMCommons.makeExceptionThrow("java/lang/IllegalStateException",
              "dalvikgate error: " + e.toString() + " / " + TextUtils.stacktraceToString(e));
      mn.maxStack = 3;
      mn.tryCatchBlocks.clear();
      return;
    }
    mn.instructions = it.getTransformed();
    PostDupInserter dups = new PostDupInserter();
    dups.visit(mn.instructions);
  }

}
