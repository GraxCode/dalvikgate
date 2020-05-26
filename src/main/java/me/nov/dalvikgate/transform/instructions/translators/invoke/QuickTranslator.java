package me.nov.dalvikgate.transform.instructions.translators.invoke;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.instruction.*;

import me.nov.dalvikgate.dexlib.DexLibCommons;
import me.nov.dalvikgate.reference.CustomMethodReference;
import me.nov.dalvikgate.transform.instructions.*;

public class QuickTranslator extends AbstractInsnTranslator<BuilderInstruction35ms> {

  public QuickTranslator(InstructionTransformer it) {
    super(it);
  }

  public void translate(BuilderInstruction35ms i) {
    // TODO: make UnresolvedQuickInvokeInsn to resolve desc by locals
    // optimized invoke instruction that uses indexes instead of method names and descs. This cannot be disassembled at all, only sort of
    Opcode real = i.getOpcode() == Opcode.INVOKE_VIRTUAL_QUICK ? Opcode.INVOKE_VIRTUAL : Opcode.INVOKE_SUPER;
    BuilderInstruction next = getNextOf(i);
    new InvokeTranslator(it)
        .translate(
            new BuilderInstruction35c(real, i.getRegisterCount(), i.getRegisterC(), i.getRegisterD(), i.getRegisterE(), i.getRegisterF(), i.getRegisterG(), new CustomMethodReference(
                "Ljava/lang/Object;", "$$$method_vtable_" + i.getVtableIndex() + "_argsize_" + (i.getRegisterCount() - 1), DexLibCommons.generateFakeQuickDesc(i.getRegisterCount(), next))),
            getNextOf(i));
  }
}
