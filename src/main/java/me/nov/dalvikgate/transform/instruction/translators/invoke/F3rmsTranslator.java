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
    // we can't really translate this to java, as it uses the current object on stack as class, and invokes the method at vtable index
    // TODO this could be translated to reflection, but would be a lot of work
    Opcode real = i.getOpcode() == Opcode.INVOKE_VIRTUAL_QUICK_RANGE ? Opcode.INVOKE_VIRTUAL : Opcode.INVOKE_SUPER;
    BuilderInstruction next = getNextOf(i);
    new F3rcTranslator(it).translate(new BuilderInstruction3rc(real, i.getRegisterCount(), i.getStartRegister(), new CustomMethodReference("Ljava/lang/Object;",
        "$invoke_range_index_" + i.getVtableIndex() + "_registers_" + i.getRegisterCount(), DexLibCommons.generateFakeQuickDesc(i.getRegisterCount(), next))));
  }
}
