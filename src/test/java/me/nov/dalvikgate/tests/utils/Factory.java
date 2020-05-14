package me.nov.dalvikgate.tests.utils;

import java.io.*;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.*;

import org.jf.dexlib2.builder.*;
import org.jf.dexlib2.dexbacked.DexBackedMethod;
import org.mockito.Mockito;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import com.google.common.io.Files;

import me.nov.dalvikgate.DexToASM;
import me.nov.dalvikgate.asm.Conversion;
import me.nov.dalvikgate.graph.Inheritance;
import me.nov.dalvikgate.transform.instructions.InstructionTransformer;

public class Factory implements Opcodes {
  public static MethodNode buildMethod(Type desc, InsnList il, TryCatchBlockNode... tcbs) {
    MethodNode mn = new MethodNode(ACC_PUBLIC | ACC_STATIC, "proxyMethod", desc.getDescriptor(), null, null);
    mn.instructions = il;
    mn.maxLocals = 999;
    mn.maxStack = 999;
    mn.tryCatchBlocks = Arrays.asList(tcbs);
    return mn;
  }

  public static ClassNode createClassProxy() {
    ClassNode proxy = new ClassNode();
    proxy.access = ACC_PUBLIC;
    proxy.version = 52;
    proxy.name = "TestClass" + proxy.hashCode();
    proxy.superName = "java/lang/Object";
    return proxy;
  }

  public static Object executeMethodAtRuntime(MethodNode mn, Object... args) {
    ClassNode classProxy = createClassProxy();
    classProxy.methods.add(mn);
    byte[] bytes = toBytecode(classProxy);
    Class<?> loadedClass = bytesToClass(classProxy.name, bytes);
    try {
      return Arrays.stream(loadedClass.getMethods()).filter(m -> m.getName().equals(mn.name)).findFirst().get().invoke(null, args);
    } catch (Throwable e) {
      throw new RuntimeException("method execution at runtime failed", e);
    }
  }

  public static byte[] toBytecode(ClassNode cn) {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    cn.accept(cw);
    byte[] b = cw.toByteArray();
    return b;
  }

  public static Class<?> bytesToClass(String name, byte[] bytes) {
    try {
      Method define = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
      define.setAccessible(true);
      return (Class<?>) define.invoke(ClassLoader.getSystemClassLoader(), name, bytes, 0, bytes.length, null);
    } catch (Exception e) {
      throw new RuntimeException("dynamic test loading failed", e);
    }
  }

  public static MethodNode runDexToASM(Type desc, MethodImplementationBuilder mmi) {
    return runDexToASM(getNopInheritance(), desc, mmi);
  }

  public static MethodNode runDexToASM(Inheritance inheritance, Type desc, MethodImplementationBuilder builder) {
    MethodNode mn = buildMethod(desc, null);
    InstructionTransformer it = new InstructionTransformer(mn, new MutableMethodImplementation(builder.getMethodImplementation()), desc, true);
    it.setInheritance(inheritance);
    DexBackedMethod dm = Mockito.mock(DexBackedMethod.class);
    Mockito.when(dm.getName()).thenReturn("dummy" + builder.hashCode());
    Mockito.when(dm.getParameters()).thenReturn(Collections.emptyList());
    Mockito.when(dm.getReturnType()).thenReturn(desc.getReturnType().getDescriptor());
    Mockito.when(dm.getDefiningClass()).thenReturn("Lcom/TestClass;");
    Mockito.when(dm.getAccessFlags()).thenReturn(Opcodes.ACC_STATIC);
    it.visit(dm);
    mn.instructions = it.getTransformed();
    return mn;
  }

  public static void saveDebug(MethodNode mn) {
    ClassNode cn = createClassProxy();
    cn.methods.add(mn);
    try {
      Files.write(Conversion.toBytecode(cn), new File("debug.class"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static Inheritance getNopInheritance() {
    return new Inheritance();
  }

  public static Inheritance getFullIheritance() {
    return DexToASM.rootInheritGraph;
  }

}
