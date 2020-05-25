package me.nov.dalvikgate.transform.instructions.translators;

import static me.nov.dalvikgate.asm.ASMCommons.*;
import static org.objectweb.asm.Type.*;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.instruction.BuilderInstruction23x;
import org.objectweb.asm.tree.InsnNode;

import me.nov.dalvikgate.transform.instructions.*;
import me.nov.dalvikgate.transform.instructions.exception.UnsupportedInsnException;
import me.nov.dalvikgate.transform.instructions.unresolved.UnresolvedWideArrayInsn;

public class F23xTranslator extends AbstractInsnTranslator<BuilderInstruction23x> {

  public F23xTranslator(InstructionTransformer it) {
    super(it);
  }

  @Override
  public void translate(BuilderInstruction23x i) {
    switch (i.getOpcode()) {
    // Perform the indicated floating point or long comparison, setting a to 0 if b == c, 1 if b > c, or -1 if b < c.
    // The "bias" listed for the floating point operations indicates how NaN comparisons are treated: "gt bias" instructions return 1 for NaN comparisons,
    // and "lt bias" instructions return -1.
    //
    // For example, to check to see if floating point x < y it is advisable to use cmpg-float;
    // a result of -1 indicates that the test was true, and the other values indicate it was false
    // either due to a valid comparison or because one of the values was NaN.
    //
    // A: destination register (8 bits)
    // B: first source register or pair
    // C: second source register or pair
    case CMPL_FLOAT:
      addLocalGet(i.getRegisterB(), FLOAT_TYPE);
      addLocalGet(i.getRegisterC(), FLOAT_TYPE);
      il.add(new InsnNode(FCMPL));
      addLocalSet(i.getRegisterA(), INT_TYPE);
      return;
    case CMPG_FLOAT:
      addLocalGet(i.getRegisterB(), FLOAT_TYPE);
      addLocalGet(i.getRegisterC(), FLOAT_TYPE);
      il.add(new InsnNode(FCMPG));
      addLocalSet(i.getRegisterA(), INT_TYPE);
      return;
    case CMPL_DOUBLE:
      addLocalGet(i.getRegisterB(), DOUBLE_TYPE);
      addLocalGet(i.getRegisterC(), DOUBLE_TYPE);
      il.add(new InsnNode(DCMPL));
      addLocalSet(i.getRegisterA(), INT_TYPE);
      return;
    case CMPG_DOUBLE:
      addLocalGet(i.getRegisterB(), DOUBLE_TYPE);
      addLocalGet(i.getRegisterC(), DOUBLE_TYPE);
      il.add(new InsnNode(DCMPG));
      addLocalSet(i.getRegisterA(), INT_TYPE);
      return;
    case CMP_LONG:
      addLocalGet(i.getRegisterB(), LONG_TYPE);
      addLocalGet(i.getRegisterC(), LONG_TYPE);
      il.add(new InsnNode(LCMP));
      addLocalSet(i.getRegisterA(), INT_TYPE);
      return;
    // array loads and sets
    // A: value register or pair; may be source or dest (8 bits)
    // B: array register (8 bits)
    // C: index register (8 bits)
    case AGET:
    case AGET_BOOLEAN:
      addLocalGet(i.getRegisterB(), OBJECT_TYPE);
      addLocalGet(i.getRegisterC(), INT_TYPE);
      il.add(new InsnNode(IALOAD));
      addLocalSet(i.getRegisterA(), i.getOpcode() == Opcode.AGET_BOOLEAN ? BOOLEAN_TYPE : INT_TYPE);
      return;
    case AGET_WIDE:
      addLocalGet(i.getRegisterB(), OBJECT_TYPE);
      addLocalGet(i.getRegisterC(), INT_TYPE);
      il.add(new UnresolvedWideArrayInsn(false));
      addLocalSet(i.getRegisterA(), true); // unsure if long or double
      return;
    case AGET_OBJECT:
      addLocalGet(i.getRegisterB(), OBJECT_TYPE);
      addLocalGet(i.getRegisterC(), INT_TYPE);
      il.add(new InsnNode(AALOAD));
      addLocalSet(i.getRegisterA(), OBJECT_TYPE);
      return;
    case AGET_BYTE:
      addLocalGet(i.getRegisterB(), OBJECT_TYPE);
      addLocalGet(i.getRegisterC(), INT_TYPE);
      il.add(new InsnNode(BALOAD));
      addLocalSet(i.getRegisterA(), BYTE_TYPE);
      return;
    case AGET_CHAR:
      addLocalGet(i.getRegisterB(), OBJECT_TYPE);
      addLocalGet(i.getRegisterC(), INT_TYPE);
      il.add(new InsnNode(CALOAD));
      addLocalSet(i.getRegisterA(), CHAR_TYPE);
      return;
    case AGET_SHORT:
      addLocalGet(i.getRegisterB(), OBJECT_TYPE);
      addLocalGet(i.getRegisterC(), INT_TYPE);
      il.add(new InsnNode(SALOAD));
      addLocalSet(i.getRegisterA(), SHORT_TYPE);
      return;
    case APUT:
    case APUT_BOOLEAN:
      addLocalGet(i.getRegisterB(), OBJECT_TYPE);
      addLocalGet(i.getRegisterC(), INT_TYPE);
      addLocalGet(i.getRegisterA(), i.getOpcode() == Opcode.APUT_BOOLEAN ? BOOLEAN_TYPE : INT_TYPE);
      il.add(new InsnNode(IASTORE));
      return;
    case APUT_WIDE:
      addLocalGet(i.getRegisterB(), OBJECT_TYPE);
      addLocalGet(i.getRegisterC(), INT_TYPE);
      addLocalGet(i.getRegisterA(), true); // unsure if long or double
      il.add(new UnresolvedWideArrayInsn(true));
      return;
    case APUT_OBJECT:
      addLocalGet(i.getRegisterB(), OBJECT_TYPE);
      addLocalGet(i.getRegisterC(), INT_TYPE);
      addLocalGet(i.getRegisterA(), OBJECT_TYPE);
      il.add(new InsnNode(AASTORE));
      return;
    case APUT_BYTE:
      addLocalGet(i.getRegisterB(), OBJECT_TYPE);
      addLocalGet(i.getRegisterC(), INT_TYPE);
      addLocalGet(i.getRegisterA(), BYTE_TYPE);
      il.add(new InsnNode(BASTORE));
      return;
    case APUT_CHAR:
      addLocalGet(i.getRegisterB(), OBJECT_TYPE);
      addLocalGet(i.getRegisterC(), INT_TYPE);
      addLocalGet(i.getRegisterA(), CHAR_TYPE);
      il.add(new InsnNode(CASTORE));
      return;
    case APUT_SHORT:
      addLocalGet(i.getRegisterB(), OBJECT_TYPE);
      addLocalGet(i.getRegisterC(), INT_TYPE);
      addLocalGet(i.getRegisterA(), SHORT_TYPE);
      il.add(new InsnNode(SASTORE));
      return;
    // Perform the identified binary operation on the two source registers, storing the result in the destination register.
    // A: destination register or pair (8 bits)
    // B: first source register or pair (8 bits)
    // C: second source register or pair (8 bits)
    case ADD_INT:
    case SUB_INT:
    case MUL_INT:
    case DIV_INT:
    case REM_INT:
    case AND_INT:
    case OR_INT:
    case XOR_INT:
    case SHL_INT:
    case SHR_INT:
    case USHR_INT:
      addLocalGet(i.getRegisterB(), INT_TYPE);
      addLocalGet(i.getRegisterC(), INT_TYPE);
      break;
    case ADD_LONG:
    case SUB_LONG:
    case MUL_LONG:
    case DIV_LONG:
    case REM_LONG:
    case AND_LONG:
    case OR_LONG:
    case XOR_LONG:
      addLocalGet(i.getRegisterB(), LONG_TYPE);
      addLocalGet(i.getRegisterC(), LONG_TYPE);
      break;
    case SHL_LONG:
    case SHR_LONG:
    case USHR_LONG:
      addLocalGet(i.getRegisterB(), LONG_TYPE);
      addLocalGet(i.getRegisterC(), INT_TYPE); // special non-wide variable for shift instructions
      break;
    case ADD_FLOAT:
    case SUB_FLOAT:
    case MUL_FLOAT:
    case DIV_FLOAT:
    case REM_FLOAT:
      addLocalGet(i.getRegisterB(), FLOAT_TYPE);
      addLocalGet(i.getRegisterC(), FLOAT_TYPE);
      break;
    case ADD_DOUBLE:
    case SUB_DOUBLE:
    case MUL_DOUBLE:
    case DIV_DOUBLE:
    case REM_DOUBLE:
      addLocalGet(i.getRegisterB(), DOUBLE_TYPE);
      addLocalGet(i.getRegisterC(), DOUBLE_TYPE);
      break;
    default:
      throw new UnsupportedInsnException(i);
    }
    switch (i.getOpcode()) {
    case ADD_INT:
      il.add(new InsnNode(IADD));
      addLocalSet(i.getRegisterA(), INT_TYPE);
      break;
    case SUB_INT:
      il.add(new InsnNode(ISUB));
      addLocalSet(i.getRegisterA(), INT_TYPE);
      break;
    case MUL_INT:
      il.add(new InsnNode(IMUL));
      addLocalSet(i.getRegisterA(), INT_TYPE);
      break;
    case DIV_INT:
      il.add(new InsnNode(IDIV));
      addLocalSet(i.getRegisterA(), INT_TYPE);
      break;
    case REM_INT:
      il.add(new InsnNode(IREM));
      addLocalSet(i.getRegisterA(), INT_TYPE);
      break;
    case AND_INT:
      il.add(new InsnNode(IAND));
      addLocalSet(i.getRegisterA(), INT_TYPE);
      break;
    case OR_INT:
      il.add(new InsnNode(IOR));
      addLocalSet(i.getRegisterA(), INT_TYPE);
      break;
    case XOR_INT:
      il.add(new InsnNode(IXOR));
      addLocalSet(i.getRegisterA(), INT_TYPE);
      break;
    case SHL_INT:
      il.add(new InsnNode(ISHL));
      addLocalSet(i.getRegisterA(), INT_TYPE);
      break;
    case SHR_INT:
      il.add(new InsnNode(ISHR));
      addLocalSet(i.getRegisterA(), INT_TYPE);
      break;
    case USHR_INT:
      il.add(new InsnNode(IUSHR));
      addLocalSet(i.getRegisterA(), INT_TYPE);
      break;
    case ADD_LONG:
      il.add(new InsnNode(LADD));
      addLocalSet(i.getRegisterA(), LONG_TYPE);
      break;
    case SUB_LONG:
      il.add(new InsnNode(LSUB));
      addLocalSet(i.getRegisterA(), LONG_TYPE);
      break;
    case MUL_LONG:
      il.add(new InsnNode(LMUL));
      addLocalSet(i.getRegisterA(), LONG_TYPE);
      break;
    case DIV_LONG:
      il.add(new InsnNode(LDIV));
      addLocalSet(i.getRegisterA(), LONG_TYPE);
      break;
    case REM_LONG:
      il.add(new InsnNode(LREM));
      addLocalSet(i.getRegisterA(), LONG_TYPE);
      break;
    case AND_LONG:
      il.add(new InsnNode(LAND));
      addLocalSet(i.getRegisterA(), LONG_TYPE);
      break;
    case OR_LONG:
      il.add(new InsnNode(LOR));
      addLocalSet(i.getRegisterA(), LONG_TYPE);
      break;
    case XOR_LONG:
      il.add(new InsnNode(LXOR));
      addLocalSet(i.getRegisterA(), LONG_TYPE);
      break;
    case SHL_LONG:
      il.add(new InsnNode(LSHL));
      addLocalSet(i.getRegisterA(), LONG_TYPE);
      break;
    case SHR_LONG:
      il.add(new InsnNode(LSHR));
      addLocalSet(i.getRegisterA(), LONG_TYPE);
      break;
    case USHR_LONG:
      il.add(new InsnNode(LUSHR));
      addLocalSet(i.getRegisterA(), LONG_TYPE);
      break;
    case ADD_FLOAT:
      il.add(new InsnNode(FADD));
      addLocalSet(i.getRegisterA(), LONG_TYPE);
      break;
    case SUB_FLOAT:
      il.add(new InsnNode(FSUB));
      addLocalSet(i.getRegisterA(), LONG_TYPE);
      break;
    case MUL_FLOAT:
      il.add(new InsnNode(FMUL));
      addLocalSet(i.getRegisterA(), LONG_TYPE);
      break;
    case DIV_FLOAT:
      il.add(new InsnNode(FDIV));
      addLocalSet(i.getRegisterA(), LONG_TYPE);
      break;
    case REM_FLOAT:
      il.add(new InsnNode(FREM));
      addLocalSet(i.getRegisterA(), LONG_TYPE);
      break;
    case ADD_DOUBLE:
      il.add(new InsnNode(FADD));
      addLocalSet(i.getRegisterA(), DOUBLE_TYPE);
      break;
    case SUB_DOUBLE:
      il.add(new InsnNode(DSUB));
      addLocalSet(i.getRegisterA(), DOUBLE_TYPE);
      break;
    case MUL_DOUBLE:
      il.add(new InsnNode(DMUL));
      addLocalSet(i.getRegisterA(), DOUBLE_TYPE);
      break;
    case DIV_DOUBLE:
      il.add(new InsnNode(DDIV));
      addLocalSet(i.getRegisterA(), DOUBLE_TYPE);
      break;
    case REM_DOUBLE:
      il.add(new InsnNode(DREM));
      addLocalSet(i.getRegisterA(), DOUBLE_TYPE);
      break;
    default:
      throw new IllegalStateException();
    }
  }
}