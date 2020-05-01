package me.nov.dalvikgate.transform.instruction.translators;

import static me.nov.dalvikgate.asm.ASMCommons.ARRAY_TYPE;
import static org.objectweb.asm.Type.BYTE_TYPE;
import static org.objectweb.asm.Type.CHAR_TYPE;
import static org.objectweb.asm.Type.DOUBLE_TYPE;
import static org.objectweb.asm.Type.FLOAT_TYPE;
import static org.objectweb.asm.Type.INT_TYPE;
import static org.objectweb.asm.Type.LONG_TYPE;
import static org.objectweb.asm.Type.SHORT_TYPE;

import org.jf.dexlib2.builder.instruction.BuilderInstruction12x;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

import me.nov.dalvikgate.transform.instruction.IInsnTranslator;
import me.nov.dalvikgate.transform.instruction.InstructionTransformer;
import me.nov.dalvikgate.transform.instruction.exception.UnsupportedInsnException;

public class F12xTranslator extends IInsnTranslator<BuilderInstruction12x> {

  public F12xTranslator(InstructionTransformer it) {
    super(it);
  }

  @Override
  public void translate(BuilderInstruction12x i) {
    int source = i.getRegisterB();
    int destination = i.getRegisterA();
    switch (i.getOpcode()) {
    case MOVE:
      if (source == destination)
        return;
      addLocalGet(source, INT_TYPE);
      addLocalSet(destination, INT_TYPE);
      return;
    case MOVE_WIDE:
      if (source == destination)
        return;
      // Cannot determine if double or long, resolve later
      addLocalGet(source, null);
      addLocalSet(destination, null);
    case MOVE_OBJECT:
      if (source == destination)
        return;
      addLocalGetObject(source);
      addLocalSetObject(destination);
      return;
    case ARRAY_LENGTH:
      addLocalGet(source, ARRAY_TYPE);
      il.add(new InsnNode(ARRAYLENGTH));
      addLocalSet(destination, INT_TYPE);
      return;
    case NEG_INT:
      addLocalGet(source, INT_TYPE);
      il.add(new InsnNode(INEG));
      addLocalSet(destination, INT_TYPE);
      return;
    case NOT_INT:
      addLocalGet(source, INT_TYPE);
      il.add(new InsnNode(ICONST_M1));
      il.add(new InsnNode(IXOR));
      addLocalSet(destination, INT_TYPE);
      return;
    case NEG_LONG:
      addLocalGet(source, LONG_TYPE);
      il.add(new InsnNode(LNEG));
      addLocalSet(destination, LONG_TYPE);
      return;
    case NOT_LONG:
      addLocalGet(source, LONG_TYPE);
      il.add(new LdcInsnNode(-1L));
      il.add(new InsnNode(LXOR));
      addLocalSet(destination, LONG_TYPE);
      return;
    case NEG_FLOAT:
      addLocalGet(source, FLOAT_TYPE);
      il.add(new InsnNode(FNEG));
      addLocalSet(destination, FLOAT_TYPE);
      return;
    case NEG_DOUBLE:
      addLocalGet(source, DOUBLE_TYPE);
      il.add(new InsnNode(DNEG));
      addLocalSet(destination, DOUBLE_TYPE);
      return;
    case INT_TO_LONG:
      addLocalGet(source, INT_TYPE);
      il.add(new InsnNode(I2L));
      addLocalSet(destination, LONG_TYPE);
      return;
    case INT_TO_FLOAT:
      addLocalGet(source, INT_TYPE);
      il.add(new InsnNode(I2F));
      addLocalSet(destination, FLOAT_TYPE);
      return;
    case INT_TO_DOUBLE:
      addLocalGet(source, INT_TYPE);
      il.add(new InsnNode(I2D));
      addLocalSet(destination, DOUBLE_TYPE);
      return;
    case LONG_TO_INT:
      addLocalGet(source, LONG_TYPE);
      il.add(new InsnNode(L2I));
      addLocalSet(destination, INT_TYPE);
      return;
    case LONG_TO_FLOAT:
      addLocalGet(source, LONG_TYPE);
      il.add(new InsnNode(L2F));
      addLocalSet(destination, FLOAT_TYPE);
      return;
    case LONG_TO_DOUBLE:
      addLocalGet(source, LONG_TYPE);
      il.add(new InsnNode(L2D));
      addLocalSet(destination, DOUBLE_TYPE);
      return;
    case FLOAT_TO_INT:
      addLocalGet(source, FLOAT_TYPE);
      il.add(new InsnNode(F2I));
      addLocalSet(destination, INT_TYPE);
      return;
    case FLOAT_TO_LONG:
      addLocalGet(source, FLOAT_TYPE);
      il.add(new InsnNode(F2L));
      addLocalSet(destination, LONG_TYPE);
      return;
    case FLOAT_TO_DOUBLE:
      addLocalGet(source, FLOAT_TYPE);
      il.add(new InsnNode(F2D));
      addLocalSet(destination, DOUBLE_TYPE);
      return;
    case DOUBLE_TO_INT:
      addLocalGet(source, DOUBLE_TYPE);
      il.add(new InsnNode(D2I));
      addLocalSet(destination, INT_TYPE);
      return;
    case DOUBLE_TO_LONG:
      addLocalGet(source, DOUBLE_TYPE);
      il.add(new InsnNode(D2L));
      addLocalSet(destination, LONG_TYPE);
      return;
    case DOUBLE_TO_FLOAT:
      addLocalGet(source, DOUBLE_TYPE);
      il.add(new InsnNode(D2F));
      addLocalSet(destination, FLOAT_TYPE);
      return;
    case INT_TO_BYTE:
      addLocalGet(source, INT_TYPE);
      il.add(new InsnNode(I2B));
      addLocalSet(destination, BYTE_TYPE);
      return;
    case INT_TO_CHAR:
      addLocalGet(source, INT_TYPE);
      il.add(new InsnNode(I2C));
      addLocalSet(destination, CHAR_TYPE);
      return;
    case INT_TO_SHORT:
      addLocalGet(source, INT_TYPE);
      il.add(new InsnNode(I2S));
      addLocalSet(destination, SHORT_TYPE);
      return;
    case ADD_INT_2ADDR:
    case SUB_INT_2ADDR:
    case MUL_INT_2ADDR:
    case DIV_INT_2ADDR:
    case REM_INT_2ADDR:
    case AND_INT_2ADDR:
    case OR_INT_2ADDR:
    case XOR_INT_2ADDR:
    case SHL_INT_2ADDR:
    case SHR_INT_2ADDR:
    case USHR_INT_2ADDR:
      addLocalGet(destination, INT_TYPE);
      addLocalGet(source, INT_TYPE);
      break;
    case ADD_LONG_2ADDR:
    case SUB_LONG_2ADDR:
    case MUL_LONG_2ADDR:
    case DIV_LONG_2ADDR:
    case REM_LONG_2ADDR:
    case AND_LONG_2ADDR:
    case OR_LONG_2ADDR:
    case XOR_LONG_2ADDR:
    case SHL_LONG_2ADDR:
    case SHR_LONG_2ADDR:
    case USHR_LONG_2ADDR:
      addLocalGet(destination, LONG_TYPE);
      addLocalGet(source, LONG_TYPE);
      break;
    case ADD_FLOAT_2ADDR:
    case SUB_FLOAT_2ADDR:
    case MUL_FLOAT_2ADDR:
    case DIV_FLOAT_2ADDR:
    case REM_FLOAT_2ADDR:
      addLocalGet(destination, FLOAT_TYPE);
      addLocalGet(source, FLOAT_TYPE);
      break;
    case ADD_DOUBLE_2ADDR:
    case SUB_DOUBLE_2ADDR:
    case MUL_DOUBLE_2ADDR:
    case DIV_DOUBLE_2ADDR:
    case REM_DOUBLE_2ADDR:
      addLocalGet(destination, DOUBLE_TYPE);
      addLocalGet(source, FLOAT_TYPE);
      break;
    default:
      throw new UnsupportedInsnException(i);
    }
    // 2addr opcodes put the result back to first source register
    switch (i.getOpcode()) {
    case ADD_INT_2ADDR:
      il.add(new InsnNode(IADD));
      addLocalSet(destination, INT_TYPE);
      break;
    case SUB_INT_2ADDR:
      il.add(new InsnNode(ISUB));
      addLocalSet(destination, INT_TYPE);
      break;
    case MUL_INT_2ADDR:
      il.add(new InsnNode(IMUL));
      addLocalSet(destination, INT_TYPE);
      break;
    case DIV_INT_2ADDR:
      il.add(new InsnNode(IDIV));
      addLocalSet(destination, INT_TYPE);
      break;
    case REM_INT_2ADDR:
      il.add(new InsnNode(IREM));
      addLocalSet(destination, INT_TYPE);
      break;
    case AND_INT_2ADDR:
      il.add(new InsnNode(IAND));
      addLocalSet(destination, INT_TYPE);
      break;
    case OR_INT_2ADDR:
      il.add(new InsnNode(IOR));
      addLocalSet(destination, INT_TYPE);
      break;
    case XOR_INT_2ADDR:
      il.add(new InsnNode(IXOR));
      addLocalSet(destination, INT_TYPE);
      break;
    case SHL_INT_2ADDR:
      il.add(new InsnNode(ISHL));
      addLocalSet(destination, INT_TYPE);
      break;
    case SHR_INT_2ADDR:
      il.add(new InsnNode(ISHR));
      addLocalSet(destination, INT_TYPE);
      break;
    case USHR_INT_2ADDR:
      il.add(new InsnNode(IUSHR));
      addLocalSet(destination, INT_TYPE);
      break;
    case ADD_LONG_2ADDR:
      il.add(new InsnNode(LADD));
      addLocalSet(destination, LONG_TYPE);
      break;
    case SUB_LONG_2ADDR:
      il.add(new InsnNode(LSUB));
      addLocalSet(destination, LONG_TYPE);
      break;
    case MUL_LONG_2ADDR:
      il.add(new InsnNode(LMUL));
      addLocalSet(destination, LONG_TYPE);
      break;
    case DIV_LONG_2ADDR:
      il.add(new InsnNode(LDIV));
      addLocalSet(destination, LONG_TYPE);
      break;
    case REM_LONG_2ADDR:
      il.add(new InsnNode(LREM));
      addLocalSet(destination, LONG_TYPE);
      break;
    case AND_LONG_2ADDR:
      il.add(new InsnNode(LAND));
      addLocalSet(destination, LONG_TYPE);
      break;
    case OR_LONG_2ADDR:
      il.add(new InsnNode(LOR));
      addLocalSet(destination, LONG_TYPE);
      break;
    case XOR_LONG_2ADDR:
      il.add(new InsnNode(LXOR));
      addLocalSet(destination, LONG_TYPE);
      break;
    case SHL_LONG_2ADDR:
      il.add(new InsnNode(LSHL));
      addLocalSet(destination, LONG_TYPE);
      break;
    case SHR_LONG_2ADDR:
      il.add(new InsnNode(LSHR));
      addLocalSet(destination, LONG_TYPE);
      break;
    case USHR_LONG_2ADDR:
      il.add(new InsnNode(LUSHR));
      addLocalSet(destination, LONG_TYPE);
      break;
    case ADD_FLOAT_2ADDR:
      il.add(new InsnNode(FADD));
      addLocalSet(destination, FLOAT_TYPE);
      break;
    case SUB_FLOAT_2ADDR:
      il.add(new InsnNode(FSUB));
      addLocalSet(destination, FLOAT_TYPE);
      break;
    case MUL_FLOAT_2ADDR:
      il.add(new InsnNode(FMUL));
      addLocalSet(destination, FLOAT_TYPE);
      break;
    case DIV_FLOAT_2ADDR:
      il.add(new InsnNode(FDIV));
      addLocalSet(destination, FLOAT_TYPE);
      break;
    case REM_FLOAT_2ADDR:
      il.add(new InsnNode(FREM));
      addLocalSet(destination, FLOAT_TYPE);
      break;
    case ADD_DOUBLE_2ADDR:
      il.add(new InsnNode(DADD));
      addLocalSet(destination, DOUBLE_TYPE);
      break;
    case SUB_DOUBLE_2ADDR:
      il.add(new InsnNode(DSUB));
      addLocalSet(destination, DOUBLE_TYPE);
      break;
    case MUL_DOUBLE_2ADDR:
      il.add(new InsnNode(DMUL));
      addLocalSet(destination, DOUBLE_TYPE);
      break;
    case DIV_DOUBLE_2ADDR:
      il.add(new InsnNode(DDIV));
      addLocalSet(destination, DOUBLE_TYPE);
      break;
    case REM_DOUBLE_2ADDR:
      il.add(new InsnNode(DREM));
      addLocalSet(destination, DOUBLE_TYPE);
      break;
    default:
      throw new IllegalStateException();
    }
  }
}