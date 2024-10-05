package com.gsdd.file.util;

import com.gsdd.constants.FileConstants;
import com.gsdd.constants.NumericConstants;
import com.gsdd.constants.RegexConstants;
import com.gsdd.exception.TechnicalException;
import java.text.DecimalFormat;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class ByteConverter {

  private static final DecimalFormat NUMBER_FORMAT =
      new DecimalFormat(RegexConstants.DECIMAL_FORMAT);

  private static final BiFunction<String, Integer, String> FILE_SIZE_TO_UNITS =
      (value, unit) ->
          new StringBuilder(value)
              .append(" ")
              .append(FileConstants.getByteUnits()[unit])
              .toString();

  public static final BiPredicate<Long, Long> MIN_AVAILABLE_SIZE =
      (value, minAvailable) -> value >= minAvailable;

  public static String readableFileSize(long size) {
    try {
      if (size <= NumericConstants.ZERO) {
        return FILE_SIZE_TO_UNITS.apply(
            String.valueOf(NumericConstants.ZERO), NumericConstants.ZERO);
      }
      int digitGroups = (int) (Math.log10(size) / Math.log10(FileConstants.NUM_1024));
      String value = NUMBER_FORMAT.format(size / Math.pow(FileConstants.NUM_1024, digitGroups));
      return FILE_SIZE_TO_UNITS.apply(value, digitGroups);
    } catch (Exception e) {
      throw new TechnicalException(e);
    }
  }
}
