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

  public static void printProgressPercentage(int done, int total) {
    int size = 100;
    if (done > total) {
      throw new IllegalArgumentException();
    }
    int donePercents = (100 * done) / total;
    int doneLength = size * donePercents / 100;

    StringBuilder bar = new StringBuilder("[");
    for (int i = 0; i < size; i++) {
      if (i < doneLength) {
        bar.append('\u25a0'); // "\u2588"
      } else {
        bar.append('.');
      }
    }
    bar.append(']');

    System.out.print("\r" + bar + " " + donePercents + "%");

    if (done == total) {
      System.out.print("\n");
    }
  }
}
