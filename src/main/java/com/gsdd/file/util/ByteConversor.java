package com.gsdd.file.util;

import java.text.DecimalFormat;
import com.gsdd.constants.FileConstants;
import com.gsdd.constants.NumericConstants;
import com.gsdd.constants.RegexConstants;
import com.gsdd.exception.TechnicalException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ByteConversor {

  private static final DecimalFormat FORMATO_NUMERO =
      new DecimalFormat(RegexConstants.DECIMAL_FORMAT);

  public static String readableFileSize(long size) {
    try {
      if (size <= NumericConstants.ZERO) {
        return getFileSizeInUnit(String.valueOf(NumericConstants.ZERO), NumericConstants.ZERO);
      }
      int digitGroups = (int) (Math.log10(size) / Math.log10(FileConstants.NUM_1024));
      String value = FORMATO_NUMERO.format(size / Math.pow(FileConstants.NUM_1024, digitGroups));
      return getFileSizeInUnit(value, digitGroups);
    } catch (Exception e) {
      throw new TechnicalException(e);
    }
  }

  private static String getFileSizeInUnit(String value, int unit) {
    return new StringBuilder().append(value).append(" ").append(FileConstants.getByteUnits()[unit])
        .toString();
  }

  public static boolean getMinAvailableSize(long value, long minAvailable) {
    return value >= minAvailable;
  }

}
