package me.nov.dalvikgate.transform.instructions;

import static me.nov.dalvikgate.asm.ASMCommons.*;

import java.util.*;

import javax.annotation.Nullable;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.*;
import org.jf.dexlib2.builder.Label;
import org.jf.dexlib2.builder.instruction.*;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import me.nov.dalvikgate.DexToASM;
import me.nov.dalvikgate.asm.Access;
import me.nov.dalvikgate.dexlib.DexLibCommons;
import me.nov.dalvikgate.transform.ITransformer;
import me.nov.dalvikgate.transform.instructions.exception.*;
import me.nov.dalvikgate.transform.instructions.resolving.InstructionResolver;
import me.nov.dalvikgate.transform.instructions.translators.*;
import me.nov.dalvikgate.transform.instructions.translators.invoke.*;
import me.nov.dalvikgate.transform.instructions.translators.jump.*;
import me.nov.dalvikgate.transform.instructions.translators.references.*;
import me.nov.dalvikgate.utils.TextUtils;

/**
 * TODO: make a variable analyzer, as it is not determinable if ifeqz takes an object or an int. also const 0 can mean aconst_null or iconst_0.
 */
public class InstructionTransformer implements ITransformer<DexBackedMethod, InsnList>, Opcodes {
  protected InsnList il;
  public MethodNode mn;
  public MutableMethodImplementation builder;
  public HashMap<BuilderInstruction, LabelNode> labels;
  public List<BuilderInstruction> dexInstructions;
  public int argumentRegisterCount;
  public boolean isStatic;

  public InstructionTransformer(MethodNode mn, DexBackedMethod method, MutableMethodImplementation builder) {
    this.mn = mn;
    this.builder = builder;
    this.dexInstructions = builder.getInstructions();
    this.isStatic = Access.isStatic(method.accessFlags); // dalvik and java bytecode have the same access values
    // "this" reference is passed as argument in dalvik
    this.argumentRegisterCount = method.getParameters().stream().mapToInt(DexLibCommons::getSize).sum() + (isStatic ? 0 : 1);
  }

  public InstructionTransformer(MethodNode mn, MutableMethodImplementation builder, Type desc, boolean isStatic) {
    this.mn = mn;
    this.builder = builder;
    this.dexInstructions = builder.getInstructions();
    this.isStatic = isStatic;
    this.argumentRegisterCount = Arrays.stream(desc.getArgumentTypes()).mapToInt(Type::getSize).sum() + (isStatic ? 0 : 1);
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
      if(method.getName().equals("f") && method.getDefiningClass().endsWith("/n;") && dexInstructions.size() == 11)
        System.out.println(TextUtils.toString(i));
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
        il.add(new JumpInsnNode(GOTO, toASMLabel(((BuilderOffsetInstruction) i).getTarget())));
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
      case Format21lh:
      case Format21s:
      case Format31c:
      case Format31i:
      case Format51l:
      case Format21ih:
        new ConstTranslator(this).translate(i);
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
        new SingleJumpTranslator(this).translate((BuilderInstruction21t) i);
        continue;
      case Format22b:
        new IntMath8Translator(this).translate((BuilderInstruction22b) i);
        continue;
      case Format22c:
        // type instructions
        new F22cTranslator(this).translate((BuilderInstruction22c) i);
        continue;
      case Format22cs:
        // quick fields
        new QuickFieldTranslator(this).translate((BuilderInstruction22cs) i);
        continue;
      case Format22s:
        // int 16 bit math
        new IntMath16Translator(this).translate((BuilderInstruction22s) i);
        continue;
      case Format22t:
        // conditional jumps
        new CompareJumpTranslator(this).translate((BuilderInstruction22t) i);
        continue;
      case Format22x:
        // move from 16
        new MoveFrom16Translator(this).translate((BuilderInstruction22x) i);
        continue;
      case Format23x:
        new F23xTranslator(this).translate((BuilderInstruction23x) i);
        continue;
      case Format31t:
        // fill-array-data and switches
        new PayloadTranslator(this).translate((BuilderInstruction31t) i);
        continue;
      case Format32x:
        // move 16
        new Move16Translator(this).translate((BuilderInstruction32x) i);
        continue;
      ////////////////////////// INVOKE //////////////////////////
      case Format35c:
        // invoke
        new InvokeTranslator(this).translate((BuilderInstruction35c) i);
        continue;
      case Format3rc:
        // invoke range
        new InvokeRangeTranslator(this).translate((BuilderInstruction3rc) i);
        continue;
      case Format35ms:
        // invoke quick
        new QuickTranslator(this).translate((BuilderInstruction35ms) i);
        continue;
      case Format3rms:
        // invoke quick range
        new QuickRangeTranslator(this).translate((BuilderInstruction3rms) i);
        continue;
      ////////////////////////// EXECUTE INLINE //////////////////////////
      case Format35mi:
        // execute inline
        new ExecInlineTranslator(this).translate((BuilderInstruction35mi) i);
        continue;
      case Format3rmi:
        // execute inline range
        il.add(makeExceptionThrow("java/lang/VerifyError",
            "execute-inline-range is unexpected! are there any predefined native methods with more than 5 registers? " + ((BuilderInstruction3rmi) i).getRegisterCount()));
        continue;
      ////////////////////////// INVOKE POLYMORPHIC //////////////////////////
      case Format45cc:
        // would actually be easy to handle (just like execute-inline, see https://stackoverflow.com/questions/49027506/how-to-generate-invoke-polymorphic-opcodes-in-dalvik)
        il.add(makeExceptionThrow("java/lang/VerifyError", "invoke-polymorphic not supported by dexlib2"));
        continue;
      case Format4rcc:
        il.add(makeExceptionThrow("java/lang/VerifyError",
            "invoke-polymorphic-range is unexpected! are there any MethodHandle / VarArgs methods with more than 5 registers? " + ((BuilderInstruction4rcc) i).getRegisterCount()));
        continue;
      ////////////////////////// SPECIAL INSTRUCTIONS //////////////////////////
      case Format20bc:
        il.add(makeExceptionThrow("java/lang/VerifyError", "throw-verification-error instruction"));
        continue;
      case UnresolvedOdexInstruction:
        il.add(makeExceptionThrow("java/lang/VerifyError", "unresolved odex instruction"));
        continue;
      ////////////////////////// PAYLOADS //////////////////////////
      // we can ignore payloads, as they are no real instructions
      case PackedSwitchPayload:
      case SparseSwitchPayload:
      case ArrayPayload:
        continue;
      default:
        throw new UnsupportedInsnException(i);
      }
    }
    this.transformTryCatchBlocks();
  }

  @Override
  public void buildDone(DexBackedMethod method) {
    if (DexToASM.noResolve)
      return;
    if (method == null)
      throw new IllegalStateException("Dex method for instruction visitor cannot be null");
    if (mn == null)
      throw new IllegalStateException("ASM method for instruction visitor cannot be null");
    new InstructionResolver(method, mn, il).run();
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
        LabelNode startLabel = toASMLabel(tb.start);
        LabelNode endLabel = getASMLabelNull(tb.end);
        LabelNode handlerLabel = toASMLabel(tb.exceptionHandler.getHandler());
        if (endLabel == null) {
          // try catch block reaches over the whole code
          // has to be some special try catch block type
          // (either obfuscation or special monitor handler)
          il.add(endLabel = new LabelNode());
        }
        if (handler == null && isSpecialMonitorHandler(firstHandlerOp)) {
          // ignore
          return;
        }
        if (firstHandlerOp.getOpcode() != Opcode.MOVE_EXCEPTION) {
          // no move-exception opcode, we need to make a "bridge" to match java stack sizes, as in java bytecode an exception object would be on the stack, while in dalvik there isn't.
          // offset can be reached by multiple routines
          if (startLabel == handlerLabel) {
            DexToASM.logger.error("unexpected case: tcb start is also handler");
            // unexpected case, use old handler creation
            LabelNode newHandler = new LabelNode();
            il.add(newHandler);
            il.add(new InsnNode(POP)); // pop ignored exception
            il.add(new JumpInsnNode(GOTO, handlerLabel)); // go to old offset
            handlerLabel = newHandler; // change tcb handler to new one
          } else {
            // try to get it back to java structure
            LabelNode newHandler = new LabelNode();
            LabelNode beforeJumpLabel = new LabelNode();
            il.insertBefore(handlerLabel, beforeJumpLabel);
            il.insertBefore(handlerLabel, new JumpInsnNode(GOTO, handlerLabel)); // jump over the new handler
            il.insertBefore(handlerLabel, newHandler);
            il.insertBefore(handlerLabel, new InsnNode(POP)); // pop ignored exception and continue where the real handler should be
            if (endLabel == handlerLabel) {
              endLabel = beforeJumpLabel; // make sure to not encapsulate the handler
            }
            handlerLabel = newHandler; // change tcb handler to new one
          }
        }
        mn.tryCatchBlocks.add(new TryCatchBlockNode(startLabel, endLabel, handlerLabel, handler));
      });
    }
  }

  /**
   * Dalvik adds special try catch blocks for synchronized blocks. We do not need to translate them to java.
   * 
   * @param firstHandlerOp first instruction in catch block
   */
  private boolean isSpecialMonitorHandler(BuilderInstruction firstHandlerOp) {
    // we do not need to check if they exist as code would be invalid either way
    if (firstHandlerOp.getOpcode() == Opcode.MOVE_EXCEPTION) {
      BuilderInstruction next = getNextOf(firstHandlerOp);
      if (next.getOpcode() == Opcode.MONITOR_EXIT) {
        BuilderInstruction athrow = getNextOf(next);
        if (athrow.getOpcode() == Opcode.THROW) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Get the assigned ASM LabelNode for a DEX label using the label map generated by {@link #buildLabels() buildLabels}. Multiple labels can have the same LabelNode.
   * 
   * @param label The label
   */
  public LabelNode toASMLabel(Label label) {
    if (label.getLocation().getInstruction() == null) {
      int index = label.getLocation().getIndex();
      if (index >= dexInstructions.size()) {
        // we could throw an exception here
        // throw new TranslationException("dalvik label points to the end of the method");
        DexToASM.logger.error("dalvik label points to the end of the method, creating throw block");
        LabelNode newBlock = new LabelNode();
        LabelNode afterBlock = new LabelNode();
        il.add(new JumpInsnNode(GOTO, afterBlock));
        il.add(newBlock);
        il.add(makeExceptionThrow("java/lang/VerifyError", "bad EOC label"));
        il.add(afterBlock);
        return newBlock;
      }
      throw new TranslationException("dalvik label has no assigned instruction at index " + index);
    }
    return Objects.requireNonNull(labels.get(labels.keySet().stream().filter(i -> i.getLocation().getLabels().contains(label)).findFirst()
        .orElseThrow(() -> new TranslationException("dalvik label has no equivalent LabelNode" + label.getLocation().getInstruction().getOpcode()))));
  }

  /**
   * Get the assigned ASM LabelNode for a DEX label using the label map generated by {@link #buildLabels() buildLabels}. Multiple labels can have the same LabelNode.
   * 
   * @param label The label
   */
  @Nullable
  public LabelNode getASMLabelNull(Label label) {
    if (label.getLocation().getInstruction() == null) {
      return null;
    }
    return labels.get(labels.keySet().stream().filter(i -> i.getLocation().getLabels().contains(label)).findFirst().get());
  }

  /**
   * Gets the next instruction, ignoring payloads
   */
  public BuilderInstruction getNextOf(BuilderInstruction i) {
    try {
      do {
        i = builder.getInstructions().get(i.getLocation().getIndex() + 1);
      } while (i.getFormat().isPayloadFormat || i.getOpcode() == Opcode.NOP);
      return i;
    } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
      throw new TranslationException("could not find succeeding instruction of " + i.getOpcode() + " at index " + i.getLocation().getIndex());
    }
  }

  /**
   * Method to convert registers to labels, considering parameters and method visibility. Be aware that dalvik reuses unused registers.
   * 
   * @param register The register to be converted
   */
  protected int regToLocal(int register) {
    // The N arguments to a method land in the last N registers of the method's invocation frame, in order
    int startingArgs = builder.getRegisterCount() - argumentRegisterCount;
    if (register >= startingArgs) {
      return register - startingArgs; // 0 is reserved for "this"
    }
    return register + argumentRegisterCount;
  }
}