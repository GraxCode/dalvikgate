package me.nov.dalvikgate.tests.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.MethodImplementationBuilder;
import org.jf.dexlib2.builder.instruction.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import me.nov.dalvikgate.tests.utils.Factory;
import me.nov.dalvikgate.utils.CustomMethodReference;

public class VariableTypeTests {
  @Test
  void floatToInt() {
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(1);
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 1, Float.floatToIntBits(14.32f)));
    mmi.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1, 1, 0, 0, 0, 0, new CustomMethodReference(Type.getType(VariableTypeTests.class).getDescriptor(), "returnFloatAsInt", "(F)I")));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT, 0));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
    assertEquals(14, Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.INT_TYPE), mmi)));
  }

  public static int returnFloatAsInt(float l) {
    return (int) l;
  }

  @Test
  void longToInt() {
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(1);
    mmi.addInstruction(new BuilderInstruction51l(Opcode.CONST_WIDE, 1, 1266777));
    mmi.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1, 1, 0, 0, 0, 0, new CustomMethodReference(Type.getType(VariableTypeTests.class).getDescriptor(), "returnLongAsInt", "(J)I")));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT, 0));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
    assertEquals(1266777, Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.INT_TYPE), mmi)));
  }

  public static int returnLongAsInt(long l) {
    return (int) l;
  }

  @Test
  void doubleToInt() {
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(1);
    mmi.addInstruction(new BuilderInstruction51l(Opcode.CONST_WIDE, 1, Double.doubleToLongBits(64728.497d)));
    mmi.addInstruction(
        new BuilderInstruction35c(Opcode.INVOKE_STATIC, 1, 1, 0, 0, 0, 0, new CustomMethodReference(Type.getType(VariableTypeTests.class).getDescriptor(), "returnDoubleAsInt", "(D)I")));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT, 0));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
    assertEquals(64728, Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.INT_TYPE), mmi)));
  }

  public static int returnDoubleAsInt(double l) {
    return (int) l;
  }
}
