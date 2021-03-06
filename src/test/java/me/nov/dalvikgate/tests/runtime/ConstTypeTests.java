package me.nov.dalvikgate.tests.runtime;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.MethodImplementationBuilder;
import org.jf.dexlib2.builder.instruction.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;

import me.nov.dalvikgate.tests.utils.Factory;

class ConstTypeTests implements Opcodes {

  @Test
  void intConst32() {
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(1);
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 0, 1234));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));

    assertEquals(1234, Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.INT_TYPE), mmi)));
  }

  @Test
  void intConst32WithMath() {
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(5);
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 1, 100));
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 3, 50));
    mmi.addInstruction(new BuilderInstruction23x(Opcode.ADD_INT, 5, 3, 1));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 5));
    assertEquals(150, Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.INT_TYPE), mmi)));
  }

  @Test
  void intConst32WithMath2() {
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(3);
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 1, 100));
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 2, 50));
    mmi.addInstruction(new BuilderInstruction23x(Opcode.ADD_INT, 3, 2, 1));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 3));
    assertEquals(150, Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.INT_TYPE), mmi)));
  }

  @Test
  void floatConst32() {
    // unusual float construction
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(1);
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 0, Float.floatToIntBits(1.2f)));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
    assertEquals(1.2f, Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.FLOAT_TYPE), mmi)));
  }

  @Test
  void longConst64() {
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(2);
    mmi.addInstruction(new BuilderInstruction51l(Opcode.CONST_WIDE, 0, 423456L));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));

    assertEquals(423456L, Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.LONG_TYPE), mmi)));
  }

  @Test
  void doubleConst64() {
    // unusual double construction
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(2);
    mmi.addInstruction(new BuilderInstruction51l(Opcode.CONST_WIDE, 0, Double.doubleToLongBits(363412d)));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
    assertEquals(363412d, Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.DOUBLE_TYPE), mmi)));
  }

  @Test
  void floatConstHigh16() {
    int value = new BigInteger("100110100000000000000000000000", 2).intValue(); // only high 16 values allowed
    // normal float construction
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(1);
    mmi.addInstruction(new BuilderInstruction21ih(Opcode.CONST_HIGH16, 0, value));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));

    assertEquals(Float.intBitsToFloat(value), Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.FLOAT_TYPE), mmi)));
  }

  @Test
  void doubleConstWideHigh16() {
    long value = new BigInteger("10110101010000000000000000000000000000000000000000000000000000", 2).longValue(); // only high 16 values allowed
    // normal double construction
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(2);
    mmi.addInstruction(new BuilderInstruction21lh(Opcode.CONST_WIDE_HIGH16, 0, value));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
    assertEquals(Double.longBitsToDouble(value), Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.DOUBLE_TYPE), mmi)));
  }

  /**
   * Test case where type of register cannot be defined, either int 0 or Object null, doesn't matter.
   */
  @Test
  void schroedingersVariableType() {
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(2);
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 0, 0)); // either Object null or int 0
    mmi.addInstruction(new BuilderInstruction21t(Opcode.IF_EQZ, 0, mmi.getLabel("target")));
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 1, 1637));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 1));
    mmi.addLabel("target");
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 1, 832));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 1));
    assertEquals(832, Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.INT_TYPE), mmi)));
  }
}
