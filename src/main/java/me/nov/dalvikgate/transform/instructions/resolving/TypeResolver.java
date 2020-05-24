package me.nov.dalvikgate.transform.instructions.resolving;

import java.util.List;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import me.nov.dalvikgate.transform.instructions.IUnresolvedInstruction;
import me.nov.dalvikgate.transform.instructions.unresolved.*;

public class TypeResolver extends SourceInterpreter {
  private static final Type OBJECT_TYPE = Type.getType("Ljava/lang/Object;");
  private boolean aggressive;

  public TypeResolver(boolean aggs) {
    super(ASM8);
    this.aggressive = aggs;
  }

  @Override
  public SourceValue binaryOperation(AbstractInsnNode insn, SourceValue value1, SourceValue value2) {
    if (insn instanceof UnresolvedJumpInsn && !((IUnresolvedInstruction) insn).isResolved()) {
      if (!isUnresolved(value1))
        ((UnresolvedJumpInsn) insn).setType(getPushType(getTop(value1)));
      else if (!isUnresolved(value2))
        ((UnresolvedJumpInsn) insn).setType(getPushType(getTop(value2)));

      if (aggressive) {
        // type of jump or variable does not matter. set it to int.
        if (isUnresolved(value1)) {
          getTopUnresolved(value1).setType(Type.INT_TYPE);
        }
        if (isUnresolved(value2)) {
          getTopUnresolved(value2).setType(Type.INT_TYPE);
        }
        ((IUnresolvedInstruction) insn).setType(Type.INT_TYPE);
      }
    }
    if (insn instanceof UnresolvedWideArrayInsn && !((IUnresolvedInstruction) insn).isResolved()) {
      throw new IllegalStateException("TODO"); // TODO
    } else {
      if (isUnresolved(value1)) {
        IUnresolvedInstruction iui = getTopUnresolved(value1);
        iui.setType(getBinaryArgType(insn, false));
      }
      if (isUnresolved(value2)) {
        IUnresolvedInstruction iui = getTopUnresolved(value2);
        iui.setType(getBinaryArgType(insn, true));
      }
    }
    return super.binaryOperation(insn, value1, value2);
  }

  /**
   * @param insn
   * @param top  true if is value2
   * @return
   */
  public Type getBinaryArgType(AbstractInsnNode insn, boolean top) {
    switch (insn.getOpcode()) {
    case IALOAD:
    case BALOAD:
    case CALOAD:
    case SALOAD:
    case IADD:
    case ISUB:
    case IMUL:
    case IDIV:
    case IREM:
    case ISHL:
    case ISHR:
    case IUSHR:
    case IAND:
    case IOR:
    case IXOR:
    case IF_ICMPEQ:
    case IF_ICMPNE:
    case IF_ICMPLT:
    case IF_ICMPGE:
    case IF_ICMPGT:
    case IF_ICMPLE:
      return Type.INT_TYPE;
    case FALOAD:
    case FADD:
    case FSUB:
    case FMUL:
    case FDIV:
    case FREM:
    case FCMPL:
    case FCMPG:
      return Type.FLOAT_TYPE;
    case LALOAD:
    case LADD:
    case LSUB:
    case LMUL:
    case LDIV:
    case LREM:
    case LOR:
    case LXOR:
    case LAND:
    case LCMP:
      return Type.LONG_TYPE;
    case LSHL:
    case LSHR:
    case LUSHR:
      if (top) {
        return Type.INT_TYPE;
      }
      return Type.LONG_TYPE;
    case DALOAD:
    case DADD:
    case DSUB:
    case DMUL:
    case DDIV:
    case DREM:
      return Type.LONG_TYPE;
    case AALOAD:
      if (top) {
        return Type.INT_TYPE;
      }
      return OBJECT_TYPE;
    case IF_ACMPEQ:
    case IF_ACMPNE:
    case PUTFIELD:
      return OBJECT_TYPE;
    case DCMPL:
    case DCMPG:
      return Type.DOUBLE_TYPE;
    default:
      throw new AssertionError();
    }
  }

  @Override
  public SourceValue copyOperation(AbstractInsnNode insn, SourceValue value) {
    if (insn.getOpcode() >= ISTORE) {
      // don't fix loads with value
      if (insn instanceof UnresolvedVarInsn && !((IUnresolvedInstruction) insn).isResolved()) {
        if (!isUnresolved(value))
          ((UnresolvedVarInsn) insn).setType(getPushType(getTop(value)));
      } else if (isUnresolved(value)) {
        IUnresolvedInstruction iui = getTopUnresolved(value);
        switch (insn.getOpcode()) {
        case ISTORE:
          iui.setType(Type.INT_TYPE);
          break;
        case LSTORE:
          iui.setType(Type.LONG_TYPE);
          break;
        case FSTORE:
          iui.setType(Type.FLOAT_TYPE);
          break;
        case DSTORE:
          iui.setType(Type.DOUBLE_TYPE);
          break;
        case ASTORE:
          iui.setType(OBJECT_TYPE);
          break;
        }
      }
    }
    return super.copyOperation(insn, value);
  }

  @Override
  public SourceValue naryOperation(AbstractInsnNode insn, List<? extends SourceValue> values) {
    if (insn.getOpcode() == MULTIANEWARRAY) {
      values.stream().filter(v -> isUnresolved(v)).forEach(v -> getTopUnresolved(v).setType(Type.INT_TYPE));
    } else {
      boolean hasReference = false;
      String desc;
      switch (insn.getOpcode()) {
      case INVOKEVIRTUAL:
      case INVOKESPECIAL:
      case INVOKEINTERFACE:
        hasReference = true;
      case INVOKESTATIC:
        desc = ((MethodInsnNode) insn).desc;
        break;
      case INVOKEDYNAMIC:
        desc = ((InvokeDynamicInsnNode) insn).desc;
        break;
      default:
        throw new AssertionError();
      }
      if (hasReference && isUnresolved(values.get(0))) {
        getTopUnresolved(values.get(0)).setType(OBJECT_TYPE);
      }
      int j = 0;
      Type[] args = Type.getArgumentTypes(desc);
      for (int i = hasReference ? 1 : 0; i < values.size(); i++) {
        Type arg = args[j];
        if (isUnresolved(values.get(i))) {
          getTopUnresolved(values.get(i)).setType(arg);
        }
        j++;
      }
    }
    return super.naryOperation(insn, values);
  }

  @Override
  public void returnOperation(AbstractInsnNode insn, SourceValue value, SourceValue expected) {
    if (isUnresolved(value)) {
      IUnresolvedInstruction unres = getTopUnresolved(value);
      switch (insn.getOpcode()) {
      case IRETURN:
        unres.setType(Type.INT_TYPE);
        break;
      case LRETURN:
        unres.setType(Type.LONG_TYPE);
        break;
      case FRETURN:
        unres.setType(Type.FLOAT_TYPE);
        break;
      case DRETURN:
        unres.setType(Type.DOUBLE_TYPE);
        break;
      case ARETURN:
        unres.setType(OBJECT_TYPE);
        break;
      }
    }
    super.returnOperation(insn, value, expected);
  }

  @Override
  public SourceValue ternaryOperation(AbstractInsnNode insn, SourceValue value1, SourceValue value2, SourceValue value3) {
    if (insn instanceof UnresolvedWideArrayInsn && !((IUnresolvedInstruction) insn).isResolved()) {
      UnresolvedWideArrayInsn unres = (UnresolvedWideArrayInsn) insn;
      unres.setType(getPushType(getTop(value3))); // TODO find a better way
    } else {
      if (isUnresolved(value1))
        getTopUnresolved(value1).setType(OBJECT_TYPE);
      if (isUnresolved(value2))
        getTopUnresolved(value2).setType(Type.INT_TYPE);
      if (isUnresolved(value3)) {
        IUnresolvedInstruction unres = getTopUnresolved(value3);
        switch (insn.getOpcode()) {
        case IASTORE:
        case BASTORE:
        case CASTORE:
        case SASTORE:
          unres.setType(Type.INT_TYPE);
          break;
        case LASTORE:
          unres.setType(Type.LONG_TYPE);
          break;
        case FASTORE:
          unres.setType(Type.FLOAT_TYPE);
          break;
        case DASTORE:
          unres.setType(Type.DOUBLE_TYPE);
          break;
        case AASTORE:
          unres.setType(OBJECT_TYPE);
          break;
        }
      }
    }
    return super.ternaryOperation(insn, value1, value2, value3);
  }

  @Override
  public SourceValue unaryOperation(AbstractInsnNode insn, SourceValue value) {
    if (insn instanceof UnresolvedJumpInsn && !((IUnresolvedInstruction) insn).isResolved()) {
      if (!isUnresolved(value))
        ((UnresolvedJumpInsn) insn).setType(getPushType(getTop(value)));

      if (aggressive) {
        // type of jump or variable does not matter. set it to int.
        if (isUnresolved(value)) {
          getTopUnresolved(value).setType(Type.INT_TYPE);
        }
        ((IUnresolvedInstruction) insn).setType(Type.INT_TYPE);
      }
    } else if (isUnresolved(value)) {
      IUnresolvedInstruction iui = getTopUnresolved(value);
      switch (insn.getOpcode()) {
      case IINC:
        break;
      case INEG:
      case I2B:
      case I2C:
      case I2S:
      case IFEQ:
      case IFNE:
      case IFLT:
      case IFGE:
      case IFGT:
      case IFLE:
      case TABLESWITCH:
      case LOOKUPSWITCH:
      case NEWARRAY:
      case ANEWARRAY:
      case I2L:
      case I2D:
      case I2F:
      case IRETURN:
        iui.setType(Type.INT_TYPE);
        break;
      case L2I:
      case LNEG:
      case L2F:
      case L2D:
      case LRETURN:
        iui.setType(Type.LONG_TYPE);
        break;
      case F2I:
      case FNEG:
      case F2L:
      case F2D:
      case FRETURN:
        iui.setType(Type.FLOAT_TYPE);
        break;
      case D2I:
      case D2F:
      case D2L:
      case DNEG:
      case DRETURN:
        iui.setType(Type.DOUBLE_TYPE);
        break;
      case ARETURN:
      case ARRAYLENGTH:
      case ATHROW:
      case CHECKCAST:
      case INSTANCEOF:
      case MONITORENTER:
      case MONITOREXIT:
      case IFNULL:
      case IFNONNULL:
      case GETFIELD:
        iui.setType(OBJECT_TYPE);
        break;
      case PUTSTATIC:
        iui.setType(Type.getType(((FieldInsnNode) insn).desc));
        break;
      default:
        throw new AssertionError("unimplemented");
      }
    }
    return super.unaryOperation(insn, value);
  }

  private Type getPushType(AbstractInsnNode insn) {
    if (insn instanceof IUnresolvedInstruction)
      throw new IllegalStateException("push type of unresolved instruction is unknown");
    switch (insn.getOpcode()) {
    case ACONST_NULL:
    case ALOAD:
    case AALOAD:
    case NEWARRAY:
    case ANEWARRAY:
    case MULTIANEWARRAY:
    case CHECKCAST:
    case NEW:
      return OBJECT_TYPE;
    case ICONST_M1:
    case ICONST_0:
    case ICONST_1:
    case ICONST_2:
    case ICONST_3:
    case ICONST_4:
    case ICONST_5:
    case BIPUSH:
    case SIPUSH:
    case ILOAD:
    case IALOAD:
    case BALOAD:
    case CALOAD:
    case SALOAD:
    case IADD:
    case ISUB:
    case IMUL:
    case IDIV:
    case IREM:
    case ISHL:
    case IUSHR:
    case IOR:
    case ISHR:
    case IAND:
    case IXOR:
    case LCMP:
    case FCMPL:
    case FCMPG:
    case DCMPL:
    case DCMPG:
    case INEG:
    case L2I:
    case F2I:
    case D2I:
    case I2B:
    case I2C:
    case I2S:
    case ARRAYLENGTH:
    case INSTANCEOF:
      return Type.INT_TYPE;
    case LCONST_0:
    case LCONST_1:
    case LLOAD:
    case LALOAD:
    case LADD:
    case LSUB:
    case LMUL:
    case LDIV:
    case LREM:
    case LSHL:
    case LSHR:
    case LUSHR:
    case LAND:
    case LOR:
    case LXOR:
    case LNEG:
    case I2L:
    case F2L:
    case D2L:
      return Type.LONG_TYPE;
    case FCONST_0:
    case FCONST_1:
    case FCONST_2:
    case FLOAD:
    case FALOAD:
    case FADD:
    case FSUB:
    case FMUL:
    case FDIV:
    case FREM:
    case FNEG:
    case I2F:
    case L2F:
    case D2F:
      return Type.FLOAT_TYPE;
    case DCONST_0:
    case DCONST_1:
    case DLOAD:
    case DALOAD:
    case DADD:
    case DSUB:
    case DMUL:
    case DDIV:
    case DREM:
    case DNEG:
    case I2D:
    case L2D:
    case F2D:
      return Type.DOUBLE_TYPE;
    case LDC:
      throw new AssertionError("should not occur");
    case GETSTATIC:
      throw new AssertionError("should not occur");
    case GETFIELD:
      throw new AssertionError("should not occur");
    case INVOKEVIRTUAL:
    case INVOKESPECIAL:
    case INVOKESTATIC:
    case INVOKEINTERFACE:
      throw new AssertionError("should not occur");
    case INVOKEDYNAMIC:
      throw new AssertionError("should not occur");
    default:
      throw new AssertionError("Illegal opcode " + insn.getOpcode());
    }
  }

  private boolean isUnresolved(SourceValue value) {
    return getTop(value) instanceof IUnresolvedInstruction;
  }

  public static AbstractInsnNode getTop(SourceValue value) {
    return value.insns.toArray(new AbstractInsnNode[0])[value.insns.size() - 1];
  }

  public static IUnresolvedInstruction getTopUnresolved(SourceValue value) {
    return (IUnresolvedInstruction) getTop(value);
  }
}