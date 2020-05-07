package me.nov.dalvikgate.utils;

import java.util.stream.*;

public class TextUtils {
  public static String stacktraceToString(Throwable t) {
    return Stream.of(t.getStackTrace()).map(StackTraceElement::toString).limit(3).collect(Collectors.joining("; "));
  }
}
