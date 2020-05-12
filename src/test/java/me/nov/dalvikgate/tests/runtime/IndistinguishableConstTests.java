package me.nov.dalvikgate.tests.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.*;
import org.jf.dexlib2.builder.instruction.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import me.nov.dalvikgate.tests.utils.Factory;

public class IndistinguishableConstTests {

  /**
   * Test case where type of register cannot be defined, either int 0 or Object null, doesn't matter.
   */
  @Test
  void withJump() {
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
