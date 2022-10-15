package com.buggysofts.streamzip;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class NonClosableInputStream extends BufferedInputStream {
    public NonClosableInputStream(@NotNull InputStream in) {
        super(in);
    }

    @Override
    public void close() throws IOException {
        // do not allow closing
    }
}
