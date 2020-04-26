package co.com.gsdd.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileConstants {

    protected static final String[] BYTE_UNITS = new String[] { "B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB" };
    public static final Integer FTP550 = 550;
    public static final Integer NUM_1024 = 1024;
    public static final String SMB_URL = "smb://";
    public static final String SMB_URL_SLASH = "\\\\";
    public static final String FILE_PERMISSION = "rw";
    public static final String FILE_EXT = ".tmp";

    /**
     * @return the byteUnits
     */
    public static String[] getByteUnits() {
        return BYTE_UNITS;
    }

}
