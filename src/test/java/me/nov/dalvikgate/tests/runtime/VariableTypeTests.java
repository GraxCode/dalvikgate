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
    Factory.saveDebug(Factory.runDexToASM(Type.getMethodType(Type.getType(Object.class)), mmi));
    assertEquals("test", Factory.executeMethodAtRuntime(Factory.runDexToASM(Type.getMethodType(Type.getType(Object.class)), mmi)));
  }

  /**
   * NOT WORKING
iget-object Format22c, 0, 1, Lcom/android/documentsui/DocumentsActivity;->mState:Lcom/android/documentsui/DocumentsActivity$State;
iget-object Format22c, 0, 0, Lcom/android/documentsui/DocumentsActivity$State;->stack:Lcom/android/documentsui/model/DocumentStack;
iget-object Format22c, 0, 0, Lcom/android/documentsui/model/DocumentStack;->root:Lcom/android/documentsui/model/RootInfo;
if-eqz Format21t, 0
iget-object Format22c, 0, 1, Lcom/android/documentsui/DocumentsActivity;->mState:Lcom/android/documentsui/DocumentsActivity$State;
iget-object Format22c, 0, 0, Lcom/android/documentsui/DocumentsActivity$State;->stack:Lcom/android/documentsui/model/DocumentStack;
iget-object Format22c, 0, 0, Lcom/android/documentsui/model/DocumentStack;->root:Lcom/android/documentsui/model/RootInfo;
return-object Format11x, 0
iget-object Format22c, 0, 1, Lcom/android/documentsui/DocumentsActivity;->mRoots:Lcom/android/documentsui/RootsCache;
invoke-virtual Format35c, 1, 0, 0, 0, 0, 0, Lcom/android/documentsui/RootsCache;->getRecentsRoot()Lcom/android/documentsui/model/RootInfo;
move-result-object Format11x, 0
goto Format10t
   */
}
