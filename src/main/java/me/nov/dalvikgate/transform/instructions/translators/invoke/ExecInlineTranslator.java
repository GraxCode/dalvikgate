package me.nov.dalvikgate.transform.instructions.translators.invoke;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.instruction.*;

import me.nov.dalvikgate.transform.instructions.*;
import me.nov.dalvikgate.transform.instructions.exception.TranslationException;
import me.nov.dalvikgate.utils.CustomMethodReference;

public class ExecInlineTranslator extends AbstractInsnTranslator<BuilderInstruction35mi> {

  public ExecInlineTranslator(InstructionTransformer it) {
    super(it);
  }

  /* @formatter:off */
  /**
   * Table of methods.
   *
   * The DEX optimizer uses the class/method/signature string fields to decide which calls it can trample. The interpreter just uses the function pointer field.
   * (Source: https://android.googlesource.com/platform/dalvik/+/kitkat-release/vm/InlineNative.cpp)
   * Can't find InlineNative.cpp for newer versions.
   **/
  public static final String gDvmInlineOpsTable[][] = {
    { "Lorg/apache/harmony/dalvik/NativeTestTarget;", "emptyInlineMethod", "()V" },
    
    { "Ljava/lang/String;", "charAt", "(I)C" },
    { "Ljava/lang/String;", "compareTo", "(Ljava/lang/String;)I" },
    { "Ljava/lang/String;", "equals", "(Ljava/lang/Object;)Z" },
    { "Ljava/lang/String;", "fastIndexOf", "(II)I" },
    { "Ljava/lang/String;", "isEmpty", "()Z" },
    { "Ljava/lang/String;", "length", "()I" },
    
    { "Ljava/lang/Math;", "abs", "(I)I" },
    { "Ljava/lang/Math;", "abs", "(J)J" },
    { "Ljava/lang/Math;", "abs", "(F)F" },
    { "Ljava/lang/Math;", "abs", "(D)D" },
    { "Ljava/lang/Math;", "min", "(II)I" },
    { "Ljava/lang/Math;", "max", "(II)I" },
    { "Ljava/lang/Math;", "sqrt", "(D)D" },
    { "Ljava/lang/Math;", "cos", "(D)D" },
    { "Ljava/lang/Math;", "sin", "(D)D" },
    
    { "Ljava/lang/Float;", "floatToIntBits", "(F)I" },
    { "Ljava/lang/Float;", "floatToRawIntBits", "(F)I" },
    { "Ljava/lang/Float;", "intBitsToFloat", "(I)F" },
    { "Ljava/lang/Double;", "doubleToLongBits", "(D)J" },
    { "Ljava/lang/Double;", "doubleToRawLongBits", "(D)J" },
    { "Ljava/lang/Double;", "longBitsToDouble", "(J)D" },
    // These are implemented exactly the same in Math and StrictMath,
    // so we can make the StrictMath calls fast too. Note that this
    // isn't true in general!
    { "Ljava/lang/StrictMath;", "abs", "(I)I" },
    { "Ljava/lang/StrictMath;", "abs", "(J)J" },
    { "Ljava/lang/StrictMath;", "abs", "(F)F" },
    { "Ljava/lang/StrictMath;", "abs", "(D)D" },
    { "Ljava/lang/StrictMath;", "min", "(II)I" },
    { "Ljava/lang/StrictMath;", "max", "(II)I" },
    { "Ljava/lang/StrictMath;", "sqrt", "(D)D" },
  };
  /* @formatter:on */

  @Override
  public void translate(BuilderInstruction35mi i) {
    // TODO UNTESTED, don't know how to generate samples.
    int index = i.getInlineIndex();
    if (index == 0) {
      // ignore emptyInline test
      return;
    }
    if (index >= gDvmInlineOpsTable.length) {
      throw new TranslationException("This ODEX contains execute-inline instructions later than android kit-kat. gDvmInlineOpsTable does not contain index " + index);
    }
    String[] method = gDvmInlineOpsTable[index];
    String desc = method[2];
    // use normal method translator with fake 35c instruction
    new InvokeTranslator(it).translate(new BuilderInstruction35c(Opcode.INVOKE_VIRTUAL, i.getRegisterCount(), i.getRegisterC(), i.getRegisterD(), i.getRegisterE(), i.getRegisterF(), i.getRegisterG(),
        new CustomMethodReference(method[0], method[1], desc)), getNextOf(i));
  }
}
