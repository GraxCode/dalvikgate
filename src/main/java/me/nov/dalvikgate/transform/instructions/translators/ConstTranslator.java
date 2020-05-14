package me.nov.dalvikgate.transform.instructions.translators;

import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.instruction.*;
import org.jf.dexlib2.iface.reference.StringReference;
import org.objectweb.asm.tree.LdcInsnNode;

import me.nov.dalvikgate.transform.instructions.*;
import me.nov.dalvikgate.transform.instructions.exception.TranslationException;
import me.nov.dalvikgate.transform.instructions.unresolved.UnresolvedNumberInsn;

public class ConstTranslator extends AbstractInsnTranslator<BuilderInstruction> {

  public ConstTranslator(InstructionTransformer it) {
    super(it);
  }

  public void translate(BuilderInstruction i) {
    boolean wide = i.getOpcode().setsWideRegister();
    switch (i.getOpcode()) {
    case CONST_4:
      // Move the given literal value (sign-extended to 32 bits) into the specified register.
      // Value: signed int (4 bits)
      BuilderInstruction11n _11n = (BuilderInstruction11n) i;
      il.add(new UnresolvedNumberInsn(wide, _11n.getWideLiteral()));
      addLocalSet(_11n.getRegisterA(), wide);
      break;
    case CONST_WIDE_16:
      // Move the given literal value (sign-extended to 64 bits) into the specified register-pair.
      // Value: signed int (16 bits)
    case CONST_16:
      // Move the given literal value (sign-extended to 32 bits) into the specified register.
      // Value: signed int (16 bits)
      BuilderInstruction21s _21s = (BuilderInstruction21s) i;
      il.add(new UnresolvedNumberInsn(wide, _21s.getWideLiteral()));
      addLocalSet(_21s.getRegisterA(), wide);
      break;
    case CONST_WIDE_32:
      // Move the given literal value (sign-extended to 64 bits) into the specified register-pair.
      // Value: signed int (32 bits)
    case CONST:
      // Move the given literal value into the specified register.
      // Value: arbitrary 32-bit constant
      BuilderInstruction31i _31i = (BuilderInstruction31i) i;
      il.add(new UnresolvedNumberInsn(wide, _31i.getWideLiteral()));
      addLocalSet(_31i.getRegisterA(), wide);
      break;
    case CONST_WIDE:
      // Move the given literal value into the specified register-pair.
      // Value: arbitrary double-width (64-bit) constant
      BuilderInstruction51l _51l = (BuilderInstruction51l) i;
      il.add(new UnresolvedNumberInsn(wide, _51l.getWideLiteral()));
      addLocalSet(_51l.getRegisterA(), wide);
      break;
    case CONST_HIGH16:
      // Move the given literal value (right-zero-extended to 32 bits) into the specified register.
      // Value: signed int (16 bits)
      BuilderInstruction21ih _21ih = (BuilderInstruction21ih) i;
      il.add(new UnresolvedNumberInsn(wide, _21ih.getWideLiteral()));
      addLocalSet(_21ih.getRegisterA(), wide);
      break;
    case CONST_WIDE_HIGH16:
      // Move the given literal value (right-zero-extended to 64 bits) into the specified register-pair.
      // Value: signed int (16 bits)
      BuilderInstruction21lh _21lh = (BuilderInstruction21lh) i;
      il.add(new UnresolvedNumberInsn(wide, _21lh.getWideLiteral()));
      addLocalSet(_21lh.getRegisterA(), wide);
      break;
    case CONST_STRING_JUMBO:
      // I think we can all agree this has to be a string
      BuilderInstruction31c _31c = (BuilderInstruction31c) i;
      il.add(new LdcInsnNode(((StringReference) _31c.getReference()).getString()));
      addLocalSetObject(_31c.getRegisterA());
      break;
    default:
      throw new TranslationException("not a const opcode or wrong translator class: " + i.getOpcode());
    }
    // other non-number const variants in F21cTranslator
  }
}
