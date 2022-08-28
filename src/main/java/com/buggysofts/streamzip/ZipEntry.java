package com.buggysofts.streamzip;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
public class ZipEntry {
    private int crc32;
    private short flags;
    private short compression;
    private long compressedSize;
    private long uncompressedSize;

    private long lastModified;

    private String fileName;
    private byte[] extraField;
    private String fileComment;

    public ZipEntry(@NonNull CentralDirFileHeader header) {
        this.crc32 = header.getCrc32();
        this.flags = header.getFlags();
        this.compression = header.getCompression();
        this.compressedSize = header.getCompressedSize();
        this.uncompressedSize = header.getUncompressedSize();

        this.lastModified = DateTimeUtils.convertMsDosDateTime(
            header.getModTime(),
            header.getModDate()
        );

        this.fileName = header.getFileName();
        this.fileComment = header.getFileComment();
        this.extraField = header.getExtraField();
    }

    public ZipEntry(@NonNull String name) {
        this.fileName = name;
    }

    public boolean isDirectory() {
        return fileName.endsWith("/");
    }
}
