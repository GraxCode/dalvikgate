package me.nov.dalvikgate.transform.instruction.translators.invoke;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.instruction.*;

import me.nov.dalvikgate.dexlib.DexLibCommons;
import me.nov.dalvikgate.transform.instruction.*;
import me.nov.dalvikgate.utils.CustomMethodReference;

public class F35msTranslator extends AbstractInsnTranslator<BuilderInstruction35ms> {

  public F35msTranslator(InstructionTransformer it) {
    super(it);
  }

  public void translate(BuilderInstruction35ms i) {
    // we can't really translate this to java, as it uses the current object on stack as class, and invokes the method at vtable index
    // TODO this could be translated to reflection
    Opcode real = i.getOpcode() == Opcode.INVOKE_VIRTUAL_QUICK ? Opcode.INVOKE_VIRTUAL : Opcode.INVOKE_SUPER;
    BuilderInstruction next = getNextOf(i);
    new F35cTranslator(it).translate(
        new BuilderInstruction35c(real, i.getRegisterCount(), i.getRegisterC(), i.getRegisterD(), i.getRegisterE(), i.getRegisterF(), i.getRegisterG(), new CustomMethodReference("Ljava/lang/Object;",
            "$invoke_index_" + i.getVtableIndex() + "_registers_" + i.getRegisterCount(), DexLibCommons.generateFakeQuickDesc(i.getRegisterCount(), next))));
  }
}
