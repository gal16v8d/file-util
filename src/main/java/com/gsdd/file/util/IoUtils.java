package com.gsdd.file.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class IoUtils {

  public static void closeQuietly(final Closeable closeable) {
    closeQuietly(closeable, null);
  }

  public static void closeQuietly(final Closeable closeable, final Consumer<IOException> consumer) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (final IOException e) {
        if (consumer != null) {
          consumer.accept(e);
        }
      }
    }
  }
}
