package com.buggysofts.streamzip;

public enum CompressionMethod {
    NO_COMPRESSION((short) 0),
    SHRUNK((short) 1),
    REDUCED_COMPRESSION_FACTOR_1((short) 2),
    REDUCED_COMPRESSION_FACTOR_2((short) 3),
    REDUCED_COMPRESSION_FACTOR_3((short) 4),
    REDUCED_COMPRESSION_FACTOR_4((short) 5),
    IMPLODED((short) 6),
    RESERVED1((short) 7),
    DEFLATED((short) 8),
    ENHANCED_DEFLATED((short) 9),
    PK_WARE_DCL_IMPLODED((short) 10),
    RESERVED2((short) 11),
    BZIP2((short) 12),
    RESERVED3((short) 13),
    LZMA((short) 14),
    RESERVED4((short) 15),
    RESERVED5((short) 16),
    RESERVED7((short) 17),
    IBM_TERSE((short) 18),
    IBM_LZ77Z((short) 19),
    PPMD_V1R1((short) 98);

    private int val;

    CompressionMethod(short method) {
        this.val = method;
    }
}
