package com.gsdd.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class FileConstants {

  public static final Integer FTP550 = 550;
  public static final Integer NUM_1024 = 1024;
  public static final String SMB_URL = "smb://";
  public static final String FILE_PERMISSION = "rw";
  public static final String FILE_EXT = ".tmp";
  static final String[] BYTE_UNITS =
      new String[] {"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};

  /**
   * @return the byteUnits
   */
  public static String[] getByteUnits() {
    return BYTE_UNITS;
  }
}
