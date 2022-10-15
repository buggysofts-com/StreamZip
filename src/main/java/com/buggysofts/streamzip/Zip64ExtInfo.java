package com.buggysofts.streamzip;

import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Getter
@ToString
class Zip64ExtInfo extends ExtDataCommons {
    private final byte[] totalExtraData;

    private ByteBuffer trimmedZip64InfoBuffer;

    public Zip64ExtInfo(byte[] totalExtraData) throws Exception {
        this.totalExtraData = totalExtraData;

        // start parse
        findAndParse();
    }

    private void findAndParse() throws Exception {
        this.id = -1;

        // start from beginning, check header id and data size of each block,
        // skip them if necessary, do this until we get to the zip64 block.
        //
        // we can not just select the first id with 0x0001, coz that may not be
        // the actual starting 2 bytes(i.e. the header) of the zip64 info block.

        ByteBuffer scanBuffer =
                ByteBuffer.wrap(totalExtraData)
                        .order(ByteOrder.LITTLE_ENDIAN);

        while (scanBuffer.hasRemaining()) {
            int id = Short.toUnsignedInt(scanBuffer.getShort());
            int size = Short.toUnsignedInt(scanBuffer.getShort());
            if (id == 1) {
                this.id = 1;
                this.size = size;

                byte[] trimmedZip64InfoBufferData = new byte[size];
                if (scanBuffer.remaining() >= size) {
                    scanBuffer.get(trimmedZip64InfoBufferData);
                    trimmedZip64InfoBuffer =
                            ByteBuffer.wrap(trimmedZip64InfoBufferData)
                                    .order(ByteOrder.LITTLE_ENDIAN);
                } else {
                    throw new Exception("Corrupted Zip64 Extended Information Extra Field (0x0001)");
                }

                break;
            } else {
                // skip by reading the specified sized data
                scanBuffer.get(new byte[size]);
            }
        }

        if (id == -1) {
            throw new Exception("No Zip64 Extended Information Extra Field (0x0001)");
        }
    }
}
