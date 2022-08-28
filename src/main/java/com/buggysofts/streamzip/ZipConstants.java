package com.buggysofts.streamzip;

public final class ZipConstants {
    public static final int SIG_LOCAL_FILE_HEADER = 0x04034b50;
    public static final int SIG_CENTRAL_DIR_FILE_HEADER = 0x02014b50;
    public static final int SIG_END_OF_CENTRAL_DIR_RECORD = 0x06054b50;

    public static final int SIG_ZIP64_END_OF_CENTRAL_DIR_RECORD = 0x06064b50;
    public static final int SIG_ZIP64_END_OF_CENTRAL_DIR_LOCATOR = 0x07064b50;
}
