package me.nov.dalvikgate.tests.utils;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Arrays;

import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

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
      Method define = ClassLoader.class.getDeclaredMethod("defineClass0", String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
      define.setAccessible(true);
      return (Class<?>) define.invoke(ClassLoader.getSystemClassLoader(), name, bytes, 0, bytes.length, null);
    } catch (Exception e) {
      throw new RuntimeException("dynamic test loading failed", e);
    }
  }

  public static MethodNode runDexToASM(Type desc, MutableMethodImplementation mmi) {
    MethodNode mn = buildMethod(desc, null);
    InstructionTransformer it = new InstructionTransformer(mn, mmi, desc, true);
    it.visit(null);
    mn.instructions = it.getTransformed();
    return mn;
  }
}
