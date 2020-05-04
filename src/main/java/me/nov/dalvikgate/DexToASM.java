package me.nov.dalvikgate;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.jar.*;
import java.util.logging.Logger;

import org.jf.dexlib2.*;
import org.jf.dexlib2.dexbacked.*;
import org.objectweb.asm.tree.ClassNode;

import me.nov.dalvikgate.asm.Conversion;
import me.nov.dalvikgate.transform.classes.ClassTransformer;
import me.nov.dalvikgate.transform.instruction.exception.UnresolvedInsnException;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class DexToASM implements Callable<Integer> {
  public static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  public static void main(String[] args) throws IOException {
    int exitCode = new CommandLine(new DexToASM()).execute(args);
    System.exit(exitCode);
  }

  @Option(names = { "-i", "--in" }, paramLabel = "input", description = "the dalvik file to convert (dex/odex/apk)")
  public static File input;

  @Option(names = { "-o", "--out" }, paramLabel = "output", description = "the output jar archive")
  public static File output;

  @Option(names = { "-nr", "--noresolve" }, description = "do not resolve variable types")
  public static boolean noResolve;

  @Option(names = { "-no", "--nooptimize" }, description = "do not optimize produced code")
  public static boolean noOptimize;

  @Override
  public Integer call() throws Exception {
    saveAsJar(output, convertToASMTree(input));
    return 0;
  }

  public static List<ClassNode> convertToASMTree(File file) throws IOException {
    if (!file.exists()) {
      throw new FileNotFoundException();
    }
    DexBackedDexFile baseBackedDexFile = DexFileFactory.loadDexFile(file, Opcodes.forApi(52));
    Set<? extends DexBackedClassDef> baseClassDefs = baseBackedDexFile.getClasses();
    List<ClassNode> asmClasses = new ArrayList<>();

    for (DexBackedClassDef clazz : baseClassDefs) {
      ClassTransformer ct = new ClassTransformer();
      ct.visit(clazz);
      asmClasses.add(ct.getTransformed());
    }
    return asmClasses;
  }

  public static void saveAsJar(File output, List<ClassNode> classes) {
    try {
      JarOutputStream out = new JarOutputStream(new FileOutputStream(output));
      out.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
      out.write(makeManifest().getBytes());
      out.closeEntry();
      for (ClassNode c : classes) {
        try {
          out.putNextEntry(new JarEntry(c.name + ".class"));
          out.write(Conversion.toBytecode(c));
          out.closeEntry();
        } catch (UnresolvedInsnException e) {
          DexToASM.logger.severe("SKIP: " + c.name + " - " + e.getMessage());
        } catch (Exception e) {
          DexToASM.logger.severe("FAIL: " + c.name);
          e.printStackTrace();
        }
      }
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
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
