package com.buggysofts.streamzip;

import lombok.Getter;
import lombok.ToString;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Getter
@ToString
class EndOfCentralDirRecord extends ChunkProperties {
    private final int signature;
    private final short diskIndex;
    private final short centralDirStartDiskIndex;
    private final short localCentralDirEntryCount;
    private final short globalCentralDirEntryCount;
    private final int centralDirSize;
    private long centralDirStartOffset; // +ve
    private final short zipFileCommentLength;

    private final String zipFileComment;

    public EndOfCentralDirRecord(FileInputStream in, long startOffset) throws Exception {
        // reposition stream at chunk start
        in.getChannel().position(this.offset = startOffset);

        // according to the fixed length field sizes
        int fixedLengthDataLength = (4) + (2 * 4) + (4 * 2) + (2);
        byte[] fixedDataChunk = StreamUtils.readFully(
            in,
            fixedLengthDataLength,
            false
        );
        if (fixedDataChunk.length < fixedLengthDataLength) {
            throw new Exception("Malformed EOCDR (End-of-Central-Directory-Record");
        } else {
            ByteBuffer buffer =
                ByteBuffer.wrap(fixedDataChunk)
                    .order(ByteOrder.LITTLE_ENDIAN);

            this.signature = buffer.getInt();
            this.diskIndex = buffer.getShort();
            this.centralDirStartDiskIndex = buffer.getShort();
            this.localCentralDirEntryCount = buffer.getShort();
            this.globalCentralDirEntryCount = buffer.getShort();
            this.centralDirSize = buffer.getInt();
            this.centralDirStartOffset = buffer.getInt();
            this.zipFileCommentLength = buffer.getShort();

            // fix if a negative value is in there for overflow error
            if (this.centralDirStartOffset < -1) {
                centralDirStartOffset = Integer.toUnsignedLong(
                    ((int) centralDirStartOffset)
                );
            }

            byte[] variableLengthData = StreamUtils.readFully(
                in,
                zipFileCommentLength,
                false
            );
            if (variableLengthData.length != zipFileCommentLength) {
                throw new Exception("Malformed EOCDR (End-of-Central-Directory-Record");
            } else {
                if (zipFileCommentLength > 0) {
                    this.zipFileComment = new String(
                        variableLengthData,
                        0,
                        zipFileCommentLength
                    );
                } else {
                    this.zipFileComment = null;
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
        return ((4) + (2 * 4) + (4 * 2) + (2)) + zipFileCommentLength;
    }
}
