package com.buggysofts.streamzip;

import java.io.FileInputStream;

public class CleanableAction implements Runnable {
    private final FileInputStream fileInputStream;

    public CleanableAction(final FileInputStream fileInputStream) {
        this.fileInputStream = fileInputStream;
    }

    @Override
    public void run() {
        if (fileInputStream != null) {
            try {
                fileInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}