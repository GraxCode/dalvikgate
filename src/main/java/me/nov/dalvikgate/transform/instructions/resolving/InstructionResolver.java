package me.nov.dalvikgate.transform.instructions.resolving;

import java.util.ArrayList;

import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import me.nov.dalvikgate.DexToASM;
import me.nov.dalvikgate.asm.Access;
import me.nov.dalvikgate.dexlib.DexLibCommons;
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
    InsnList initialIl = mn.instructions;
    try {
      mn.instructions = il;
      // TODO: Properly set these beforehand
      mn.maxLocals = 100;
      mn.maxStack = 100;
      boolean isStatic = Access.isStatic(mn.access);
      Type[] args = Type.getArgumentTypes(mn.desc);
      // first resolve local variables linked with args
      for (int i = il.size() - 1; i >= 0; i--) {
        AbstractInsnNode insn = il.get(i);
        if (insn instanceof UnresolvedVarInsn) {
          UnresolvedVarInsn resolvable = (UnresolvedVarInsn) insn;
          if (isStatic) {
            if (resolvable.var < args.length)
              resolvable.setType(args[resolvable.var]);
          } else {
            if (resolvable.var == 0)
              resolvable.setType(Type.getType(Object.class)); // this reference
            else if (resolvable.var <= args.length)
              resolvable.setType(args[resolvable.var - 1]);
          }
        }
      }

      boolean aggressive = false;
      boolean finalPass = false;
      
      int unresolvedCount = UnresolvedUtils.countUnresolved(il);

      // ALGORITHM
      // 1: multiple passes using type resolver -> repeat if unresolved decrease
      // 2: try link locals -> go to 1 if unresolved decrease
      // 3: continue with 4 if aggressive, else go to 1 and enable aggressive
      // 4: enable final pass and go to 2
      while (true) {
        new Analyzer<>(new TypeResolver(aggressive)).analyze(owner, mn);
        for (int i = il.size() - 1; i >= 0; i--) {
          AbstractInsnNode insn = il.get(i);
          if (insn instanceof UnresolvedNumberInsn) {
            if (((UnresolvedNumberInsn) insn).cst.equals(Type.getType("V"))) {
              // replace actual null ldcs with aconst_null
              il.set(insn, new InsnNode(ACONST_NULL));
            }
          }
        }
        int newUnresolvedCount = UnresolvedUtils.countUnresolved(il);
        if (newUnresolvedCount == 0)
          break; // everything resolved, break!
        if (newUnresolvedCount < unresolvedCount) {
          if (aggressive) {
            aggressive = false; // use normal method again, no false resulutions wanted
          }
          unresolvedCount = newUnresolvedCount;
          continue;
        }
        // link variable types together, see if we can improve something this way
        for (int i = il.size() - 1; i >= 0; i--) {
          AbstractInsnNode insn = il.get(i);
          if (insn instanceof UnresolvedVarInsn) {
            UnresolvedVarInsn resolvable = (UnresolvedVarInsn) insn;
            if (resolvable.isResolved())
              continue;
            resolvable.tryResolveUnlinked(i, mn, finalPass);
          }
        }
        newUnresolvedCount = UnresolvedUtils.countUnresolved(il);
        if (newUnresolvedCount < unresolvedCount) {
          // redo everything!
          if (aggressive) {
            aggressive = false; // use normal method again, no false resulutions wanted
          }
          unresolvedCount = newUnresolvedCount; // set state
          continue;
        }
        // nothing has changed, still the same amount of unresolved instructions
        if (!aggressive) {
          aggressive = true;
          continue;
        }
        if (!finalPass) {
          // resolve variables without any use
          finalPass = true;
          aggressive = false;
          continue;
        }
        break;
      }
    } catch (AnalyzerException ex) {
      DexToASM.logger.error("Analyzer error: {}: {}{}", ex, owner, method.getName(), DexLibCommons.getMethodDesc(method));
      mn.instructions = initialIl;
      addAnnotation("TypeResolutionFailed");
      return;
    } catch (Throwable t) {
      DexToASM.logger.error("Analyzer crash: {}: {}{}", t, owner, method.getName(), DexLibCommons.getMethodDesc(method));
      addAnnotation("TypeResolutionCrashed");
      mn.instructions = initialIl;
      return;
    }
    int unresolvedCount = UnresolvedUtils.countUnresolved(il);
    if (unresolvedCount > 0) {
      addAnnotation("TypeResolutionIncomplete");
      DexToASM.logger.error("{} missing unresolved instructions in {}: {}{}", unresolvedCount, owner, method.getName(), DexLibCommons.getMethodDesc(method));
    }
  }

  private void addAnnotation(String string) {
    if (mn.visibleAnnotations == null)
      mn.visibleAnnotations = new ArrayList<>();
    mn.visibleAnnotations.add(new AnnotationNode("Lme/nov/dalvikgate/" + string + ";"));
  }
}
