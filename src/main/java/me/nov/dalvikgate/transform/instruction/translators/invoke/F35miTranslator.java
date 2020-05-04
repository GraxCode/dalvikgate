package me.nov.dalvikgate.transform.instruction.translators.invoke;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.instruction.*;

import me.nov.dalvikgate.transform.instruction.*;
import me.nov.dalvikgate.utils.CustomMethodReference;

public class F35miTranslator extends AbstractInsnTranslator<BuilderInstruction35mi> {

  public F35miTranslator(InstructionTransformer it) {
    super(it);
  }

  /**
   * Table of methods.
   *
   * The DEX optimizer uses the class/method/signature string fields to decide which calls it can trample. The interpreter just uses the function pointer field.
   *
   * From: https://android.googlesource.com/platform/dalvik/+/donut-release/vm/InlineNative.c
   **/
  public static final String gDvmInlineOpsTable[][] = { { "Lorg/apache/harmony/dalvik/NativeTestTarget;", "emptyInlineMethod", "()V" }, { "Ljava/lang/String;", "charAt", "(I)C" },
      { "Ljava/lang/String;", "compareTo", "(Ljava/lang/String;)I" }, { "Ljava/lang/String;", "equals", "(Ljava/lang/Object;)Z" }, { "Ljava/lang/String;", "length", "()I" } };

  @Override
  public void translate(BuilderInstruction35mi i) {
    // TODO UNTESTED, don't know how to generate samples.
    int index = i.getInlineIndex();
    if (index == 0) {
      // ignore emptyInline test
      return;
    }
    String[] method = gDvmInlineOpsTable[index];
    String desc = method[2];
    // use normal method translator with fake 35c instruction
    new F35cTranslator(it).translate(new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL, i.getRegisterCount(), i.getRegisterC(), i.getRegisterD(), i.getRegisterE(), i.getRegisterF(), i.getRegisterG(),
        new CustomMethodReference(method[0], method[1], desc)), getNextOf(i));
  }
}
