package me.nov.dalvikgate.tests.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.MethodImplementationBuilder;
import org.jf.dexlib2.builder.instruction.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import me.nov.dalvikgate.tests.utils.Factory;

public class ParameterTests {
  @Test
  void parameterIsObjectTest() {
    MethodImplementationBuilder mmi = new MethodImplementationBuilder(2);
    mmi.addInstruction(new BuilderInstruction21t(Opcode.IF_EQZ, 1, mmi.getLabel("target"))); // ifnull arg0
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 0, 30));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
    mmi.addLabel("target");
    mmi.addInstruction(new BuilderInstruction31i(Opcode.CONST, 0, 25));
    mmi.addInstruction(new BuilderInstruction11x(Opcode.RETURN, 0));
    
    assertEquals(30, Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.getType(int.class), Type.getType(Object.class)), mmi), new Object()));
  }

}
