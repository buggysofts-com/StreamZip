package com.buggysofts.streamzip;

import lombok.Getter;
import lombok.ToString;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Getter
@ToString
class ArchiveExtraDataRecord extends ChunkProperties {
    private final int signature;
    private final int extraFieldLength;
    private final byte[] extraFieldData;

    public ArchiveExtraDataRecord(FileInputStream in, long startOffset) throws Exception {
        // reposition stream at chunk start
        in.getChannel().position(this.offset = startOffset);

        int fixedLengthDataLength = 2 * 4;
        byte[] fixedLengthData = StreamUtils.readFully(
                in,
                fixedLengthDataLength,
                false
        );
        if (fixedLengthData.length < fixedLengthDataLength) {
            throw new Exception("Malformed decryption header");
        } else {
            ByteBuffer buffer =
                    ByteBuffer.wrap(fixedLengthData)
                            .order(ByteOrder.LITTLE_ENDIAN);

            this.signature = buffer.getInt();
            this.extraFieldLength = buffer.getInt();

            this.extraFieldData = StreamUtils.readFully(
                    in,
                    extraFieldLength,
                    false
            );

            if (extraFieldData.length != extraFieldLength) {
                throw new Exception("Malformed decryption header");
            }
        }
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public int getSize() {
        return (this.size = (2 * 4) + extraFieldLength);
    }
}
