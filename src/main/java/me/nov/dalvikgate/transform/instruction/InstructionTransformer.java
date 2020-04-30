package me.nov.dalvikgate.transform.instruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import me.nov.dalvikgate.transform.instruction.exception.UnsupportedInsnException;
import me.nov.dalvikgate.transform.instruction.tree.UnresolvedJumpInsnNode;

import org.jf.dexlib2.Format;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.BuilderOffsetInstruction;
import org.jf.dexlib2.builder.Label;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction11n;
import org.jf.dexlib2.builder.instruction.BuilderInstruction11x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction12x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21ih;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21lh;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21s;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21t;
import org.jf.dexlib2.builder.instruction.BuilderInstruction22b;
import org.jf.dexlib2.builder.instruction.BuilderInstruction22c;
import org.jf.dexlib2.builder.instruction.BuilderInstruction22s;
import org.jf.dexlib2.builder.instruction.BuilderInstruction22t;
import org.jf.dexlib2.builder.instruction.BuilderInstruction23x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction31c;
import org.jf.dexlib2.builder.instruction.BuilderInstruction31i;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;
import org.jf.dexlib2.builder.instruction.BuilderInstruction51l;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.asm.Access;
import me.nov.dalvikgate.dexlib.DexLibCommons;
import me.nov.dalvikgate.transform.ITransformer;
import me.nov.dalvikgate.transform.instruction.tree.UnresolvedVarInsnNode;

import static me.nov.dalvikgate.asm.ASMCommons.*;
import static org.objectweb.asm.Type.*;

/**
 * TODO: make a variable analyzer, as it is not determinable if ifeqz takes an object or an int. also const 0 can mean aconst_null or iconst_0.
 */
public class InstructionTransformer implements ITransformer<DexBackedMethod, InsnList>, Opcodes {
  private InsnList il;
  private MethodNode mn;
  private MutableMethodImplementation builder;
  private HashMap<BuilderInstruction, LabelNode> labels;
  private List<BuilderInstruction> dexInstructions;
  private int argumentRegisterCount;
  private boolean isStatic;

  public InstructionTransformer(MethodNode mn, DexBackedMethod method, MutableMethodImplementation builder) {
    this.mn = mn;
    this.builder = builder;
    this.dexInstructions = builder.getInstructions();
    this.isStatic = Access.isStatic(method.accessFlags); // dalvik and java bytecode have the same access values
    // "this" reference is passed as argument in dalvik
    this.argumentRegisterCount = method.getParameters().stream().mapToInt(DexLibCommons::getSize).sum() + (isStatic ? 0 : 1);
  }

  @Override
  public InsnList getTransformed() {
    return Objects.requireNonNull(il);
  }

  @Override
  public void build(DexBackedMethod method) {
    il = new InsnList();
    this.buildLabels();
    this.transformTryCatchBlocks();
    for (BuilderInstruction i : dexInstructions) {
      if (labels.containsKey(i)) {
        // add labels to the code
        il.add(labels.get(i));
      }
      switch (i.getFormat()) {
      case ArrayPayload:
        continue;

      ////////////////////////// GOTOS //////////////////////////
      case Format10t:
      case Format20t:
      case Format30t:
        // 8, 16 and 32 bit goto
        il.add(new JumpInsnNode(GOTO, getASMLabel(((BuilderOffsetInstruction) i).getTarget())));
        continue;
      ////////////////////////// VOID RETURNS //////////////////////////
      case Format10x:
        if (i.getOpcode() != Opcode.NOP) {
          // only void returns, ignore NOPs
          il.add(new InsnNode(RETURN));
        }
        continue;
      ////////////////////////// CONST //////////////////////////
      case Format11n:
        // Move the given literal value (sign-extended to 32 bits) into the specified register.
        // A: destination register (4 bits)
        // B: signed int (4 bits)
        // TODO this can be the same as ACONST_NULL too
        BuilderInstruction11n _11n = (BuilderInstruction11n) i;
        int value11n = _11n.getNarrowLiteral();
        il.add(makeIntPush(value11n));
        addLocalSet(_11n.getRegisterA(), value11n);
        continue;
      case Format31c:
        // Move a reference to the string specified by the given index into the specified register.
        // A: destination register (8 bits)
        // B: string index
        BuilderInstruction31c _31c = (BuilderInstruction31c) i;
        il.add(new LdcInsnNode(((StringReference) _31c.getReference()).getString()));
        addLocalSetObject(_31c.getRegisterA());
        continue;
      case Format31i:
        // 0x14: Move the given literal value into the specified register.
        // A: destination register (8 bits)
        // B: arbitrary 32-bit constant
        // 0x17: Move the given literal value (sign-extended to 64 bits) into the specified register-pair.
        // A: destination register (8 bits)
        // B: signed int (32 bits)
        // TODO unsure if const-wide-32 need some bitshifting
        BuilderInstruction31i _31i = (BuilderInstruction31i) i;
        int value3li = _31i.getNarrowLiteral();
        il.add(makeIntPush(value3li));
        addLocalSet(_31i.getRegisterA(), value3li);
        continue;
      case Format51l:
        // Move the given literal value into the specified register-pair.
        // A: destination register (8 bits)
        // B: arbitrary double-width (64-bit) constant
        BuilderInstruction51l _51l = (BuilderInstruction51l) i;
        long value51l = _51l.getWideLiteral();
        il.add(new LdcInsnNode(value51l));
        addLocalSet(_51l.getRegisterA(), value51l);
        continue;
      case Format11x:
        visitSingleRegister((BuilderInstruction11x) i);
        continue;
      case Format12x:
        visitDoubleRegister((BuilderInstruction12x) i);
        continue;
      case Format21c:
        visitReferenceSingleRegister((BuilderInstruction21c) i);
        continue;
      case Format21ih:
        BuilderInstruction21ih _21ih = (BuilderInstruction21ih) i;
        int value21ih = _21ih.getNarrowLiteral();
        il.add(makeIntPush(value21ih));
        addLocalSet(_21ih.getRegisterA(), value21ih);
        continue;
      case Format21lh:
        // Move the given literal value (right-zero-extended to 64 bits) into the specified register-pair
        // A: destination register (8 bits)
        // B: signed int (16 bits)
        BuilderInstruction21lh _21lh = (BuilderInstruction21lh) i;
        long value23lh = _21lh.getWideLiteral();
        il.add(makeLongPush(value23lh));
        addLocalSet(_21lh.getRegisterA(), value23lh);
        continue;
      case Format21s:
        // Move the given literal value (sign-extended to 64 bits) into the specified register-pair.
        // A: destination register (8 bits)
        // B: signed int (16 bits)
        BuilderInstruction21s _21s = (BuilderInstruction21s) i;
        int value21s = _21s.getNarrowLiteral();
        il.add(makeIntPush(value21s));
        addLocalSet(_21s.getRegisterA(), value21s);
        continue;
      case Format21t:
        visitSingleJump((BuilderInstruction21t) i);
        continue;
      case Format22b:
        visitInt8Math((BuilderInstruction22b) i);
        continue;
      case Format22c:
        BuilderInstruction22c _22c = (BuilderInstruction22c) i;
        if (_22c.getReferenceType() == ReferenceType.FIELD) {
          // non-static field ops
          FieldReference fieldReference = (FieldReference) _22c.getReference();
          String owner = Type.getType(fieldReference.getDefiningClass()).getInternalName();
          String name = fieldReference.getName();
          String desc = fieldReference.getType();
          if (_22c.getOpcode().name.startsWith("iget")) {
            // B: object reference
            // A: destination
            addLocalGetObject(_22c.getRegisterB());
            il.add(new FieldInsnNode(GETFIELD, owner, name, desc));
            addLocalSet(_22c.getRegisterA(), Type.getType(desc));
          } else {
            // B: object reference
            // A: value
            addLocalGetObject(_22c.getRegisterB());
            addLocalGet(_22c.getRegisterA(), getTypeForDesc(desc));
            il.add(new FieldInsnNode(PUTFIELD, owner, name, desc));
          }
          continue;
        } else {
          TypeReference typeReference = (TypeReference) _22c.getReference();
          if (i.getOpcode() == Opcode.INSTANCE_OF) {
            addLocalGetObject(_22c.getRegisterB()); // object reference
            il.add(new TypeInsnNode(INSTANCEOF, Type.getType(typeReference.getType()).getInternalName()));
            addLocalSet(_22c.getRegisterA(), INT_TYPE); // boolean
            continue;
          }
          if (i.getOpcode() == Opcode.NEW_ARRAY) {
            addLocalGet(_22c.getRegisterB(), INT_TYPE); // array size
            Type arrayType = Type.getType(typeReference.getType()).getElementType();
            if (arrayType.getSort() == Type.OBJECT) {
              il.add(new TypeInsnNode(ANEWARRAY, arrayType.getInternalName()));
            } else {
              il.add(new IntInsnNode(NEWARRAY, getPrimitiveIndex(arrayType.getDescriptor())));
            }
            addLocalSet(_22c.getRegisterA(), ARRAY_TYPE);
            continue;
          }
        }
      case Format22cs:
        // iput/iget-type-quick ==> Not listed on dalvik bytecode page?
        throw new UnsupportedInsnException(i);
      case Format22s:
        visitInt16Math((BuilderInstruction22s) i);
        continue;
      case Format22t:
        // conditional jumps
        visitCompareJump((BuilderInstruction22t) i);
        continue;
      case Format22x:
        // move from 16
        throw new UnsupportedInsnException(i);
      case Format23x:
        visitTripleRegister((BuilderInstruction23x) i);
        continue;
      case Format31t:
        // fill-array-data and switches
        throw new UnsupportedInsnException(i);
      case Format32x:
        // more moves
        throw new UnsupportedInsnException(i);
      ////////////////////////// INVOKE //////////////////////////
      case Format35c:
        visitInvoke((BuilderInstruction35c) i);
        continue;
      case Format35ms:
        // invokeQuick
        throw new UnsupportedInsnException(i);
      case Format3rc:
        throw new UnsupportedInsnException(i);
      ////////////////////////// EXECUTE INLINE //////////////////////////
      case Format35mi:
        // execute inline range
        throw new UnsupportedInsnException(i);
      case Format3rmi:
        // execute inline range
        throw new UnsupportedInsnException(i);
      case Format3rms:
        // invoke quick range
        throw new UnsupportedInsnException(i);
      ////////////////////////// INVOKE POLYMORPHIC //////////////////////////
      case Format45cc:
        throw new UnsupportedInsnException(i);
      case Format4rcc:
        throw new UnsupportedInsnException(i);
      ////////////////////////// SPECIAL INSTRUCTIONS //////////////////////////
      case Format20bc:
        il.add(makeExceptionThrow("java/lang/VerifyError", "throw-verification-error instruction"));
        continue;
      case PackedSwitchPayload:
        throw new UnsupportedInsnException(i);
      case SparseSwitchPayload:
        throw new UnsupportedInsnException(i);
      case UnresolvedOdexInstruction:
      default:
        throw new UnsupportedInsnException(i);
      }
    }
  }

  @Override
  public void buildDone(DexBackedMethod method) {
    int i = 0;
    // TODO: Type analysis and fill in missing data for resolvable instructions
    System.err.println(method.getDefiningClass() + " - " + method.getName());
    for (AbstractInsnNode insn : il) {
      if (insn instanceof UnresolvedJumpInsnNode) {
        System.err.println("   - " + i + ": unresolved JUMP");
      } else if (insn instanceof UnresolvedVarInsnNode) {
        System.err.println("   - " + i + ": unresolved VARIABLE");
      }
      i++;
    }
  }

  /**
   * Assign a new LabelNode for every instruction with a minimum of one label
   */
  private void buildLabels() {
    labels = new HashMap<>();
    // build labels first so we can reference them while rewriting
    for (BuilderInstruction i : dexInstructions) {
      if (!i.getLocation().getLabels().isEmpty()) {
        labels.put(i, new LabelNode());
      }
    }
  }

  /**
   * Transform try catch blocks using the label map generated by {@link #buildLabels() buildLabels}
   */
  private void transformTryCatchBlocks() {
    if (builder.getTryBlocks() != null) {
      mn.tryCatchBlocks = new ArrayList<>();
      builder.getTryBlocks().forEach(tb -> {
        String handlerType = tb.exceptionHandler.getExceptionType();
        String handler = handlerType == null ? null : Type.getType(handlerType).getInternalName();
        mn.tryCatchBlocks.add(new TryCatchBlockNode(getASMLabel(tb.start), getASMLabel(tb.end), getASMLabel(tb.exceptionHandler.getHandler()), handler));
      });
    }
  }

  /**
   * Get the assigned ASM LabelNode for a DEX label using the label map generated by {@link #buildLabels() buildLabels}. Multiple labels can have the same LabelNode.
   * 
   * @param label The label
   */
  public LabelNode getASMLabel(Label label) {
    return labels.get(labels.keySet().stream().filter(i -> i.getLocation().getLabels().contains(label)).findFirst().get());
  }

  /**
   * Old method to convert registers to labels, considering parameters and method visibility. This can lead to illegal use of labels in java bytecode.
   * 
   * @param register The register to be converted
   */
  @Deprecated
  private int regToLocal(int register) {
    // The N arguments to a method land in the last N registers of the method's invocation frame, in order
    int startingArgs = builder.getRegisterCount() - argumentRegisterCount;
    if (register >= startingArgs) {
      return register - startingArgs; // 0 is reserved for "this"
    }
    return register + argumentRegisterCount;
  }

  /**
   * Add local set for object type.
   *
   * @param register Register index.
   */
  private void addLocalSetObject(int register) {
    addLocalGetSet(true, register, OBJECT_TYPE);
  }

  /**
   * Add local set for the given type.
   *
   * @param register Register index.
   * @param type     Discovered type to put.
   */
  private void addLocalSet(int register, Type type) {
    if (type != null) {
      if (type.getSort() == ARRAY)
        type = ARRAY_TYPE;
      else if (type.getSort() == OBJECT)
        type = OBJECT_TYPE;
      else if (type.getSort() == VOID)
        throw new IllegalStateException("Illegal type 'void'");
    }
    addLocalGetSet(true, register, type);
  }

  /**
   * Add local set for potentially int type.
   *
   * @param register Register index.
   * @param value    Int value.
   */
  private void addLocalSet(int register, int value) {
    addLocalGetSet(true, register, value == 0 ? null : INT_TYPE);
  }

  /**
   * Add local set for potentially long type.
   *
   * @param register Register index.
   * @param value    Long value.
   */
  private void addLocalSet(int register, long value) {
    addLocalGetSet(true, register, value == 0 ? null : LONG_TYPE);
  }

  /**
   * Add local get for object type.
   *
   * @param register Register index.
   */
  private void addLocalGetObject(int register) {
    addLocalGetSet(false, register, OBJECT_TYPE);
  }

  /**
   * Add local get for the given type.
   *
   * @param register Register index.
   * @param type     Discovered type to get.
   */
  private void addLocalGet(int register, Type type) {
    if (type != null) {
      if (type.getSort() == ARRAY)
        type = ARRAY_TYPE;
      else if (type.getSort() == OBJECT)
        type = OBJECT_TYPE;
      else if (type.getSort() == VOID)
        throw new IllegalStateException("Illegal type 'void'");
    }
    // TODO: Check calls to this method. Is there a case where "0" will be used as a "null"?
    // if so, then uncomment the following code:
    //
    // else if (type.getSort() == INT)
    // type = null;
    //
    addLocalGetSet(false, register, type);
  }

  /**
   * Add local set for given type.
   *
   * @param store    {@code true} when insn is a setter.
   * @param register Variable index.
   * @param type     Type of variable. {@code null} if ambiguous.
   */
  private void addLocalGetSet(boolean store, int register, Type type) {
    UnresolvedVarInsnNode var = new UnresolvedVarInsnNode(store, type);
    var.setLocal(regToLocal(register)); // only for now. this only works when no variables are reused.
    if (type != null)
      var.setType(type);
    il.add(var);
  }

  private void visitInt8Math(BuilderInstruction22b i) {
    // Perform the indicated binary op on the indicated register (first argument) and literal value (second argument),
    // storing the result in the destination register.
    // A: destination register (8 bits)
    // B: source register (8 bits)
    // C: signed int constant (8 bits)
    addLocalGet(i.getRegisterB(), INT_TYPE);
    addLocalGet(i.getRegisterA(), INT_TYPE);
    switch (i.getOpcode()) {
    case ADD_INT_LIT8:
      il.add(new InsnNode(IADD));
      break;
    case RSUB_INT_LIT8:
      il.add(new InsnNode(ISUB));
      break;
    case MUL_INT_LIT8:
      il.add(new InsnNode(IMUL));
      break;
    case DIV_INT_LIT8:
      il.add(new InsnNode(IDIV));
      break;
    case REM_INT_LIT8:
      il.add(new InsnNode(IREM));
      break;
    case AND_INT_LIT8:
      il.add(new InsnNode(IAND));
      break;
    case OR_INT_LIT8:
      il.add(new InsnNode(IOR));
      break;
    case XOR_INT_LIT8:
      il.add(new InsnNode(IXOR));
      break;
    case SHL_INT_LIT8:
      il.add(new InsnNode(ISHL));
      break;
    case SHR_INT_LIT8:
      il.add(new InsnNode(ISHR));
      break;
    case USHR_INT_LIT8:
      il.add(new InsnNode(IUSHR));
      break;
    default:
      throw new UnsupportedInsnException(i);
    }
    addLocalSet(i.getRegisterA(), INT_TYPE);
  }

  private void visitInt16Math(BuilderInstruction22s i) {
    // Perform the indicated binary op on the indicated register (first argument) and literal value (second argument),
    // storing the result in the destination register.
    // A: destination register (4 bits)
    // B: source register (4 bits)
    // C: signed int constant (16 bits)
    addLocalGet(i.getRegisterB(), INT_TYPE);
    addLocalGet(i.getRegisterA(), INT_TYPE);
    switch (i.getOpcode()) {
    case ADD_INT_LIT8:
      il.add(new InsnNode(IADD));
      break;
    case RSUB_INT_LIT8:
      il.add(new InsnNode(ISUB));
      break;
    case MUL_INT_LIT8:
      il.add(new InsnNode(IMUL));
      break;
    case DIV_INT_LIT8:
      il.add(new InsnNode(IDIV));
      break;
    case REM_INT_LIT8:
      il.add(new InsnNode(IREM));
      break;
    case AND_INT_LIT8:
      il.add(new InsnNode(IAND));
      break;
    case OR_INT_LIT8:
      il.add(new InsnNode(IOR));
      break;
    case XOR_INT_LIT8:
      il.add(new InsnNode(IXOR));
      break;
    case SHL_INT_LIT8:
      il.add(new InsnNode(ISHL));
      break;
    case SHR_INT_LIT8:
      il.add(new InsnNode(ISHR));
      break;
    case USHR_INT_LIT8:
      il.add(new InsnNode(IUSHR));
      break;
    default:
      throw new UnsupportedInsnException(i);
    }
    addLocalSet(i.getRegisterA(), INT_TYPE);
  }

  private void visitSingleRegister(BuilderInstruction11x i) {
    // Move immediate word value into register.
    // A: destination register
    int source = i.getRegisterA();
    switch (i.getOpcode()) {
    case MOVE_RESULT:
      // "Move the single-word non-object result of..."
      // - So this should be a primitive
      addLocalSet(source, getPushedTypeForInsn(il.getLast()));
      break;
    case MOVE_EXCEPTION:
    case MOVE_RESULT_OBJECT:
      addLocalSetObject(source);
      break;
    case MOVE_RESULT_WIDE:
      // Get type from last written instruction
      addLocalSet(source, getPushedTypeForInsn(il.getLast()));
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

  private void visitDoubleRegister(BuilderInstruction12x i) {
    int source = i.getRegisterB();
    int destination = i.getRegisterA();
    switch (i.getOpcode()) {
    case MOVE:
      if (source == destination)
        break;
      addLocalGet(source, INT_TYPE);
      addLocalSet(destination, INT_TYPE);
      break;
    case MOVE_WIDE:
      if (source == destination)
        break;
      // Cannot determine if double or long, resolve later
      addLocalGet(source, null);
      addLocalSet(destination, null);
    case MOVE_OBJECT:
      if (source == destination)
        break;
      addLocalGetObject(source);
      addLocalSetObject(destination);
      break;
    case ARRAY_LENGTH:
      addLocalGet(source, ARRAY_TYPE);
      il.add(new InsnNode(ARRAYLENGTH));
      addLocalSet(destination, INT_TYPE);
      break;
    case NEG_INT:
      addLocalGet(source, INT_TYPE);
      il.add(new InsnNode(INEG));
      addLocalSet(destination, INT_TYPE);
      break;
    case NOT_INT:
      addLocalGet(source, INT_TYPE);
      il.add(new InsnNode(ICONST_M1));
      il.add(new InsnNode(IXOR));
      addLocalSet(destination, INT_TYPE);
      break;
    case NEG_LONG:
      addLocalGet(source, LONG_TYPE);
      il.add(new InsnNode(LNEG));
      addLocalSet(destination, LONG_TYPE);
      break;
    case NOT_LONG:
      addLocalGet(source, LONG_TYPE);
      il.add(new LdcInsnNode(-1L));
      il.add(new InsnNode(LXOR));
      addLocalSet(destination, LONG_TYPE);
      break;
    case NEG_FLOAT:
      addLocalGet(source, FLOAT_TYPE);
      il.add(new InsnNode(FNEG));
      addLocalSet(destination, FLOAT_TYPE);
      break;
    case NEG_DOUBLE:
      addLocalGet(source, DOUBLE_TYPE);
      il.add(new InsnNode(DNEG));
      addLocalSet(destination, DOUBLE_TYPE);
      break;
    case INT_TO_LONG:
      addLocalGet(source, INT_TYPE);
      il.add(new InsnNode(I2L));
      addLocalSet(destination, LONG_TYPE);
      break;
    case INT_TO_FLOAT:
      addLocalGet(source, INT_TYPE);
      il.add(new InsnNode(I2F));
      addLocalSet(destination, FLOAT_TYPE);
      break;
    case INT_TO_DOUBLE:
      addLocalGet(source, INT_TYPE);
      il.add(new InsnNode(I2D));
      addLocalSet(destination, DOUBLE_TYPE);
      break;
    case LONG_TO_INT:
      addLocalGet(source, LONG_TYPE);
      il.add(new InsnNode(L2I));
      addLocalSet(destination, INT_TYPE);
      break;
    case LONG_TO_FLOAT:
      addLocalGet(source, LONG_TYPE);
      il.add(new InsnNode(L2F));
      addLocalSet(destination, FLOAT_TYPE);
      break;
    case LONG_TO_DOUBLE:
      addLocalGet(source, LONG_TYPE);
      il.add(new InsnNode(L2D));
      addLocalSet(destination, DOUBLE_TYPE);
      break;
    case FLOAT_TO_INT:
      addLocalGet(source, FLOAT_TYPE);
      il.add(new InsnNode(F2I));
      addLocalSet(destination, INT_TYPE);
      break;
    case FLOAT_TO_LONG:
      addLocalGet(source, FLOAT_TYPE);
      il.add(new InsnNode(F2L));
      addLocalSet(destination, LONG_TYPE);
      break;
    case FLOAT_TO_DOUBLE:
      addLocalGet(source, FLOAT_TYPE);
      il.add(new InsnNode(F2D));
      addLocalSet(destination, DOUBLE_TYPE);
      break;
    case DOUBLE_TO_INT:
      addLocalGet(source, DOUBLE_TYPE);
      il.add(new InsnNode(D2I));
      addLocalSet(destination, INT_TYPE);
      break;
    case DOUBLE_TO_LONG:
      addLocalGet(source, DOUBLE_TYPE);
      il.add(new InsnNode(D2L));
      addLocalSet(destination, LONG_TYPE);
      break;
    case DOUBLE_TO_FLOAT:
      addLocalGet(source, DOUBLE_TYPE);
      il.add(new InsnNode(D2F));
      addLocalSet(destination, FLOAT_TYPE);
      break;
    case INT_TO_BYTE:
      addLocalGet(source, INT_TYPE);
      il.add(new InsnNode(I2B));
      addLocalSet(destination, BYTE_TYPE);
      break;
    case INT_TO_CHAR:
      addLocalGet(source, INT_TYPE);
      il.add(new InsnNode(I2C));
      addLocalSet(destination, CHAR_TYPE);
      break;
    case INT_TO_SHORT:
      addLocalGet(source, INT_TYPE);
      il.add(new InsnNode(I2S));
      addLocalSet(destination, SHORT_TYPE);
      break;
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
    case ADD_FLOAT_2ADDR:
    case SUB_FLOAT_2ADDR:
    case MUL_FLOAT_2ADDR:
    case DIV_FLOAT_2ADDR:
    case REM_FLOAT_2ADDR:
    case ADD_DOUBLE_2ADDR:
    case SUB_DOUBLE_2ADDR:
    case MUL_DOUBLE_2ADDR:
    case DIV_DOUBLE_2ADDR:
    case REM_DOUBLE_2ADDR:
      // TODO destination also contains first source register (first two bits destination, second two source). int source = second source register
      // have to do some bit shifting action here
    default:
      throw new UnsupportedInsnException(i);
    }
  }

  private void visitReferenceSingleRegister(BuilderInstruction21c i) {
    // Move a reference to the string specified by the given index into the specified register.
    // A: destination register (8 bits)
    // B: string or type index
    int local = i.getRegisterA();
    Reference ref = i.getReference();
    switch (i.getOpcode()) {
    case CONST_STRING:
      il.add(new LdcInsnNode(((StringReference) ref).getString()));
      addLocalSet(local, OBJECT_TYPE);
      return;
    case CONST_CLASS:
      il.add(new LdcInsnNode(Type.getType(((TypeReference) ref).getType())));
      addLocalSet(local, OBJECT_TYPE);
      return;
    case CONST_METHOD_HANDLE:
    case CONST_METHOD_TYPE:
      throw new UnsupportedInsnException(i);
    case CHECK_CAST:
      addLocalGet(local, OBJECT_TYPE);
      il.add(new TypeInsnNode(CHECKCAST, Type.getType(((TypeReference) ref).getType()).getInternalName()));
      return;
    case NEW_INSTANCE:
      il.add(new TypeInsnNode(NEW, Type.getType(((TypeReference) ref).getType()).getInternalName()));
      addLocalSet(local, OBJECT_TYPE);
      return;
    default:
      break;
    }
    FieldReference fr = (FieldReference) ref;
    String owner = Type.getType(fr.getDefiningClass()).getInternalName();
    String name = fr.getName();
    String desc = fr.getType();
    switch (i.getOpcode()) {
    case SGET:
    case SGET_VOLATILE:
    case SGET_BOOLEAN:
    case SGET_BYTE:
    case SGET_CHAR:
    case SGET_SHORT:
      il.add(new FieldInsnNode(GETSTATIC, owner, name, desc));
      addLocalSet(local, INT_TYPE);
      break;
    case SGET_WIDE:
    case SGET_WIDE_VOLATILE:
      il.add(new FieldInsnNode(GETSTATIC, owner, name, desc));
      addLocalSet(local, desc.equals("J") ? LONG_TYPE : DOUBLE_TYPE);
      break;
    case SGET_OBJECT:
    case SGET_OBJECT_VOLATILE:
      il.add(new FieldInsnNode(GETSTATIC, owner, name, desc));
      addLocalSetObject(local);
      break;
    case SPUT:
    case SPUT_VOLATILE:
    case SPUT_BOOLEAN:
    case SPUT_BYTE:
    case SPUT_CHAR:
    case SPUT_SHORT:
      addLocalGet(local, INT_TYPE);
      il.add(new FieldInsnNode(PUTSTATIC, owner, name, desc));
      break;
    case SPUT_WIDE:
    case SPUT_WIDE_VOLATILE:
      addLocalGet(local, desc.equals("J") ? LONG_TYPE : DOUBLE_TYPE);
      il.add(new FieldInsnNode(PUTSTATIC, owner, name, desc));
      break;
    case SPUT_OBJECT:
    case SPUT_OBJECT_VOLATILE:
      addLocalGetObject(local);
      il.add(new FieldInsnNode(PUTSTATIC, owner, name, desc));
      break;
    default:
      throw new UnsupportedInsnException(i);
    }
  }

  private void visitCompareJump(BuilderInstruction22t i) {
    // Branch to the given destination if the given two registers' values compare as specified.
    // A: first register to test (4 bits)
    // B: second register to test (4 bits)
    int first = i.getRegisterA();
    int second = i.getRegisterB();
    // Check if it we can safely assume the values are numeric.
    // - Any compare op except <IF_EQ> and <IF_NE> is safely numeric
    Opcode opcode = i.getOpcode();
    boolean refsMustBeNumeric = !(opcode == Opcode.IF_EQ || opcode == Opcode.IF_NE);
    if (refsMustBeNumeric) {
      // TODO: can comparison jumps in dalvik also take longs, doubles, etc?
      // - Or are all comparison-jumps based on integers?
      addLocalGet(first, INT_TYPE);
      addLocalGet(second, INT_TYPE);
    } else {
      addLocalGet(first, null);
      addLocalGet(second, null);
    }
    Label label = i.getTarget();
    switch (opcode) {
    case IF_EQ:
    case IF_NE:
      // Dalvik has no "null", instead used "0" which means we can't immediately tell if we should use object comparison, or integer comparison.
      // So we will resolve the opcode later when we can perform type analysis.
      il.add(new UnresolvedJumpInsnNode(opcode, getASMLabel(label)));
      break;
    case IF_LT:
      il.add(new JumpInsnNode(IF_ICMPLT, getASMLabel(label)));
      break;
    case IF_GE:
      il.add(new JumpInsnNode(IF_ICMPGE, getASMLabel(label)));
      break;
    case IF_GT:
      il.add(new JumpInsnNode(IF_ICMPGT, getASMLabel(label)));
      break;
    case IF_LE:
      il.add(new JumpInsnNode(IF_ICMPLE, getASMLabel(label)));
      break;
    default:
      throw new UnsupportedInsnException("compare-jump", i);
    }
  }

  private void visitSingleJump(BuilderInstruction21t i) {
    // Branch to the given destination if the given register's value compares with 0 as specified.
    // A: register to test (8 bits)
    // B: signed branch offset (16 bits)
    int source = i.getRegisterA();
    Opcode opcode = i.getOpcode();
    boolean refsMustBeNumeric = !(opcode == Opcode.IF_EQZ || opcode == Opcode.IF_NEZ);
    if (refsMustBeNumeric) {
      // TODO: can single jumps in dalvik also take longs, doubles, etc?
      // - Or are all single jumps based on integers?
      addLocalGet(source, INT_TYPE);
    } else {
      addLocalGet(source, null);
    }
    Label label = i.getTarget();
    switch (opcode) {
    case IF_EQZ:
    case IF_NEZ:
      // Dalvik has no "null", instead used "0" which means we can't immediately tell if we should use object comparison, or integer comparison.
      // So we will resolve the opcode later when we can perform type analysis.
      il.add(new UnresolvedJumpInsnNode(opcode, getASMLabel(label)));
      break;
    case IF_LTZ:
      il.add(new JumpInsnNode(IFLT, getASMLabel(label)));
      break;
    case IF_GEZ:
      il.add(new JumpInsnNode(IFGE, getASMLabel(label)));
      break;
    case IF_GTZ:
      il.add(new JumpInsnNode(IFGT, getASMLabel(label)));
      break;
    case IF_LEZ:
      il.add(new JumpInsnNode(IFLE, getASMLabel(label)));
      break;
    default:
      throw new UnsupportedInsnException(i);
    }
  }

  private void visitTripleRegister(BuilderInstruction23x i) {
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
    switch (i.getOpcode()) {
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
    default:
      break;
    }
  }

  /**
   * Visit invoke instruction with a maximum of 5 arguments
   *
   * @param i
   */
  private void visitInvoke(BuilderInstruction35c i) {
    // Call the indicated method. The result (if any) may be stored with an appropriate move-result* variant as the immediately subsequent instruction.
    // A: argument word count (4 bits)
    // B: method reference index (16 bits)
    // C..G: argument registers (4 bits each)
    if (i.getOpcode() == Opcode.FILLED_NEW_ARRAY) {
      throw new UnsupportedInsnException("filled-new-array", i);
    }
    MethodReference mr = (MethodReference) i.getReference();
    String owner = Type.getType(mr.getDefiningClass()).getInternalName();
    String name = mr.getName();
    String desc = buildMethodDesc(mr.getParameterTypes(), mr.getReturnType());
    int registers = i.getRegisterCount(); // sum of all local sizes
    int parameters = mr.getParameterTypes().stream().mapToInt(p -> Type.getType((String) p).getSize()).sum(); // sum of all parameter sizes (parameters + reference = registers)
    int parIdx = 0;
    int regIdx = 0;
    if (registers > parameters) {
      if (registers > parameters + 1) {
        throw new IllegalArgumentException("too many registers: " + registers + " for method with desc " + desc);
      }
      // TODO: Check parameter type?
      // - then use addLocalGet(register, <<TYPE>>) instead of null
      addLocalGetObject(i.getRegisterC());
      registers--; // reference can only be size 1
      regIdx++;
    }
    @SuppressWarnings("unchecked")
    List<String> parameterTypes = (List<String>) mr.getParameterTypes();
    while (registers > 0) {
      int register;
      switch (regIdx) {
      case 0:
        register = i.getRegisterC();
        break;
      case 1:
        register = i.getRegisterD();
        break;
      case 2:
        register = i.getRegisterE();
        break;
      case 3:
        register = i.getRegisterF();
        break;
      case 4:
        register = i.getRegisterG();
        break;
      default:
        throw new IllegalArgumentException("more than 5 parameters: " + desc);
      }
      regIdx++;
      String pDesc = parameterTypes.get(parIdx);
      addLocalGet(register, Type.getType(pDesc));
      registers -= Type.getType(pDesc).getSize();
      parIdx++;
    }
    switch (i.getOpcode()) {
    case INVOKE_SUPER:
    case INVOKE_DIRECT_EMPTY:
      il.add(new MethodInsnNode(INVOKESPECIAL, owner, name, desc));
      break;
    case INVOKE_VIRTUAL:
      il.add(new MethodInsnNode(INVOKEVIRTUAL, owner, name, desc));
      break;
    case INVOKE_DIRECT:
      il.add(new MethodInsnNode(name.equals("<init>") ? INVOKESPECIAL : INVOKEVIRTUAL, owner, name, desc));
      break;
    case INVOKE_STATIC:
      il.add(new MethodInsnNode(INVOKESTATIC, owner, name, desc));
      break;
    case INVOKE_INTERFACE:
      il.add(new MethodInsnNode(INVOKEINTERFACE, owner, name, desc));

      break;
    case INVOKE_CUSTOM:
      // like invokedynamic
    default:
      throw new UnsupportedInsnException(i);
    }
  }
}