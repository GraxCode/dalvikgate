package me.nov.dalvikgate.transform.instructions.resolving;

import java.util.ArrayList;

import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import me.nov.dalvikgate.DexToASM;
import me.nov.dalvikgate.asm.Access;
import me.nov.dalvikgate.dexlib.DexLibCommons;
import me.nov.dalvikgate.transform.instructions.unresolved.UnresolvedVarInsn;
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

      boolean usedAggressive = false;
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
          if (aggressive) {
            usedAggressive = true;
            aggressive = false; // use normal method again, no false resulutions wanted
          }
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
      if (aggressive || usedAggressive) {
        addAnnotation("AggressivelyResolved");
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
      DexToASM.logger.error("{} missing unresolved instructions in {}: {}{}", unresolvedCount, owner, method.getName(), DexLibCommons.getMethodDesc(method));
    }
  }

  private void addAnnotation(String string) {
    if (mn.visibleAnnotations == null)
      mn.visibleAnnotations = new ArrayList<>();
    mn.visibleAnnotations.add(new AnnotationNode("Lme/nov/dalvikgate/" + string + ";"));
  }
}
