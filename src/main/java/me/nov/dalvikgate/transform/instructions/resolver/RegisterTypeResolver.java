package me.nov.dalvikgate.transform.instructions.resolver;

import java.util.*;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.*;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;

import me.nov.dalvikgate.transform.instructions.exception.*;

public class RegisterTypeResolver {

  public static final int NOT_DETERMINED = -2;
  public static final int UNKNOWN = -1;

  private MutableMethodImplementation code;
  private List<BuilderInstruction> instructions;
  private int targetRegister;

  private int foundSort = NOT_DETERMINED;

  public RegisterTypeResolver(MutableMethodImplementation code, int register) {
    this.code = code;
    this.instructions = code.getInstructions();
    this.targetRegister = register;
  }

  public void analyze(BuilderInstruction start) {
    Set<Integer> results = findFirstUseTypeSubroutine(start);
    if (results.isEmpty()) {
      foundSort = UNKNOWN;
    } else {
      foundSort = results.iterator().next();
      if (Collections.frequency(results, foundSort) != results.size()) {
        throw new TranslationException("got different results for different subroutines: " + results.toString());
      }
    }
  }

  private Set<Integer> findFirstUseTypeSubroutine(BuilderInstruction instruction) {
    Set<Integer> results = new HashSet<>();
    while (true) {
      int foundSort = findSortIfAvailable(instruction);
      if (foundSort != UNKNOWN) {
        results.add(foundSort);
        return results;
      }
      int index = instructions.indexOf(instruction);
      if (isCodeEnd(instruction) || index >= instructions.size()) {
        // stop analyzing this subroutine, end
        return results;
      }
      instruction = instructions.get(index + 1);
      if (instruction instanceof OffsetInstruction) {
        // careful, could lead to infinite loop
        results.addAll(analyzeJumpSubroutines((BuilderOffsetInstruction) instruction));
      }
    }
  }

  private BuilderInstruction getInstructionAtLabel(Label target) {
    return instructions.stream().filter(i -> i.getLocation().getLabels().contains(target)).findFirst().get();
  }

  private boolean isCodeEnd(BuilderInstruction i) {
    return i.getOpcode().name.startsWith("return") || i.getOpcode().name.startsWith("throw") || i.getOpcode().name.startsWith("goto");
  }

  private Set<Integer> analyzeJumpSubroutines(BuilderOffsetInstruction jump) {
    Set<Integer> results;
    switch (jump.getFormat()) {
    case Format10t:
    case Format20t:
    case Format21t:
    case Format22t:
    case Format30t:
      results = findFirstUseTypeSubroutine(getInstructionAtLabel(jump.getTarget()));
      break;
    case Format31t:
      results = new HashSet<>();
      // payload stuff, switches

      // for each label
      // results.add(findFirstUseTypeSubroutine( label ))
      break;
    default:
      throw new UnsupportedInsnException(jump);
    }
    return results;
  }

  private int findSortIfAvailable(BuilderInstruction instruction) {
    // here check methods or returns for what sort the register could have
    // if method has register as argument
    // determine sort from desc

    // if return has register as argument
    // return method return type sort

    // etc ...

    // don't use jumps for determination
    return UNKNOWN;
  }

  /**
   * @return sort of the found register type, or UNKNOWN, or NOT_DETERMINED
   */
  public int getResultSort() {
    return foundSort;
  }
}
