package me.nov.dalvikgate.transform.instructions.translators;

import static me.nov.dalvikgate.asm.ASMCommons.*;

import org.jf.dexlib2.builder.instruction.BuilderInstruction11x;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnNode;

import me.nov.dalvikgate.transform.instructions.*;
import me.nov.dalvikgate.transform.instructions.exception.UnsupportedInsnException;


public class F11xTranslator extends AbstractInsnTranslator<BuilderInstruction11x> {

  public F11xTranslator(InstructionTransformer it) {
    super(it);
  }

  @Override
  public void translate(BuilderInstruction11x i) {
    // A: destination or source register
    int register = i.getRegisterA();
    switch (i.getOpcode()) {
    case MOVE_RESULT:
      // "Move the single-word non-object result of..."
      // - So this should be a primitive
      addLocalSet(register, getPushedTypeForInsn(getRealLast(il))); // this can be tricked by jumps or similar
      // addLocalSet(register, null); //let the analyzer do the work
      break;
    case MOVE_EXCEPTION:
    case MOVE_RESULT_OBJECT:
      addLocalSetObject(register);
      break;
    case MOVE_RESULT_WIDE:
      // Get type from last written instruction
      addLocalSet(register, getPushedTypeForInsn(getRealLast(il))); // this can be tricked by jumps or similar
      // addLocalSet(register, null); //let the analyzer do the work
      break;
    case MONITOR_ENTER:
      addLocalGetObject(register);
      il.add(new InsnNode(MONITORENTER));
      break;
    case MONITOR_EXIT:
      addLocalGetObject(register);
      il.add(new InsnNode(MONITOREXIT));
      break;
    case RETURN:
    case RETURN_WIDE:
      // handle return instructions using known method return type, ugly but works
      Type returnType = Type.getReturnType(it.mn.desc);
      addLocalGet(register, returnType);
      il.add(new InsnNode(returnType.getOpcode(IRETURN)));
      break;
    case RETURN_OBJECT:
      // Dalvik has this object-specific return
      addLocalGetObject(register);
      il.add(new InsnNode(ARETURN));
      break;
    case THROW:
      addLocalGetObject(register);
      il.add(new InsnNode(ATHROW));
      break;
    default:
      throw new UnsupportedInsnException(i);
    }
  }

}
