package me.nov.dalvikgate.transform.instructions.resolving;

import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import me.coley.analysis.*;
import me.coley.analysis.value.AbstractValue;
import me.nov.dalvikgate.DexToASM;
import me.nov.dalvikgate.dexlib.DexLibCommons;
import me.nov.dalvikgate.graph.Inheritance;
import me.nov.dalvikgate.transform.instructions.IUnresolvedInstruction;
import me.nov.dalvikgate.transform.instructions.exception.TranslationException;
import me.nov.dalvikgate.transform.instructions.unresolved.*;

public class InstructionResolver implements Opcodes {
  private final Inheritance inheritance;
  private final DexBackedMethod method;
  private final MethodNode mn;
  private final InsnList il;
  private SimAnalyzer analyzer;

  public InstructionResolver(Inheritance inheritance, DexBackedMethod method, MethodNode mn, InsnList il) {
    this.inheritance = inheritance;
    this.method = method;
    this.mn = mn;
    this.il = il;
    setupAnalyzer();
  }

  private void setupAnalyzer() {
    SimInterpreter it = new SimInterpreter();
    analyzer = new SimAnalyzer(it) {
      @Override
      protected TypeChecker createTypeChecker() {
        return (parent, child) -> inheritance.getAllChildren(parent.getInternalName()).contains(child.getInternalName());
      }
    };
    analyzer.setThrowUnresolvedAnalyzerErrors(false);
    analyzer.setSkipDeadCodeBlocks(false);
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
      // VARIABLES
      Frame<AbstractValue>[] frames = analyzer.analyze(owner, mn);
      for (int i = il.size() - 1; i >= 0; i--) {
        AbstractInsnNode insn = il.get(i);
        if (insn instanceof UnresolvedVarInsn) {
          IUnresolvedInstruction resolvable = (IUnresolvedInstruction) insn;
          if (resolvable.isResolved())
            continue;
          if (!resolvable.tryResolve(i, mn, frames))
            throw new TranslationException("Failed to patch unresolved instruction: " + insn.getClass().getSimpleName() + " - " + mn.name + mn.desc);
        }
      }
      // NUMBERS
      frames = analyzer.analyze(owner, mn);
      for (int i = il.size() - 1; i >= 0; i--) {
        AbstractInsnNode insn = il.get(i);
        if (insn instanceof UnresolvedNumberInsn) {
          IUnresolvedInstruction resolvable = (IUnresolvedInstruction) insn;
          if (resolvable.isResolved())
            continue;
          if (!resolvable.tryResolve(i, mn, frames))
            throw new TranslationException("Failed to patch unresolved instruction: " + insn.getClass().getSimpleName() + " - " + mn.name + mn.desc);
        }
      }
      // WIDE ARRAY
      frames = analyzer.analyze(owner, mn);
      for (int i = il.size() - 1; i >= 0; i--) {
        AbstractInsnNode insn = il.get(i);
        if (insn instanceof UnresolvedWideArrayInsn) {
          IUnresolvedInstruction resolvable = (IUnresolvedInstruction) insn;
          if (resolvable.isResolved())
            continue;
          if (!resolvable.tryResolve(i, mn, frames))
            throw new TranslationException("Failed to patch unresolved instruction: " + insn.getClass().getSimpleName() + " - " + mn.name + mn.desc);
        }
      }
      // JUMP
      frames = analyzer.analyze(owner, mn);
      for (int i = il.size() - 1; i >= 0; i--) {
        AbstractInsnNode insn = il.get(i);
        if (insn instanceof UnresolvedJumpInsn) {
          IUnresolvedInstruction resolvable = (IUnresolvedInstruction) insn;
          if (resolvable.isResolved())
            continue;
          if (!resolvable.tryResolve(i, mn, frames))
            throw new TranslationException("Failed to patch unresolved instruction: " + insn.getClass().getSimpleName() + " - " + mn.name + mn.desc);
        }
      }

      DexToASM.logger.info(" - Success: {}", frames.length);
    } catch (AnalyzerException ex) {
      DexToASM.logger.error(" - Analyzer error: {}", ex.getMessage());
      mn.instructions = initialIl;
      return;
    } catch (TranslationException ex) {
      DexToASM.logger.error(" - Translation error: {}", ex.getMessage());
      mn.instructions = initialIl;
      return;
    } catch (Throwable t) {
      t.printStackTrace();
      DexToASM.logger.error(" - Analyzer crash: {}", t.getMessage());
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
}
