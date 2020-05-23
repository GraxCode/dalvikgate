package me.nov.dalvikgate.transform.methods;

import java.util.Objects;

import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

import me.nov.dalvikgate.DexToASM;
import me.nov.dalvikgate.asm.ASMCommons;
import me.nov.dalvikgate.transform.ITransformer;
import me.nov.dalvikgate.transform.instructions.InstructionTransformer;
import me.nov.dalvikgate.transform.instructions.exception.UnsupportedInsnException;
import me.nov.dalvikgate.transform.instructions.postoptimize.*;
import me.nov.dalvikgate.utils.TextUtils;

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
    try {
      MutableMethodImplementation builder = new MutableMethodImplementation(method.getImplementation());
      mn.maxLocals = mn.maxStack = builder.getRegisterCount(); // we need this because some decompilers crash if this is zero
      InstructionTransformer it = new InstructionTransformer(mn, method, builder);
      it.visit(method);
      mn.instructions = it.getTransformed();
      if (!DexToASM.noOptimize) {
        new PostLocalRemover().applyPatch(mn);
        new PostDupInserter().applyPatch(mn);
        // new PostCombiner().applyPatch(mn); analyzer is probably the better way to do this
      }
    } catch (Exception e) {
      if (e instanceof UnsupportedInsnException) {
        DexToASM.logger.error("{} ::: {}", e.getStackTrace()[0], e.getMessage());
      } else {
        e.printStackTrace();
      }
      mn.instructions = ASMCommons.makeExceptionThrow("java/lang/IllegalStateException", e.toString() + " / " + TextUtils.stacktraceToString(e));
      mn.maxStack = 3;
      mn.tryCatchBlocks.clear();
    }
  }

}
