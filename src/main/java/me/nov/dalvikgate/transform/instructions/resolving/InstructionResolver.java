package me.nov.dalvikgate.transform.instructions.resolving;

import java.util.*;

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

      boolean resolvedUnused = false;
      while (true) {
        while (linkByStack()) {
          // repeat while unresolved instructions decrease
        }

        replaceNullLdcs(); // replace null ldcs with aconst_null

        // TODO: remove the next two, replace them by using varTypes list in TypeResolver.
        if (linkByLocals())
          continue; // connected some locals, restart
//        if (linkByParameters())
//          continue; // inlined parameter types, restart
//        
        
        if (!resolvedUnused && resolveRemainingUnused()) {
          // final pass, restart
          resolvedUnused = true;
          continue;
        }
        break;
      }
    } catch (AnalyzerException ex) {
      DexToASM.logger.error("Analyzer error: {}: {}{}", ex, owner, method.getName(), DexLibCommons.getMethodDesc(method));
      mn.instructions = initialIl;
      addAnnotation("TypeResolutionFailed", ex.getMessage());
      return;
    } catch (Throwable t) {
      // to find small samples: StreamSupport.stream(method.getImplementation().getInstructions().spliterator(), false).count()
      DexToASM.logger.error("Analyzer crash: {}: {}{}", t, owner, method.getName(), DexLibCommons.getMethodDesc(method));
      addAnnotation("TypeResolutionCrashed", t.getMessage());
      mn.instructions = initialIl;
      return;
    }
    int unresolvedCount = UnresolvedUtils.countUnresolved(il);
    if (unresolvedCount > 0) {
      addAnnotation("TypeResolutionIncomplete", unresolvedCount + " instructions unresolved, used default opcode");
      DexToASM.logger.error("{} missing unresolved instructions in {}: {}{}", unresolvedCount, owner, method.getName(), DexLibCommons.getMethodDesc(method));
    }
  }

  private boolean resolveRemainingUnused() {
    boolean resolved = false;
    for (int i = il.size() - 1; i >= 0; i--) {
      AbstractInsnNode insn = il.get(i);
      if (insn instanceof UnresolvedNumberInsn && !((UnresolvedNumberInsn) insn).isResolved()) {
        UnresolvedNumberInsn num = (UnresolvedNumberInsn) insn;
        num.setType(num.isWide() ? Type.LONG_TYPE : Type.INT_TYPE);
        resolved = true;
      }
    }
    return resolved;
  }

  private void replaceNullLdcs() {
    for (int i = il.size() - 1; i >= 0; i--) {
      AbstractInsnNode insn = il.get(i);
      if (insn instanceof UnresolvedNumberInsn) {
        if (((UnresolvedNumberInsn) insn).cst.equals(Type.getType("V"))) {
          // replace actual null ldcs with aconst_null
          il.set(insn, new InsnNode(ACONST_NULL));
        }
      }
    }
  }

  private boolean linkByParameters() {
    int prevCount = UnresolvedUtils.countUnresolved(il);

    boolean isStatic = Access.isStatic(mn.access);
    Type[] args = Type.getArgumentTypes(mn.desc);

    for (int i = il.size() - 1; i >= 0; i--) {
      AbstractInsnNode insn = il.get(i);
      if (insn instanceof UnresolvedVarInsn && !((UnresolvedVarInsn) insn).isResolved()) {
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
    return prevCount - UnresolvedUtils.countUnresolved(il) > 0;
  }

  private boolean linkByLocals() {
    int prevCount = UnresolvedUtils.countUnresolved(il);
    for (int i = il.size() - 1; i >= 0; i--) {
      AbstractInsnNode insn = il.get(i);
      if (insn instanceof UnresolvedVarInsn) {
        UnresolvedVarInsn resolvable = (UnresolvedVarInsn) insn;
        if (resolvable.isResolved())
          continue;
        resolvable.tryResolveUnlinked(i, mn);
      }
    }
    return prevCount - UnresolvedUtils.countUnresolved(il) > 0;
  }

  private boolean linkByStack() throws AnalyzerException {
    int prevCount = UnresolvedUtils.countUnresolved(il);
    new Analyzer<>(new TypeResolver(false, Type.getArgumentTypes(mn.desc), mn.maxStack)).analyze(Type.getType(method.getDefiningClass()).getInternalName(), mn);
    return prevCount - UnresolvedUtils.countUnresolved(il) > 0;
  }

  private void addAnnotation(String string, String description) {
    if (mn.visibleAnnotations == null)
      mn.visibleAnnotations = new ArrayList<>();
    AnnotationNode an = new AnnotationNode("Lme/nov/dalvikgate/" + string + ";");
    an.values = Arrays.asList("cause", description);
    mn.visibleAnnotations.add(an);
  }
}
