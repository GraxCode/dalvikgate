package me.nov.dalvikgate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.objectweb.asm.tree.ClassNode;

import me.nov.dalvikgate.asm.Conversion;
import me.nov.dalvikgate.transform.classes.ClassTransformer;

public class DexToASM {
  public static void main(String[] args) throws IOException {
    if (args.length == 2) {
      saveAsJar(new File(args[1]), convertToASMTree(new File(args[0])));
    } else {
      System.out.println("java -jar dalvikgate.jar <input.dex> <output.jar>");
    }
  }

  private static ArrayList<ClassNode> convertToASMTree(File file) throws IOException {
    if (!file.exists()) {
      throw new FileNotFoundException();
    }
    DexBackedDexFile baseBackedDexFile = DexFileFactory.loadDexFile(file, Opcodes.forApi(52));
    final Set<? extends DexBackedClassDef> baseClassDefs = baseBackedDexFile.getClasses();
    ArrayList<ClassNode> asmClasses = new ArrayList<>();

    for (DexBackedClassDef clazz : baseClassDefs) {
      ClassTransformer transformer = new ClassTransformer(clazz, 52);
      transformer.build();
      asmClasses.add(transformer.get());
    }
    return asmClasses;
  }

  public static void saveAsJar(File output, ArrayList<ClassNode> classes) {
    try {
      JarOutputStream out = new JarOutputStream(new FileOutputStream(output));
      out.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
      out.write("Manifest-Version: 1.0\r\nTransformed-By: dalvikgate\r\n".getBytes());
      out.closeEntry();
      for (ClassNode c : classes) {
        try {
          out.putNextEntry(new JarEntry(c.name + ".class"));
          out.write(Conversion.toBytecode(c, true));
          out.closeEntry();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
