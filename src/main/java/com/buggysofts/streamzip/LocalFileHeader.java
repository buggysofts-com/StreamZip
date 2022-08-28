package com.buggysofts.streamzip;

import lombok.Getter;
import lombok.ToString;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Getter
@ToString
class LocalFileHeader extends ChunkProperties {
    private final int signature;
    private final short version;
    private final short flags;
    private final short compression;
    private final short modTime;
    private final short modDate;
    private final int crc32;
    private long compressedSize;
    private long uncompressedSize;
    private final short fileNameLength;
    private final short extraFieldLength;
    private final String fileName;
    private final byte[] extraField;

    public LocalFileHeader(FileInputStream in, long startOffset, boolean isZip64) throws Exception {
        // reposition stream at chunk start
        in.getChannel().position(this.offset = startOffset);

        // according to the fixed length field sizes
        int fixedLengthDataLength = (4) + (2 * 5) + (4 * 3) + (2 * 2);
        byte[] fixedLengthData = StreamUtils.readFully(
            in,
            fixedLengthDataLength,
            false
        );
        if (fixedLengthData.length != fixedLengthDataLength) {
            // returned data length is less than usual - not a zip file/corrupted one?
            throw new Exception("Invalid local file header");
        } else {
            ByteBuffer fixedLengthDataBuffer =
                ByteBuffer.wrap(fixedLengthData)
                    .order(ByteOrder.LITTLE_ENDIAN);

            // for a valid zip file, signature will always be 0x04034b50 = 67324752
            this.signature = fixedLengthDataBuffer.getInt();
            if (this.signature != ZipConstants.SIG_LOCAL_FILE_HEADER) {
                // invalid header signature
                throw new Exception("Invalid local file header");
            }

            this.version = fixedLengthDataBuffer.getShort();
            this.flags = fixedLengthDataBuffer.getShort();
            this.compression = fixedLengthDataBuffer.getShort();
            this.modTime = fixedLengthDataBuffer.getShort();
            this.modDate = fixedLengthDataBuffer.getShort();
            this.crc32 = fixedLengthDataBuffer.getInt();
            // 0xFFFFFFFF if zip64, and it is stored in extraField
            this.compressedSize = fixedLengthDataBuffer.getInt();
            // 0xFFFFFFFF if zip64, and it is stored in extraField
            this.uncompressedSize = fixedLengthDataBuffer.getInt();
            this.fileNameLength = fixedLengthDataBuffer.getShort();
            this.extraFieldLength = fixedLengthDataBuffer.getShort();

            int variableLengthDataLength = fileNameLength + extraFieldLength;
            byte[] variableLengthData = StreamUtils.readFully(
                in,
                variableLengthDataLength,
                false
            );
            if (variableLengthData.length != variableLengthDataLength) {
                // returned data length is less than usual - not a zip file/corrupted one?
                throw new Exception("Malformed local file header");
            } else {
                ByteBuffer variableLengthDataBuffer =
                    ByteBuffer.wrap(variableLengthData)
                        .order(ByteOrder.LITTLE_ENDIAN);

                try {
                    byte[] fileNameData = new byte[fileNameLength];
                    variableLengthDataBuffer.get(fileNameData);
                    this.fileName = new String(fileNameData);

                    // if archive is in ZIP64 format, this field holds the compressed & uncompressed data length,
                    // local file header offset, number of disk on which the corresponding file exists.
                    // in that case these values will be -1 (0xFFFFFFFF in case of int, 0xFFFF in case of short)
                    // some other data may exist along with these two.
                    this.extraField = new byte[extraFieldLength];
                    variableLengthDataBuffer.get(extraField);

                    if (compressedSize == -1 || uncompressedSize == -1) {
                        ByteBuffer extraDataBuffer =
                            ByteBuffer.wrap(extraField)
                                .order(ByteOrder.LITTLE_ENDIAN);

                        // read out block that is not required
                        extraDataBuffer.get(new byte[4]);

                        if (uncompressedSize == -1) {
                            uncompressedSize = extraDataBuffer.getLong();
                        }
                        if (compressedSize == -1) {
                            compressedSize = extraDataBuffer.getLong();
                        }
                    }
                } catch (Exception e) {
                    throw new Exception("Malformed local file header");
                }
            }
        }
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public int getSize() {
        return (this.size = ((4) + (2 * 5) + (4 * 3) + (2 * 2)) + (fileNameLength + extraFieldLength));
    }
}
