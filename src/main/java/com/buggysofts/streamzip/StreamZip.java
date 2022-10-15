package com.buggysofts.streamzip;

import lombok.NonNull;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class StreamZip implements Closeable {
    private final FileInputStream sourceStream;

    private EndOfCentralDirRecord ecdRecord;
    private Zip64EndOfCentralDirRecord zip64EcdRecord;
    private Zip64EndOfCentralDirLocator zip64EcdLocator;

    private Map<String, LocalFileHeader> localFileHeaderMap;
    private Map<String, CentralDirFileHeader> centralDirFileHeaderMap;

    public StreamZip(@NonNull FileInputStream sourceStream) throws Exception {
        this.sourceStream = sourceStream;

        if (sourceStream.getChannel().size() < 22) {
            throw new Exception("File size too low to be a zip file.");
        }

        // initialize data
        initializeData();

        // extract all metadata
        extractMetadata();
    }

    private void initializeData() {
        localFileHeaderMap = new HashMap<>(0);
        centralDirFileHeaderMap = new HashMap<>(0);
    }

    private void extractMetadata() throws Exception {
        // get a channel handle
        FileChannel channel = sourceStream.getChannel();

        // search from end of the file for ECD(End of Central Directory) record signature.
        // As minimum ECDR size is 22 bytes, we can narrow our search by starting it from (file-size) - 22
        long ecdRecordPosition = getLastDataPosition(
                channel.size() - 22,
                22 + (1 << 16),
                ZipConstants.SIG_END_OF_CENTRAL_DIR_RECORD
        );
        if (ecdRecordPosition >= 0) {
            try {
                ecdRecord =
                        new EndOfCentralDirRecord(
                                sourceStream,
                                ecdRecordPosition
                        );
            } catch (Exception e) {
                throw new Exception(
                        String.format(
                                "%s - %s",
                                "Invalid zip file",
                                "Make sure you are working with a valid zip file."
                        )
                );
            }

            if (ecdRecord.getLocalCentralDirEntryCount() !=
                    ecdRecord.getGlobalCentralDirEntryCount()) {
                throw new Exception("Multi-Disk zip file is not yet supported.");
            } else {
                long zip64ecdLocatorPosition = getLastDataPosition(
                        ecdRecord.getOffset() - 1,
                        20,
                        ZipConstants.SIG_ZIP64_END_OF_CENTRAL_DIR_LOCATOR
                );
                if (zip64ecdLocatorPosition >= 0) {
                    try {
                        zip64EcdLocator =
                                new Zip64EndOfCentralDirLocator(
                                        sourceStream,
                                        zip64ecdLocatorPosition
                                );

                        zip64EcdRecord =
                                new Zip64EndOfCentralDirRecord(
                                        sourceStream,
                                        zip64EcdLocator.getRelativeZip64ECDROffset()
                                );
                    } catch (Exception e) {
                        throw new Exception(
                                String.format(
                                        "%s - %s",
                                        "Invalid zip file",
                                        "Make sure you are working with a valid zip file."
                                )
                        );
                    }

                    if (zip64EcdRecord.getLocalCentralDirEntryCount() !=
                            zip64EcdRecord.getGlobalCentralDirEntryCount()) {
                        throw new Exception("Multi-Disk zip file is not yet supported.");
                    } else {
                        // ZIP64-END-OF-CENTRAL-DIRECTORY-LOCATOR is available
                        long currentOffset = zip64EcdRecord.getCentralDirStartOffset();
                        for (int i = 0; i < zip64EcdRecord.getLocalCentralDirEntryCount(); ++i) {
                            CentralDirFileHeader currentDFH = new CentralDirFileHeader(
                                    sourceStream,
                                    currentOffset,
                                    true
                            );
                            centralDirFileHeaderMap.put(
                                    currentDFH.getFileName(),
                                    currentDFH
                            );
                            currentOffset += currentDFH.getSize();
                            // System.out.println(currentDFH.toString());
                        }
                        for (Map.Entry<String, CentralDirFileHeader> entry : centralDirFileHeaderMap.entrySet()) {
                            CentralDirFileHeader currentCFH = entry.getValue();
                            LocalFileHeader currentFH = new LocalFileHeader(
                                    sourceStream,
                                    currentCFH.getLocalFileHeaderOffset(),
                                    true
                            );
                            localFileHeaderMap.put(
                                    currentFH.getFileName(),
                                    currentFH
                            );
                            currentOffset += currentFH.getSize();
                            // System.out.println(currentFH.toString());
                        }
                    }
                } else {
                    // no ZIP64-END-OF-CENTRAL-DIRECTORY-LOCATOR is available
                    // we can rely on normal END-OF-CENTRAL-DIRECTORY
                    long currentOffset = ecdRecord.getCentralDirStartOffset();
                    for (int i = 0; i < ecdRecord.getLocalCentralDirEntryCount(); ++i) {
                        CentralDirFileHeader currentDFH = new CentralDirFileHeader(
                                sourceStream,
                                currentOffset,
                                false
                        );
                        centralDirFileHeaderMap.put(
                                currentDFH.getFileName(),
                                currentDFH
                        );
                        currentOffset += currentDFH.getSize();
                        // System.out.println(currentDFH.toString());
                    }

                    for (Map.Entry<String, CentralDirFileHeader> entry : centralDirFileHeaderMap.entrySet()) {
                        CentralDirFileHeader currentCFH = entry.getValue();
                        LocalFileHeader currentFH = new LocalFileHeader(
                                sourceStream,
                                currentCFH.getLocalFileHeaderOffset(),
                                false
                        );
                        localFileHeaderMap.put(
                                currentFH.getFileName(),
                                currentFH
                        );
                        currentOffset += currentFH.getSize();
                        // System.out.println(currentFH.toString());
                    }
                }
            }
        } else {
            throw new Exception(
                    String.format(
                            "%s - %s - %s",
                            "Invalid zip file",
                            "Could not find EOCDR(End of Central Directory Record)",
                            "Make sure you are working on a valid zip file."
                    )
            );
        }
    }

    /**
     * Get a particular entry.
     */
    public ZipEntry getEntry(@NonNull String name) {
        return new ZipEntry(centralDirFileHeaderMap.get(name));
    }

    /**
     * Get a list of all the entries available in the zip file.
     */
    public List<ZipEntry> entries() {
        List<ZipEntry> zipEntryList = new ArrayList<>(0);
        centralDirFileHeaderMap.forEach(
                new BiConsumer<String, CentralDirFileHeader>() {
                    @Override
                    public void accept(String s, CentralDirFileHeader centralDirFileHeader) {
                        zipEntryList.add(new ZipEntry(centralDirFileHeader));
                    }
                }
        );
        return zipEntryList;
    }

    /**
     * Get input stream for a particular entry.
     *
     * @throws Exception If the input stream can not be opened due to unavailability,
     *                   or if the entry is a directory entry, or the zip has been closed.
     */
    public InputStream getInputStream(@NonNull ZipEntry entry) throws Exception {
        // if the zip is closed, we can no longer access the stream.
        // however, other fields of this class (ZipStream) that does not depend on an
        // open connection and is initialized at object creation can still get cached values.
        if (!sourceStream.getChannel().isOpen()) {
            throw new Exception("Trying to get a stream from a closed zip.");
        }

        // check if the entry is not a directory
        // if so, throw an error
        if (entry.isDirectory()) {
            throw new Exception("Can not return stream for directory entry.");
        }

        // get header info
        LocalFileHeader localFileHeader = localFileHeaderMap.get(entry.getFileName());
        CentralDirFileHeader centralDirFileHeader = centralDirFileHeaderMap.get(entry.getFileName());

        if (localFileHeader != null && centralDirFileHeader != null) {
            // position the stream at the start of the entry data
            sourceStream.getChannel().position(
                    localFileHeader.getOffset() + localFileHeader.getSize()
            );

            if (centralDirFileHeader.getCompression() == 0) {
                // not deflated, just return the main stream with appropriate bound
                return new NonClosableInputStream(
                        new BoundedInputStream(
                                sourceStream,
                                centralDirFileHeader.getCompressedSize()
                        )
                );
            } else {
                // return a bounded input stream wrapped by an InflaterInputStream
                // to decompress the data while the caller is reading data.
                return new NonClosableInputStream(
                        new InflaterInputStream(
                                new BoundedInputStream(
                                        sourceStream,
                                        centralDirFileHeader.getCompressedSize()
                                ),
                                new Inflater(true)
                        )
                );
            }
        } else {
            throw new Exception("The requested zip entry was not found.");
        }
    }

    /**
     * Get number of available entries in this zip.
     */
    public int size() {
        return centralDirFileHeaderMap.size();
    }

    /**
     * Get global comment of the zip file.
     */
    @NonNull
    public String getComment() {
        return ecdRecord.getZipFileComment();
    }

    /**
     * Close the zip file. After this you won't be able to call {@code getInputStream()}.
     */
    @Override
    public void close() throws IOException {
        if (sourceStream != null) {
            try {
                sourceStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    /**
     * Search for the given data (any 32bit value) from the specified initial position,
     * up to the start of the file/stream (traverses in backward direction).
     * <br>
     * <br>
     * <b>Please note</b> - if the data is after(i.e. larger or >) the given initial position,
     * the search will not be able to find it.
     * <br>
     * <br>
     * Search starts from the given initial position, traverses data within the stream in decreasing
     * order of the position, and when it encounters an occurrence of the data, it returns the position
     * of the first byte of that occurrence, or -1 if not found within the searched region.
     *
     * @return Position of the last occurrence of the data, or -1 if not found.
     */
    private long getLastDataPosition(long initialPosition, int maxReadCount, int data) throws Exception {
        // keep a copy of the original position.
        // we will reposition the stream to the original position after we are done working.
        long currentPosition = initialPosition;
        int firstSignatureByte = (data & ((1 << 8) - 1));

        int readCount = 0;
        while (currentPosition >= 0) {
            // position the stream to currentPosition
            sourceStream.getChannel().position(currentPosition);

            // look for a signature candidate, if found or failed, break
            while (currentPosition >= 0 && readCount <= maxReadCount && sourceStream.read() != firstSignatureByte) {
                ++readCount;
                if (--currentPosition >= 0) {
                    sourceStream.getChannel().position(currentPosition);
                }
            }

            // if currentPosition < 0, we're at start of the file and couldn't find the first byte of the signature.
            // also, we will be here if we've read more than maxReadCount bytes of data (in reverse order).
            // apart from the above two cases, we've found a candidate.
            if (currentPosition < 0 || readCount > maxReadCount) {
                // reposition the stream to original position
                sourceStream.getChannel().position(initialPosition);
                // return -1 to indicate that the search was unsuccessful
                return -1;
            } else {
                sourceStream.getChannel().position(currentPosition);
                byte[] signatureData = StreamUtils.readFully(
                        sourceStream,
                        4,
                        false
                );

                if (ByteBuffer.wrap(signatureData).order(ByteOrder.LITTLE_ENDIAN).getInt() == data) {
                    // reposition the stream to original position
                    sourceStream.getChannel().position(initialPosition);
                    // return the position of the starting byte of the data
                    return currentPosition;
                } else {
                    --currentPosition;
                }
            }
        }

        // reposition the stream to original position
        sourceStream.getChannel().position(initialPosition);
        // not found
        return -1;
    }
}
