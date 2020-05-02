package me.nov.dalvikgate.transform.instruction;

import static me.nov.dalvikgate.asm.ASMCommons.*;
import static org.objectweb.asm.Type.*;

import java.util.*;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.*;
import org.jf.dexlib2.builder.Label;
import org.jf.dexlib2.builder.instruction.*;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.jf.dexlib2.iface.reference.StringReference;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.asm.Access;
import me.nov.dalvikgate.dexlib.DexLibCommons;
import me.nov.dalvikgate.transform.ITransformer;
import me.nov.dalvikgate.transform.instruction.exception.*;
import me.nov.dalvikgate.transform.instruction.translators.*;
import me.nov.dalvikgate.transform.instruction.tree.*;

/**
 * TODO: make a variable analyzer, as it is not determinable if ifeqz takes an object or an int. also const 0 can mean aconst_null or iconst_0.
 */
public class InstructionTransformer implements ITransformer<DexBackedMethod, InsnList>, Opcodes {
  protected InsnList il;
  protected MethodNode mn;
  protected MutableMethodImplementation builder;
  protected HashMap<BuilderInstruction, LabelNode> labels;
  protected List<BuilderInstruction> dexInstructions;
  protected int argumentRegisterCount;
  protected boolean isStatic;

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
    for (BuilderInstruction i : dexInstructions) {
      if (labels.containsKey(i)) {
        // add labels to the code
        il.add(labels.get(i));
      }
      switch (i.getFormat()) {
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
      case Format21ih:
        // const high 16
        BuilderInstruction21ih _21ih = (BuilderInstruction21ih) i;
        int value21ih = _21ih.getNarrowLiteral();
        il.add(makeIntPush(value21ih));
        addLocalSet(_21ih.getRegisterA(), value21ih);
        continue;
      ////////////////////////// OTHER //////////////////////////
      case Format11x:
        new F11xTranslator(this).translate((BuilderInstruction11x) i);
        continue;
      case Format12x:
        new F12xTranslator(this).translate((BuilderInstruction12x) i);
        continue;
      case Format21c:
        new F21cTranslator(this).translate((BuilderInstruction21c) i);
        continue;
      case Format21t:
        new F21tTranslator(this).translate((BuilderInstruction21t) i);
        continue;
      case Format22b:
        new F22bTranslator(this).translate((BuilderInstruction22b) i);
        continue;
      case Format22c:
        new F22cTranslator(this).translate((BuilderInstruction22c) i);
        continue;
      case Format22cs:
        // iput/iget-type-quick ==> only generated by optimizer
        throw new UnsupportedInsnException(i);
      case Format22s:
        // int 16 bit math
        new F22sTranslator(this).translate((BuilderInstruction22s) i);
        continue;
      case Format22t:
        // conditional jumps
        new F22tTranslator(this).translate((BuilderInstruction22t) i);
        continue;
      case Format22x:
        // move from 16
        new F22xTranslator(this).translate((BuilderInstruction22x) i);
        continue;
      case Format23x:
        new F23xTranslator(this).translate((BuilderInstruction23x) i);
        continue;
      case Format31t:
        // fill-array-data and switches
        new F31tTranslator(this).translate((BuilderInstruction31t) i);
        continue;
      case Format32x:
        // move 16
        new F32xTranslator(this).translate((BuilderInstruction32x) i);
        continue;
      ////////////////////////// INVOKE //////////////////////////
      case Format35c:
        new F35cTranslator(this).translate((BuilderInstruction35c) i);
        continue;
      case Format35ms:
        // invokeQuick
        throw new UnsupportedInsnException(i);
      case Format3rc:
        new F3rcTranslator(this).translate((BuilderInstruction3rc) i);
        continue;
      ////////////////////////// EXECUTE INLINE //////////////////////////
      case Format35mi:
        // execute inline
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
      ////////////////////////// PAYLOADS //////////////////////////
      // we can ignore payloads, as they are no real instructions
      case PackedSwitchPayload:
      case SparseSwitchPayload:
      case ArrayPayload:
        continue;
      case UnresolvedOdexInstruction:
      default:
        throw new UnsupportedInsnException(i);
      }
    }
    this.transformTryCatchBlocks();
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
      } else if (insn instanceof UnresolvedWideArrayInsnNode) {
        System.err.println("   - " + i + ": unresolved WIDE ARRAY");
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
        BuilderInstruction firstHandlerOp = (BuilderInstruction) tb.exceptionHandler.getHandler().getLocation().getInstruction();
        LabelNode startLabel = getASMLabel(tb.start);
        LabelNode endLabel = getASMLabel(tb.end);
        LabelNode handlerLabel = getASMLabel(tb.exceptionHandler.getHandler());
        if (firstHandlerOp.getOpcode() != Opcode.MOVE_EXCEPTION) {
          // no move-exception opcode, we need to make a "bridge" to match java stack sizes, as in java bytecode an exception object would be on the stack, while in dalvik there isn't.
          LabelNode newHandler = new LabelNode();
          il.add(newHandler);
          il.add(new InsnNode(POP)); // pop ignored exception
          il.add(new JumpInsnNode(GOTO, handlerLabel)); // go to old offset
          handlerLabel = newHandler; // change tcb handler to new one
        }
        mn.tryCatchBlocks.add(new TryCatchBlockNode(startLabel, endLabel, handlerLabel, handler));
      });
    }
  }

  /**
   * Get the assigned ASM LabelNode for a DEX label using the label map generated by {@link #buildLabels() buildLabels}. Multiple labels can have the same LabelNode.
   * 
   * @param label The label
   */
  public LabelNode getASMLabel(Label label) {
    return Objects.requireNonNull(labels.get(labels.keySet().stream().filter(i -> i.getLocation().getLabels().contains(label)).findFirst()
        .orElseThrow(() -> new TranslationException("dex label has no equivalent LabelNode " + label.getLocation().getInstruction().getOpcode()))));
  }

  /**
   * Gets the next instruction, ignoring payloads
   */
  public BuilderInstruction getNextOf(BuilderInstruction i) {
    try {
      do {
        i = builder.getInstructions().get(i.getLocation().getIndex() + 1);
      } while (i.getFormat().isPayloadFormat);
      return i;
    } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
      throw new TranslationException("could not find next of " + i.getOpcode() + " at index " + i.getLocation().getIndex());
    }
  }

  /**
   * Old method to convert registers to labels, considering parameters and method visibility. This can lead to illegal use of labels in java bytecode.
   * 
   * @param register The register to be converted
   */
  @Deprecated
  protected int regToLocal(int register) {
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
  protected void addLocalSetObject(int register) {
    addLocalGetSet(true, register, OBJECT_TYPE);
  }

  /**
   * Add local set for the given type.
   *
   * @param register Register index.
   * @param type     Discovered type to put.
   */
  protected void addLocalSet(int register, Type type) {
    if (type != null) {
      if (type.getSort() == ARRAY)
        type = ARRAY_TYPE;
      else if (type.getSort() == OBJECT)
        type = OBJECT_TYPE;
      else if (type.getSort() == VOID)
        throw new TranslationException("Illegal type 'void'");
    }
    addLocalGetSet(true, register, type);
  }

  /**
   * Add local set for potentially int type.
   *
   * @param register Register index.
   * @param value    Int value.
   */
  protected void addLocalSet(int register, int value) {
    addLocalGetSet(true, register, value == 0 ? null : INT_TYPE);
  }

  /**
   * Add local set for potentially long type.
   *
   * @param register Register index.
   * @param value    Long value.
   */
  protected void addLocalSet(int register, long value) {
    addLocalGetSet(true, register, value == 0 ? null : LONG_TYPE);
  }

  /**
   * Add local get for object type.
   *
   * @param register Register index.
   */
  protected void addLocalGetObject(int register) {
    addLocalGetSet(false, register, OBJECT_TYPE);
  }

  /**
   * Add local get for the given type.
   *
   * @param register Register index.
   * @param type     Discovered type to get.
   */
  protected void addLocalGet(int register, Type type) {
    if (type != null) {
      if (type.getSort() == ARRAY)
        type = ARRAY_TYPE;
      else if (type.getSort() == OBJECT)
        type = OBJECT_TYPE;
      else if (type.getSort() == VOID)
        throw new TranslationException("Illegal type 'void'");
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
  protected void addLocalGetSet(boolean store, int register, Type type) {
    UnresolvedVarInsnNode var = new UnresolvedVarInsnNode(store, type);
    var.setLocal(regToLocal(register)); // only for now. this only works when no variables are reused.
    if (type != null)
      var.setType(type);
    // else
    // var.setOpcode(store ? ASTORE : ALOAD); for debugging purposes
    il.add(var);
  }
}