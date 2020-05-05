package me.nov.dalvikgate.transform.instruction.translators;

import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.instruction.*;
import org.jf.dexlib2.iface.reference.StringReference;
import org.objectweb.asm.tree.LdcInsnNode;

import me.nov.dalvikgate.transform.instruction.*;
import me.nov.dalvikgate.transform.instruction.exception.TranslationException;
import me.nov.dalvikgate.transform.instruction.tree.UnresolvedNumberInsn;

public class ConstTranslator extends AbstractInsnTranslator<BuilderInstruction> {

  public ConstTranslator(InstructionTransformer it) {
    super(it);
  }

  /*
   * OLD CODE, not fully supporting floats etc
   * 
   * // TODO 0 ints can be the same as ACONST_NULL too public void translate(BuilderInstruction i) { // TODO problem: CONST may also initialize float. it is unknown which of them
   * switch (i.getOpcode()) { case CONST: BuilderInstruction31i _31i = (BuilderInstruction31i) i; int intValue = _31i.getNarrowLiteral(); il.add(makeIntPush(intValue));
   * addLocalSet(_31i.getRegisterA(), intValue); return; case CONST_WIDE: // const wide, used for long BuilderInstruction51l _51l = (BuilderInstruction51l) i; long longValue =
   * _51l.getWideLiteral(); il.add(new LdcInsnNode(longValue)); addLocalSet(_51l.getRegisterA(), longValue); return; case CONST_4: BuilderInstruction11n _11n =
   * (BuilderInstruction11n) i; int intValue4 = _11n.getNarrowLiteral(); il.add(makeIntPush(intValue4)); addLocalSet(_11n.getRegisterA(), intValue4); return; case CONST_16:
   * BuilderInstruction21s _21s = (BuilderInstruction21s) i; int intValue16 = _21s.getNarrowLiteral(); il.add(makeIntPush(intValue16)); addLocalSet(_21s.getRegisterA(), intValue16);
   * return; case CONST_WIDE_16: BuilderInstruction21s __21s = (BuilderInstruction21s) i; long longValue16 = __21s.getWideLiteral(); il.add(makeLongPush(longValue16));
   * addLocalSet(__21s.getRegisterA(), longValue16); return; case CONST_WIDE_32: BuilderInstruction31i __31i = (BuilderInstruction31i) i; long longValue32 = __31i.getWideLiteral();
   * il.add(makeLongPush(longValue32)); addLocalSet(__31i.getRegisterA(), longValue32); return; case CONST_HIGH16: // used for float initialization BuilderInstruction21ih _21ih =
   * (BuilderInstruction21ih) i; float floatValue = Float.intBitsToFloat(_21ih.getNarrowLiteral()); il.add(new LdcInsnNode(floatValue)); addLocalSet(_21ih.getRegisterA(),
   * FLOAT_TYPE); return; case CONST_WIDE_HIGH16: // used for double initialization BuilderInstruction21lh _21lh = (BuilderInstruction21lh) i; Double doubleValue =
   * Double.longBitsToDouble(_21lh.getWideLiteral()); il.add(new LdcInsnNode(doubleValue)); addLocalSet(_21lh.getRegisterA(), DOUBLE_TYPE); return; case CONST_STRING_JUMBO:
   * BuilderInstruction31c _31c = (BuilderInstruction31c) i; il.add(new LdcInsnNode(((StringReference) _31c.getReference()).getString())); addLocalSetObject(_31c.getRegisterA());
   * return; default: throw new TranslationException("not a const opcode or wrong translator class: " + i.getOpcode()); } // other const types in F21cTranslator }
   */

  public void translate(BuilderInstruction i) {
    switch (i.getOpcode()) {
    case CONST:
    case CONST_WIDE_32:
      // used for everything
      BuilderInstruction31i _31i = (BuilderInstruction31i) i;
      il.add(new UnresolvedNumberInsn(_31i.getWideLiteral()));
      addLocalSet(_31i.getRegisterA(), null);
      break;
    case CONST_WIDE:
      // const wide, used for long, but can be used for 0 double too
      BuilderInstruction51l _51l = (BuilderInstruction51l) i;
      il.add(new UnresolvedNumberInsn(_51l.getWideLiteral()));
      addLocalSet(_51l.getRegisterA(), null);
      break;
    case CONST_4:
      // const 4 is so small, as double or float require 32 bit minimum it can only be an int or long value
      BuilderInstruction11n _11n = (BuilderInstruction11n) i;
      il.add(new UnresolvedNumberInsn(_11n.getWideLiteral()));
      addLocalSet(_11n.getRegisterA(), null);
      break;
    case CONST_16:
    case CONST_WIDE_16:
      // as double or float require 32 bit minimum it can only be an int or long value
      BuilderInstruction21s _21s = (BuilderInstruction21s) i;
      il.add(new UnresolvedNumberInsn(_21s.getWideLiteral()));
      addLocalSet(_21s.getRegisterA(), null);
      break;
    case CONST_HIGH16:
      // normally used for float initialization, but can be something else too, trust nobody
      BuilderInstruction21ih _21ih = (BuilderInstruction21ih) i;
      il.add(new UnresolvedNumberInsn(_21ih.getWideLiteral()));
      addLocalSet(_21ih.getRegisterA(), null);
      break;
    case CONST_WIDE_HIGH16:
      // normally used for double initialization, but can be long too, trust nobody
      BuilderInstruction21lh _21lh = (BuilderInstruction21lh) i;
      il.add(new UnresolvedNumberInsn(_21lh.getWideLiteral()));
      addLocalSet(_21lh.getRegisterA(), null);
      break;
    case CONST_STRING_JUMBO:
      // i think we can all agree this has to be a string
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
