package com.buggysofts.streamzip;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
class HeaderFlags {
    private final boolean isEncrypted;
    private final boolean compressionOption1;
    private final boolean compressionOption2;
    private final boolean dataDescriptor;
    private final boolean enhancedDeflation;
    private final boolean compressedPatchedData;
    private final boolean strongEncryption;
    private final boolean languageEncoding;
    private final boolean reserved1;
    private final boolean maskedHeaderValues;
    private final boolean isReserved2;
    private final boolean isReserved3;

    public HeaderFlags(int flagsInt) {
        this.isEncrypted = (flagsInt & (1 << 0)) != 0;
        this.compressionOption1 = (flagsInt & (1 << 1)) != 0;
        this.compressionOption2 = (flagsInt & (1 << 2)) != 0;
        this.dataDescriptor = (flagsInt & (1 << 3)) != 0;
        this.enhancedDeflation = (flagsInt & (1 << 4)) != 0;
        this.compressedPatchedData = (flagsInt & (1 << 5)) != 0;
        this.strongEncryption = (flagsInt & (1 << 6)) != 0;
        this.languageEncoding = (flagsInt & (1 << 11)) != 0;
        this.reserved1 = (flagsInt & (1 << 12)) != 0;
        this.maskedHeaderValues = (flagsInt & (1 << 13)) != 0;
        this.isReserved2 = (flagsInt & (1 << 14)) != 0;
        this.isReserved3 = (flagsInt & (1 << 15)) != 0;
    }
}
