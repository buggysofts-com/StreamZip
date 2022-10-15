package com.buggysofts.streamzip;

import lombok.Getter;
import lombok.ToString;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Getter
@ToString
class Zip64EndOfCentralDirRecord extends ChunkProperties {
    private final int signature;
    private final long sizeofZip64ECDR;
    private final short creatorVersion;
    private final short extractorVersion;
    private final int diskIndex;
    private final int centralDirStartDiskIndex;
    private final long localCentralDirEntryCount;
    private final long globalCentralDirEntryCount;
    private final long centralDirSize;
    private final long centralDirStartOffset;

    public Zip64EndOfCentralDirRecord(FileInputStream in, long startOffset) throws Exception {
        // reposition stream at chunk start
        in.getChannel().position(this.offset = startOffset);

        // according to the fixed length field sizes
        int fixedLengthDataLength = (4) + (8) + (2 * 2) + (2 * 4) + (8 * 4);
        byte[] fixedDataChunk = StreamUtils.readFully(
                in,
                fixedLengthDataLength,
                false
        );
        if (fixedDataChunk.length < fixedLengthDataLength) {
            throw new Exception("Malformed Zip64EOCDR (Zip64-End-of-Central-Directory-Record");
        } else {
            ByteBuffer buffer =
                    ByteBuffer.wrap(fixedDataChunk)
                            .order(ByteOrder.LITTLE_ENDIAN);

            this.signature = buffer.getInt();
            this.sizeofZip64ECDR = buffer.getLong();
            this.creatorVersion = buffer.getShort();
            this.extractorVersion = buffer.getShort();
            this.diskIndex = buffer.getInt();
            this.centralDirStartDiskIndex = buffer.getInt();
            this.localCentralDirEntryCount = buffer.getLong();
            this.globalCentralDirEntryCount = buffer.getLong();
            this.centralDirSize = buffer.getLong();
            this.centralDirStartOffset = buffer.getLong();
        }
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public int getSize() {
        return ((4) + (2 * 4) + (4 * 2) + (2));
    }
}
