package me.nov.dalvikgate.utils;

import java.util.Arrays;
import java.util.stream.*;

import org.jf.dexlib2.builder.BuilderInstruction;

public class TextUtils {
  public static String stacktraceToString(Throwable t) {
    return Stream.of(t.getStackTrace()).map(StackTraceElement::toString).limit(3).collect(Collectors.joining("; "));
  }

  public static String toString(BuilderInstruction i) {
    return i.getOpcode().name + " " + Arrays.stream(i.getClass().getDeclaredFields()).map(f -> {
      try {
        f.setAccessible(true);
        return f.get(i).toString();
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }).collect(Collectors.joining(", "));
  }
}
