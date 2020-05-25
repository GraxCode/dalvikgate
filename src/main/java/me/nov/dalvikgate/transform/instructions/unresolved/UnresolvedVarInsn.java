package me.nov.dalvikgate.transform.instructions.unresolved;

import static me.nov.dalvikgate.asm.ASMCommons.*;

import java.util.*;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.DexToASM;
import me.nov.dalvikgate.transform.instructions.IUnresolvedInstruction;
import me.nov.dalvikgate.transform.instructions.exception.UnresolvedInsnException;
import me.nov.dalvikgate.utils.UnresolvedUtils;

public class UnresolvedVarInsn extends VarInsnNode implements IUnresolvedInstruction, Opcodes {
  private final boolean store;
  private final Type initialType;
  private boolean resolvedVar;
  private boolean wide;
  private Type type;

  /**
   * Create new unresolved variable instruction.
   *
   * @param store       {@code true} for storage insns.
   * @param initialType Initial type indicated by android instruction. Will be {@code null} if ambiguous.
   */
  public UnresolvedVarInsn(boolean wide, boolean store, Type initialType) {
    super(UnresolvedUtils.getDefaultVarOp(store), -1);
    this.wide = wide;
    this.store = store;
    this.initialType = initialType;
  }

  @Override
  public void accept(final MethodVisitor methodVisitor) {
    validate();
    super.accept(methodVisitor);
  }

  @Override
  public AbstractInsnNode clone(final Map<LabelNode, LabelNode> clonedLabels) {
    validate();
    return super.clone(clonedLabels);
  }

  /**
   * Validate the opcode and variable index are set.
   */
  public void validate() {
    if (DexToASM.noResolve)
      setType(initialType == null ? Type.INT_TYPE : initialType);
    if (opcode < 0)
      throw new UnresolvedInsnException("Variable opcode has not been resolved!");
    if (var < 0)
      throw new UnresolvedInsnException("Variable index has not been resolved!");
  }

  /**
   * Update the instruction's variable index.
   *
   * @param local Variable index.
   */
  public void setLocal(int local) {
    this.var = local;
    resolvedVar = true;
  }

  public void setType(Type type) {
    if (type.getSize() != (wide ? 2 : 1)) {
      throw new IllegalArgumentException("Wrong size, expected a " + (wide ? "wide" : "single word") + " type, but got " + type.getClassName());
    }
    switch (type.getSort()) {
    case Type.OBJECT:
    case Type.ARRAY:
      setOpcode(store ? ASTORE : ALOAD);
      break;
    case Type.BOOLEAN:
    case Type.INT:
    case Type.BYTE:
    case Type.SHORT:
    case Type.CHAR:
      setOpcode(store ? ISTORE : ILOAD);
      break;
    case Type.FLOAT:
      setOpcode(store ? FSTORE : FLOAD);
      break;
    case Type.LONG:
      setOpcode(store ? LSTORE : LLOAD);
      break;
    case Type.DOUBLE:
      setOpcode(store ? DSTORE : DLOAD);
      break;
    default:
      throw new IllegalArgumentException("Unsupported var type: " + type.getDescriptor());
    }
    this.type = type;
  }

  public boolean isStore() {
    return store;
  }

  @Override
  public boolean isResolved() {
    return resolvedVar && type != null;
  }

  @Override
  public Type getResolvedType() {
    return type;
  }

  public boolean tryResolveUnlinked(int index, MethodNode method) {
    if (var == -1 || isResolved())
      throw new IllegalArgumentException();
    if (!store) {
      visited.clear();
      Type secondAttempt = tryResolveBackwards(method.instructions, method.instructions.get(index).getPrevious());
      if (secondAttempt != null) {
        setType(secondAttempt);
        return true;
      }
    }
    visited.clear();
    Type realType = tryResolveForwards(method.instructions.get(index).getNext());
    if (realType != null) {
      setType(realType);
      return true;
    }
    return false;
  }

  private Set<AbstractInsnNode> visited = new HashSet<>();

  private Type tryResolveForwards(AbstractInsnNode ain) {
    while (ain != null && !isReturn(ain)) {
      if (visited.contains(ain)) {
        // prevent infinite loops
        return null;
      }
      visited.add(ain);
      int op = ain.getOpcode();
      if (op == ATHROW) {
        // block end
        return null;
      }
      if (ain.getType() == AbstractInsnNode.VAR_INSN) {
        VarInsnNode vin = (VarInsnNode) ain;
        boolean canBeReferencePoint = !(vin instanceof UnresolvedVarInsn) || ((UnresolvedVarInsn) vin).isResolved();
        if (vin.var == var && canBeReferencePoint) {
          if (isVarStore(op)) {
            // variable is set before it is loaded, we don't know the type, as the register could now store something else.
            // TODO test if in dalvik the same is possible as in java, reusing locals with different type sorts, otherwise this can be removed
            return null;
          } else {
            switch (op) {
            case ALOAD:
              return OBJECT_TYPE;
            case ILOAD:
              return Type.INT_TYPE;
            case FLOAD:
              return Type.FLOAT_TYPE;
            case DLOAD:
              return Type.DOUBLE_TYPE;
            case LLOAD:
              return Type.LONG_TYPE;
            }
          }
        }
      }
      if (op == GOTO) {
        ain = ((JumpInsnNode) ain).label;
      } else if (ain.getType() == AbstractInsnNode.JUMP_INSN) {
        Type subroutineType = tryResolveForwards(((JumpInsnNode) ain).label);
        if (subroutineType != null) {
          return subroutineType;
        }
        // else continue
      } else if (ain.getType() == AbstractInsnNode.TABLESWITCH_INSN) {
        TableSwitchInsnNode tsin = (TableSwitchInsnNode) ain;
        for (LabelNode label : tsin.labels) {
          Type subroutineType = tryResolveForwards(label);
          if (subroutineType != null) {
            return subroutineType;
          }
        }
        ain = tsin.dflt;
      } else if (ain.getType() == AbstractInsnNode.LOOKUPSWITCH_INSN) {
        LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) ain;
        for (LabelNode label : lsin.labels) {
          Type subroutineType = tryResolveForwards(label);
          if (subroutineType != null) {
            return subroutineType;
          }
        }
        ain = lsin.dflt;
      }
      ain = ain.getNext();
    }
    return null;
  }

  private Type tryResolveBackwards(InsnList instructions, AbstractInsnNode ain) {
    // be careful with this, imagine try catch block handlers
    while (ain != null && !isReturn(ain)) {
      if (visited.contains(ain)) {
        // prevent infinite loops
        return null;
      }
      visited.add(ain);
      int op = ain.getOpcode();
      if (op == ATHROW || op == GOTO) {
        // block end
        return null;
      }
      if (ain.getType() == AbstractInsnNode.VAR_INSN) {
        VarInsnNode vin = (VarInsnNode) ain;
        boolean canBeReferencePoint = !(vin instanceof UnresolvedVarInsn) || ((UnresolvedVarInsn) vin).isResolved();
        if (vin.var == var && canBeReferencePoint) {
          switch (op) {
          case ALOAD:
          case ASTORE:
            return OBJECT_TYPE;
          case ILOAD:
          case ISTORE:
            return Type.INT_TYPE;
          case FLOAD:
          case FSTORE:
            return Type.FLOAT_TYPE;
          case DLOAD:
          case DSTORE:
            return Type.DOUBLE_TYPE;
          case LLOAD:
          case LSTORE:
            return Type.LONG_TYPE;
          }
        }
      }
      if (ain.getType() == AbstractInsnNode.LABEL) {
        List<AbstractInsnNode> references = findJumpsTargettingLabel(instructions, (LabelNode) ain);
        for (AbstractInsnNode jump : references) {
          Type subroutineType = tryResolveBackwards(instructions, jump);
          if (subroutineType != null) {
            return subroutineType;
          }
        }
      }
      ain = ain.getPrevious();
    }
    return null;
  }

  private List<AbstractInsnNode> findJumpsTargettingLabel(InsnList instructions, LabelNode label) {
    ArrayList<AbstractInsnNode> jumps = new ArrayList<>();
    for (AbstractInsnNode ain : instructions) {
      if (ain.getType() == AbstractInsnNode.JUMP_INSN) {
        if (((JumpInsnNode) ain).label == label) {
          jumps.add(ain);
        }
      } else if (ain.getType() == AbstractInsnNode.TABLESWITCH_INSN) {
        TableSwitchInsnNode tsin = (TableSwitchInsnNode) ain;
        if (tsin.labels.contains(label)) {
          jumps.add(ain);
        }
      } else if (ain.getType() == AbstractInsnNode.LOOKUPSWITCH_INSN) {
        LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) ain;
        if (lsin.labels.contains(label)) {
          jumps.add(ain);
        }
      }
    }
    return jumps;
  }
}
