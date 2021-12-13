package com.gsdd.file.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.gsdd.constants.FileConstants;
import com.gsdd.constants.NumericConstants;

class FileConstantsTest {

  @Test
  void getByteUnitsTest() {
    String[] units = FileConstants.getByteUnits();
    Assertions.assertEquals(NumericConstants.NINE, units.length);
    Assertions.assertArrayEquals(new String[] {"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"},
        units);
  }
}
