package com.github.seregamorph.maven.test.util;

import java.math.BigDecimal;

public final class TimeFormatUtils {

    public static String formatTime(BigDecimal seconds) {
        var secondsInt = seconds.intValue();
        if (secondsInt < 1) {
            return seconds + "s";
        }

        String result = (secondsInt % 60) + "s";
        secondsInt /= 60;
        if (secondsInt > 0) {
            result = (secondsInt % 60) + "m" + result;
            secondsInt /= 60;
            if (secondsInt > 0) {
                result = secondsInt + "h" + result;
            }
        }
        return result;
    }

    private TimeFormatUtils() {
    }
}
