package me.nov.dalvikgate;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.stream.Collectors;

import org.jf.dexlib2.*;
import org.jf.dexlib2.dexbacked.*;
import org.objectweb.asm.tree.ClassNode;

import me.nov.dalvikgate.asm.Conversion;
import me.nov.dalvikgate.logging.LogWrapper;
import me.nov.dalvikgate.transform.classes.ClassTransformer;
import me.nov.dalvikgate.transform.instructions.exception.UnresolvedInsnException;
import me.nov.dalvikgate.utils.TextUtils;

public class DexToASM {
  public static final LogWrapper logger = new LogWrapper();
  public static boolean noResolve;
  public static boolean noOptimize;
  public static String nameFilter;

  public static void dex2Jar(File inDex, File outJar) throws IOException {
    saveAsJar(outJar, convertToASMTree(inDex));
  }

  public static void dex2JarConsole(File inDex, File outJar) throws IOException {
    if (!inDex.exists()) {
      throw new FileNotFoundException();
    }
    DexBackedDexFile baseBackedDexFile = DexFileFactory.loadDexFile(inDex, Opcodes.forApi(52));
    List<ClassNode> asmClasses = new ArrayList<>();
    List<DexBackedClassDef> toConvert = baseBackedDexFile.getClasses().stream().filter(clazz -> clazz.getType().substring(1, clazz.getType().length() - 1).matches(nameFilter))
        .collect(Collectors.toList());
    int size = toConvert.size();
    logger.disableConsoleLog();
    for (int i = 0; i < size; i++) {
      TextUtils.printProgressPercentage(i, size);
      DexBackedClassDef clazz = toConvert.get(i);
      ClassTransformer ct = new ClassTransformer();
      ct.visit(clazz);
      asmClasses.add(ct.getTransformed());
    }
    saveAsJar(outJar, asmClasses);
  }

  public static List<ClassNode> convertToASMTree(File file) throws IOException {
    if (!file.exists()) {
      throw new FileNotFoundException();
    }
    DexBackedDexFile baseBackedDexFile = DexFileFactory.loadDexFile(file, Opcodes.forApi(52));
    return convertToASMTree(baseBackedDexFile);
  }

  public static List<ClassNode> convertToASMTree(DexBackedDexFile baseBackedDexFile) throws IOException {
    List<ClassNode> asmClasses = new ArrayList<>();
    List<DexBackedClassDef> toConvert = baseBackedDexFile.getClasses().stream().filter(clazz -> clazz.getType().substring(1, clazz.getType().length() - 1).matches(nameFilter))
        .collect(Collectors.toList());
    for (DexBackedClassDef clazz : toConvert) {
      ClassTransformer ct = new ClassTransformer();
      ct.visit(clazz);
      asmClasses.add(ct.getTransformed());
    }
    return asmClasses;
  }

  public static void saveAsJar(File output, List<ClassNode> classes) {
    try (JarOutputStream out = new JarOutputStream(new FileOutputStream(output))) {
      out.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
      out.write(makeManifest().getBytes());
      out.closeEntry();
      for (ClassNode c : classes) {
        if (!c.name.matches(nameFilter))
          continue;
        try {
          out.putNextEntry(new JarEntry(c.name + ".class"));
          out.write(Conversion.toBytecode(c));
          out.closeEntry();
        } catch (UnresolvedInsnException e) {
          DexToASM.logger.error("SKIP: {}", c.name, e);
        } catch (Exception e) {
          DexToASM.logger.error("FAIL: {}", c.name, e);
        }
      }
    } catch (IOException e) {
      DexToASM.logger.error("IO exception", e);
    }
  }

  private static String makeManifest() {
    String lineSeparator = "\r\n";
    StringBuilder manifest = new StringBuilder();
    manifest.append("Manifest-Version: 1.0");
    manifest.append(lineSeparator);
    manifest.append("Transformed-By: dalvikgate ");
    manifest.append(getVersion());
    manifest.append(lineSeparator);
    return manifest.toString();
  }

  public static String getVersion() {
    try {
      return Objects.requireNonNull(DexToASM.class.getPackage().getImplementationVersion());
    } catch (NullPointerException e) {
      return "(dev)";
    }
  }
}
