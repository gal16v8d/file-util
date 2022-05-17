package com.gsdd.file.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ByteConversorTest {

  private static final long EIGHT_THOUSAND = 8000L;
  private static final long TEN_THOUSAND = 10000L;

  @ParameterizedTest
  @ValueSource(longs = {-1, -2})
  void readableFileSizeReturnZeroBytesTest(long input) {
    Assertions.assertEquals("0 B", ByteConversor.readableFileSize(input));
  }

  @ParameterizedTest
  @CsvSource({"100,100 B", Long.MAX_VALUE + ",8 EB"})
  void readableFileSizeTest(long input, String expectedSize) {
    Assertions.assertEquals(expectedSize, ByteConversor.readableFileSize(input));
  }

  @ParameterizedTest
  @CsvSource({"10000,8000", "10000,10000"})
  void getMinAvailableSizeTrueTest(long value, long minAvailable) {
    Assertions.assertTrue(ByteConversor.getMinAvailableSize(value, minAvailable));
  }

  @Test
  void getMinAvailableSizeFalseTest() {
    Assertions.assertFalse(ByteConversor.getMinAvailableSize(EIGHT_THOUSAND, TEN_THOUSAND));
  }
}
