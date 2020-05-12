package me.nov.dalvikgate.tests.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.*;
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
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(1);
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 0, 100));
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 1, 50));
    mmi.addInstruction(new BuilderInstruction23x(Opcode.ADD_INT, 0, 1, 2));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 2));

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
    // normal float construction
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(1);
    mmi.addInstruction(new BuilderInstruction21ih(Opcode.CONST_HIGH16, 0, Float.floatToIntBits(21.3f)));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));

    assertEquals(21.3f, Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.FLOAT_TYPE), mmi)));
  }

  @Test
  void doubleConstWideHigh16() {
    // normal double construction
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(2);
    mmi.addInstruction(new BuilderInstruction21lh(Opcode.CONST_WIDE_HIGH16, 0, Double.doubleToLongBits(363417d)));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
    assertEquals(363417d, Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.DOUBLE_TYPE), mmi)));
  }
}
