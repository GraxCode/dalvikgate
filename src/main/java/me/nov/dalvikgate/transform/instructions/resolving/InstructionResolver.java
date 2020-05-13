package me.nov.dalvikgate.transform.instructions.resolving;

import me.coley.analysis.SimAnalyzer;
import me.coley.analysis.SimInterpreter;
import me.coley.analysis.TypeChecker;
import me.coley.analysis.util.FrameUtil;
import me.coley.analysis.value.AbstractValue;
import me.nov.dalvikgate.DexToASM;
import me.nov.dalvikgate.asm.ASMCommons;
import me.nov.dalvikgate.dexlib.DexLibCommons;
import me.nov.dalvikgate.graph.Inheritance;
import me.nov.dalvikgate.transform.instructions.IUnresolvedInstruction;
import me.nov.dalvikgate.transform.instructions.exception.TranslationException;
import me.nov.dalvikgate.transform.instructions.unresolved.UnresolvedJumpInsn;
import me.nov.dalvikgate.transform.instructions.unresolved.UnresolvedNumberInsn;
import me.nov.dalvikgate.transform.instructions.unresolved.UnresolvedVarInsn;
import me.nov.dalvikgate.transform.instructions.unresolved.UnresolvedWideArrayInsn;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.List;

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
      visitNormalInnstructions(owner);
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

  private void visitNormalInnstructions(String owner) throws AnalyzerException {
    Frame<AbstractValue>[] frames = analyzer.analyze(owner, mn);
    for (int i = 0; i < il.size(); i++) {
      Frame<AbstractValue> frame = frames[i];
      AbstractInsnNode insn = il.get(i);
      if (insn instanceof IUnresolvedInstruction && !((IUnresolvedInstruction) insn).isResolved())
        continue;
      int op = insn.getOpcode();
      // Check insns that take one operand of a known type
      if ((op >= I2L && op <= I2S) || (op >= IRETURN && op <= ARETURN) || (op >= ARRAYLENGTH && op <= MONITOREXIT) ||
              (op >= IFEQ && op <= IFLE) || (op >= ISTORE && op <= ASTORE)) {
        Type type = ASMCommons.getOperatingType(insn);
        AbstractInsnNode stackTop1 = last(FrameUtil.getTopStack(frame).getInsns());
        if (stackTop1 instanceof IUnresolvedInstruction) {
          ((IUnresolvedInstruction) stackTop1).setType(type);
        }
      }
      // Check insns that take two operand of the same type
      if ((op >= IADD && op <= LXOR) || (op >= LCMP && op <= DCMPG) || (op >= IF_ICMPEQ && op <= IF_ICMPLE)) {
        Type type = ASMCommons.getOperatingType(insn);
        AbstractInsnNode stackTop1 = last(FrameUtil.getTopStack(frame).getInsns());
        AbstractInsnNode stackTop2 = last(FrameUtil.getStackFromTop(frame, 1).getInsns());
        if (stackTop1 instanceof IUnresolvedInstruction) {
          ((IUnresolvedInstruction) stackTop1).setType(type);
        }
        if (stackTop2 instanceof IUnresolvedInstruction) {
          ((IUnresolvedInstruction) stackTop2).setType(type);
        }
      }
      // TODO: Check insns that take variable operand types
      //  - MethodInsnNode
      //    - owner, args
      //  - FielInsnNode
      //    - owner, type (put)
    }
  }

  private static <T> T last(List<T> list) {
    return list.get(list.size() - 1);
  }
}
