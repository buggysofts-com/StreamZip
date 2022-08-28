package com.buggysofts.streamzip;

import java.util.Date;

public final class DateTimeUtils {
    public static long convertMsDosDateTime(int time, int date) {
        int hr = time >> 11;
        int min = ((time << 21) >>> 26);
        int sec = (time & ((1 << 5) - 1)) << 1;
        int yr = 1980 + (date >> 9);
        int mo = ((date << 23) >>> 28);
        int day = date & ((1 << 5) - 1);
        return new Date(
            yr - 1900,
            mo - 1,
            day,
            hr - 1,
            min - 1,
            sec - 1
        ).getTime();
    }
}
