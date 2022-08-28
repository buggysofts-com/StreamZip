package com.buggysofts.streamzip;

import lombok.Getter;
import lombok.ToString;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Getter
@ToString
class Zip64EndOfCentralDirLocator extends ChunkProperties {
    private int signature;
    private int diskIndex;
    private long relativeZip64ECDROffset;
    private int diskCount;

    public Zip64EndOfCentralDirLocator(FileInputStream in, long startOffset) throws Exception {
        // reposition stream at chunk start
        in.getChannel().position(this.offset = startOffset);

        // according to the fixed length field sizes
        int fixedLengthDataLength = (2 * 4) + (8) + (4);
        byte[] fixedDataChunk = StreamUtils.readFully(
            in,
            fixedLengthDataLength,
            false
        );
        if (fixedDataChunk.length < fixedLengthDataLength) {
            throw new Exception("Malformed Zip64EOCDL (Zip64-End-of-Central-Directory-Locator");
        } else {
            ByteBuffer buffer =
                ByteBuffer.wrap(fixedDataChunk)
                    .order(ByteOrder.LITTLE_ENDIAN);

            this.signature = buffer.getInt();
            this.diskIndex = buffer.getInt();
            this.relativeZip64ECDROffset = buffer.getLong();
            this.diskCount = buffer.getInt();
        }
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public int getSize() {
        return (2 * 4) + (8) + (4);
    }
}
