package com.buggysofts.streamzip;

import lombok.Getter;
import lombok.ToString;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Getter
@ToString
class CentralDirFileHeader extends ChunkProperties {
    private final int signature;
    private final short creatorVersion;
    private final short extractorVersion;
    private final short flags;
    private final short compression;
    private final short modTime;
    private final short modDate;
    private final int crc32;
    private long compressedSize; // +ve
    private long uncompressedSize; // +ve
    private final short fileNameLength;
    private final short extraFieldLength;
    private final short fileCommentLength;
    private int diskIndexStart; // +ve
    private final short internalAttributes;
    private final int externalAttributes;
    private long localFileHeaderOffset; // +ve

    private final String fileName;
    private final byte[] extraField;
    private final String fileComment;

    public CentralDirFileHeader(FileInputStream in, long startOffset, boolean isZip64) throws Exception {
        // reposition stream at chunk start
        in.getChannel().position(this.offset = startOffset);

        // according to the fixed length field sizes
        int fixedLengthDataLength = (4) + (2 * 6) + (4 * 3) + (5 * 2) + (4 * 2);

        byte[] fixedLengthData = StreamUtils.readFully(
                in,
                fixedLengthDataLength,
                false
        );
        if (fixedLengthData.length != fixedLengthDataLength) {
            // returned data length is less than usual - not a zip file/corrupted one?
            throw new Exception("Malformed central directory file header");
        } else {
            ByteBuffer fixedLengthDataBuffer =
                    ByteBuffer.wrap(fixedLengthData)
                            .order(ByteOrder.LITTLE_ENDIAN);

            // for a valid zip file, signature will always be 0x04034b50 = 67324752
            this.signature = fixedLengthDataBuffer.getInt();
            if (this.signature != ZipConstants.SIG_CENTRAL_DIR_FILE_HEADER) {
                // invalid header signature
                throw new Exception("Malformed central directory file header");
            }

            this.creatorVersion = fixedLengthDataBuffer.getShort();
            this.extractorVersion = fixedLengthDataBuffer.getShort();
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
            this.fileCommentLength = fixedLengthDataBuffer.getShort();
            // 0xFFFF if zip64, and it is stored in extraField
            this.diskIndexStart = fixedLengthDataBuffer.getShort();
            this.internalAttributes = fixedLengthDataBuffer.getShort();
            this.externalAttributes = fixedLengthDataBuffer.getInt();
            // 0xFFFFFFFF if zip64, and it is stored in extraField
            this.localFileHeaderOffset = fixedLengthDataBuffer.getInt();

            int variableLengthDataLength = fileNameLength + extraFieldLength + fileCommentLength;
            byte[] variableLengthData = StreamUtils.readFully(
                    in,
                    variableLengthDataLength,
                    false
            );
            if (variableLengthData.length != variableLengthDataLength) {
                // returned data length is less than usual - not a zip file/corrupted one?
                throw new Exception("Malformed central directory file header");
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

                    // block for correcting size and offset values
                    if (compressedSize <= -1 || uncompressedSize <= -1 ||
                            localFileHeaderOffset <= -1 || diskIndexStart <= -1) {
                        // create a Zip64ExtInfoParser - we may have a zip 64 entry
                        Zip64ExtInfo parser = null;
                        try {
                            parser = new Zip64ExtInfo(extraField);
                        } catch (Exception e) {
                            // e.printStackTrace();
                        }
                        if (parser != null) {
                            // if one of them are -1, means we are dealing with zip 64, in that case fetch them from zip64 extra block
                            if (uncompressedSize == -1) {
                                uncompressedSize = parser.getTrimmedZip64InfoBuffer().getLong();
                            } else {
                                uncompressedSize = Integer.toUnsignedLong((int) uncompressedSize);
                            }
                            if (compressedSize == -1) {
                                compressedSize = parser.getTrimmedZip64InfoBuffer().getLong();
                            } else {
                                compressedSize = Integer.toUnsignedLong((int) compressedSize);
                            }
                            if (localFileHeaderOffset == -1) {
                                localFileHeaderOffset = parser.getTrimmedZip64InfoBuffer().getLong();
                            } else {
                                localFileHeaderOffset = Integer.toUnsignedLong((int) localFileHeaderOffset);
                            }
                            if (diskIndexStart == -1) {
                                diskIndexStart = parser.getTrimmedZip64InfoBuffer().getInt();
                            } else {
                                diskIndexStart = Short.toUnsignedInt((short) diskIndexStart);
                            }
                        } else {
                            // if they are not -1 but < 0, means we will have to convert them to unsigned
                            if (uncompressedSize != -1) {
                                uncompressedSize = Integer.toUnsignedLong((int) uncompressedSize);
                            }
                            if (compressedSize != -1) {
                                compressedSize = Integer.toUnsignedLong((int) compressedSize);
                            }
                            if (localFileHeaderOffset != -1) {
                                localFileHeaderOffset = Integer.toUnsignedLong((int) localFileHeaderOffset);
                            }
                            if (diskIndexStart == -1) {
                                diskIndexStart = Short.toUnsignedInt((short) diskIndexStart);
                            }
                        }
                    }

                    if (fileCommentLength > 0) {
                        byte[] fileCommentData = new byte[fileCommentLength];
                        variableLengthDataBuffer.get(fileCommentData);
                        this.fileComment = new String(fileCommentData);
                    } else {
                        this.fileComment = null;
                    }
                } catch (Exception e) {
                    System.out.println("ERR:" + this);
                    throw new Exception("Malformed central directory file header");
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
        return (this.size = ((4) + (2 * 6) + (4 * 3) + (5 * 2) + (4 * 2)) + (fileNameLength + fileCommentLength + extraFieldLength));
    }
}
