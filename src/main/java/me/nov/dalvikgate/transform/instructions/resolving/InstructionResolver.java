package me.nov.dalvikgate.transform.instructions.resolving;

import java.util.ArrayList;

import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import me.nov.dalvikgate.DexToASM;
import me.nov.dalvikgate.dexlib.DexLibCommons;
import me.nov.dalvikgate.transform.instructions.IUnresolvedInstruction;
import me.nov.dalvikgate.transform.instructions.unresolved.*;
import me.nov.dalvikgate.utils.UnresolvedUtils;

public class InstructionResolver implements Opcodes {
  private final DexBackedMethod method;
  private final MethodNode mn;
  private final InsnList il;

  public InstructionResolver(DexBackedMethod method, MethodNode mn, InsnList il) {
    this.method = method;
    this.mn = mn;
    this.il = il;
  }

  public void run() {
    String owner = Type.getType(method.getDefiningClass()).getInternalName();
    DexToASM.logger.error("{}.{}{}", owner, method.getName(), DexLibCommons.getMethodDesc(method));
    InsnList initialIl = mn.instructions;
    try {
      mn.instructions = il;
      // TODO: Properly set these beforehand
      mn.maxLocals = 100;
      mn.maxStack = 100;
      boolean aggressive = false;
      int unresolved = UnresolvedUtils.countUnresolved(il);
      while (true) {
        new Analyzer<>(new TypeResolver(aggressive)).analyze(owner, mn);
        for (int i = il.size() - 1; i >= 0; i--) {
          AbstractInsnNode insn = il.get(i);
          if (insn instanceof UnresolvedVarInsn) {
            UnresolvedVarInsn resolvable = (UnresolvedVarInsn) insn;
            if (resolvable.isResolved())
              continue;
            resolvable.tryResolveUnlinked(i, mn);
          }
        }
        int newUnresolved = UnresolvedUtils.countUnresolved(il);
        if (newUnresolved == 0)
          break; // everything resolved, break!
        if (newUnresolved < unresolved) {
          aggressive = false; // use normal method again, no false resulutions wanted
          unresolved = newUnresolved; // set state
          continue;
        }
        // nothing has changed, still the same amount of unresolved instructions
        if (!aggressive) {
          aggressive = true;
          continue;
        }
        break;
      }
    } catch (AnalyzerException ex) {
      DexToASM.logger.error(" - Analyzer error: {}", ex.getMessage());
      mn.instructions = initialIl;
      addAnnotation("TypeResolutionFailed");
      return;
    } catch (Throwable t) {
      DexToASM.logger.error(" - Analyzer crash: {}", t.getMessage());
      addAnnotation("TypeResolutionCrashed");
      mn.instructions = initialIl;
      return;
    }
    // Log missing
    int i = -1;
    for (AbstractInsnNode insn : il) {
      i++;
      // Skip resolved instructions
      if (insn instanceof IUnresolvedInstruction && ((IUnresolvedInstruction) insn).isResolved())
        continue;
      // Log unresolved type
      if (insn instanceof UnresolvedJumpInsn) {
        DexToASM.logger.error("   - {} : unresolved JUMP", i);
      } else if (insn instanceof UnresolvedVarInsn) {
        DexToASM.logger.error("   - {} : unresolved VARIABLE", i);
      } else if (insn instanceof UnresolvedWideArrayInsn) {
        DexToASM.logger.error("   - {} : unresolved WIDE ARRAY", i);
      } else if (insn instanceof UnresolvedNumberInsn) {
        DexToASM.logger.error("   - {} : unresolved NUMBER", i);
      }
    }
  }

  private void addAnnotation(String string) {
    if (mn.visibleAnnotations == null)
      mn.visibleAnnotations = new ArrayList<>();
    mn.visibleAnnotations.add(new AnnotationNode("Lme/nov/dalvikgate/" + string + ";"));
  }
}
