package me.nov.dalvikgate.transform.instruction.translators;

import static me.nov.dalvikgate.asm.ASMCommons.*;
import static org.objectweb.asm.Type.*;

import org.jf.dexlib2.builder.instruction.BuilderInstruction11x;
import org.objectweb.asm.tree.InsnNode;

import me.nov.dalvikgate.transform.instruction.*;
import me.nov.dalvikgate.transform.instruction.exception.UnsupportedInsnException;

public class F11xTranslator extends AbstractInsnTranslator<BuilderInstruction11x> {

  public F11xTranslator(InstructionTransformer it) {
    super(it);
  }

  @Override
  public void translate(BuilderInstruction11x i) {
    // Move immediate word value into register.
    // A: destination register
    int source = i.getRegisterA();
    switch (i.getOpcode()) {
    case MOVE_RESULT:
      // "Move the single-word non-object result of..."
      // - So this should be a primitive
      addLocalSet(source, getPushedTypeForInsn(getRealLast(il))); // TODO  think this can be tricked by goto
      break;
    case MOVE_EXCEPTION:
    case MOVE_RESULT_OBJECT:
      addLocalSetObject(source);
      break;
    case MOVE_RESULT_WIDE:
      // Get type from last written instruction
      addLocalSet(source, getPushedTypeForInsn(getRealLast(il))); // TODO i think this can be tricked by goto
      break;
    case MONITOR_ENTER:
      addLocalGetObject(source);
      il.add(new InsnNode(MONITORENTER));
      break;
    case MONITOR_EXIT:
      addLocalGetObject(source);
      il.add(new InsnNode(MONITOREXIT));
      break;
    case RETURN:
      // Dalvik has this int-specific return
      addLocalGet(source, INT_TYPE);
      il.add(new InsnNode(IRETURN));
      break;
    case RETURN_WIDE:
      // Dalvik has this long/double-specific return
      // Cannot determine if double or long, resolve later
      addLocalGet(source, null);
      il.add(new InsnNode(LRETURN));
      break;
    case RETURN_OBJECT:
      // Dalvik has this object-specific return
      addLocalGetObject(source);
      il.add(new InsnNode(ARETURN));
      break;
    case THROW:
      addLocalGetObject(source);
      il.add(new InsnNode(ATHROW));
      break;
    default:
      throw new UnsupportedInsnException(i);
    }
  }

}
