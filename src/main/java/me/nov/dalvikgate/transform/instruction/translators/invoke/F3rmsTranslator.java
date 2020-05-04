package me.nov.dalvikgate.transform.instruction.translators.invoke;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.instruction.*;

import me.nov.dalvikgate.dexlib.DexLibCommons;
import me.nov.dalvikgate.transform.instruction.*;
import me.nov.dalvikgate.utils.CustomMethodReference;

public class F3rmsTranslator extends AbstractInsnTranslator<BuilderInstruction3rms> {

  public F3rmsTranslator(InstructionTransformer it) {
    super(it);
  }

  public void translate(BuilderInstruction3rms i) {
    // optimized invoke instruction that uses indexes instead of method names and descs. This cannot be disassembled at all, only sort of
    Opcode real = i.getOpcode() == Opcode.INVOKE_VIRTUAL_QUICK_RANGE ? Opcode.INVOKE_VIRTUAL : Opcode.INVOKE_SUPER;
    BuilderInstruction next = getNextOf(i);
    new F3rcTranslator(it).translate(new BuilderInstruction3rc(real, i.getRegisterCount(), i.getStartRegister(), new CustomMethodReference("Ljava/lang/Object;",
        "$$$method_vtable_" + i.getVtableIndex() + "_argsize_" + (i.getRegisterCount() - 1), DexLibCommons.generateFakeQuickDesc(i.getRegisterCount(), next))), getNextOf(i));
  }
}
