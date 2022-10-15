package com.buggysofts.streamzip;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

class Main {
    public static void main(String[] args) throws Exception {
        // test
        // test
        long s = System.currentTimeMillis();
        StreamZip zip = new StreamZip(
                new FileInputStream(
                        new File(
                                "/home/ragib/Desktop/addr.zip"
                        )
                )
        );
        List<ZipEntry> entries = zip.entries();
        long e = System.currentTimeMillis();
        for (int i = 0; i < entries.size(); ++i) {
            ZipEntry zipEntry = entries.get(i);
            if (!zipEntry.isDirectory()) {
                System.out.printf(
                        "\nFILE: %s --- %d --- %d - %s",
                        zipEntry.getFileName(),
                        zipEntry.getCompressedSize(),
                        zipEntry.getUncompressedSize(),
                        new String(StreamUtils.readFully(zip.getInputStream(zipEntry), Integer.MAX_VALUE, false))
                );
            } else {
                // do anything
                System.out.printf(
                        "\nDIR: %s",
                        zipEntry.getFileName()
                );
            }
        }
        System.out.println("\n\nTime taken: " + (e - s) + "ms");
    }

    private static String getDeflatedString(byte[] data) throws Exception {
        InflaterInputStream inflaterInputStream = new InflaterInputStream(
                new ByteArrayInputStream(data),
                new Inflater(true)
        );
        byte[] bytes = StreamUtils.readFully(inflaterInputStream, Integer.MAX_VALUE, false);
        return new String(bytes);
    }
}
