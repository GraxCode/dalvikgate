package me.nov.dalvikgate.tests.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;

import me.nov.dalvikgate.tests.utils.Factory;

class NumberTests implements Opcodes {

  @Test
  void intLdc() {
    MutableMethodImplementation mmi = new MutableMethodImplementation(1);
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 0, 1234));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));

    assertEquals(1235, Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.INT_TYPE), mmi)));
  }

  @Test
  void floatLdc() {
    MutableMethodImplementation mmi = new MutableMethodImplementation(1);
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 0, Float.floatToIntBits(1.2f)));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));

    assertEquals(1.2f, Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.FLOAT_TYPE), mmi)));
  }
}
