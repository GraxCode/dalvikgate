package me.nov.dalvikgate.tests.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.MethodImplementationBuilder;
import org.jf.dexlib2.builder.instruction.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import me.nov.dalvikgate.tests.utils.Factory;
import me.nov.dalvikgate.utils.*;

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

  @Test
  void varRangeTest() {
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(1);
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 0, 5)); // const 5
    mmi.addInstruction(new BuilderInstruction21t(Opcode.IF_EQZ, 0, mmi.getLabel("target"))); // if 5 == 0
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 0, 0)); // const null
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
    mmi.addLabel("target");
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 0, 0)); // const null
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
    assertEquals(null, Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.getType(Object.class)), mmi)));
  }

  public static String test = "test";

  @Test
  void unlinkedLoadTest() {
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(1);
    mmi.addInstruction(new BuilderInstruction21c(Opcode.SGET_OBJECT, 0, new CustomFieldReference(Type.getType(VariableTypeTests.class).getDescriptor(), "test", "Ljava/lang/String;")));
    mmi.addInstruction(new BuilderInstruction21t(Opcode.IF_EQZ, 0, mmi.getLabel("target")));
    mmi.addLabel("return");
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
    mmi.addLabel("target");
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 0, 0));
    mmi.addInstruction(new BuilderInstruction30t(Opcode.GOTO_32, mmi.getLabel("return")));

    assertEquals("test", Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.getType(Object.class)), mmi)));
  }

  @Test
  void uglyFloatDoubleCalculation() {
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(6);
    mmi.addInstruction(new BuilderInstruction21ih(Opcode.CONST_HIGH16, 0, 1056964608));
    mmi.addInstruction(new BuilderInstruction12x(Opcode.SUB_FLOAT_2ADDR, 5, 0));
    mmi.addInstruction(new BuilderInstruction12x(Opcode.FLOAT_TO_DOUBLE, 0, 5));

    mmi.addInstruction(new BuilderInstruction51l(Opcode.CONST_WIDE, 2, 4602160705557665991L));
    mmi.addInstruction(new BuilderInstruction12x(Opcode.MUL_DOUBLE_2ADDR, 0, 2));
    mmi.addInstruction(new BuilderInstruction12x(Opcode.DOUBLE_TO_FLOAT, 5, 0));
    mmi.addInstruction(new BuilderInstruction12x(Opcode.FLOAT_TO_DOUBLE, 0, 5));

    mmi.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_STATIC, 2, 0, 1, 0, 0, 0, new CustomMethodReference("Ljava/lang/Math;", "sin", "(D)D")));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.MOVE_RESULT_WIDE, 0));
    mmi.addInstruction(new BuilderInstruction12x(Opcode.DOUBLE_TO_FLOAT, 0, 0));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));

    assertEquals(0.8526401f, Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.FLOAT_TYPE, Type.FLOAT_TYPE), mmi), 5f));
  }

  public boolean bool = false;
  public static VariableTypeTests instance = new VariableTypeTests();

  @Test
  void booleanVarStore() {
    String vtt = Type.getType(VariableTypeTests.class).getDescriptor();
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(2);
    mmi.addInstruction(new BuilderInstruction21c(Opcode.SGET_OBJECT, 0, new CustomFieldReference(vtt, "instance", vtt)));
    mmi.addInstruction(new BuilderInstruction11n(Opcode.CONST_4, 1, 1));
    mmi.addInstruction(new BuilderInstruction22c(Opcode.IPUT_BOOLEAN, 1, 0, new CustomFieldReference(vtt, "bool", "Z")));
    mmi.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));
    Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.VOID_TYPE), mmi));
    assertEquals(true, instance.bool);
  }
}
