package com.github.seregamorph.maven.test.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author Sergey Chernov
 */
public final class TimeFormatUtils {

    public static BigDecimal toSeconds(long durationMillis) {
        return BigDecimal.valueOf(durationMillis).divide(BigDecimal.valueOf(1000L), 3, RoundingMode.HALF_UP);
    }

    public static String formatTime(BigDecimal seconds) {
        int secondsInt = seconds.intValue();
        if (secondsInt < 1) {
            return seconds + "s";
        }

        String result = (secondsInt % 60) + "s";
        secondsInt /= 60;
        if (secondsInt > 0) {
            result = (secondsInt % 60) + "m" + result;
            secondsInt /= 60;
            if (secondsInt > 0) {
                result = (secondsInt % 24) + "h" + result;
                secondsInt /= 24;
                if (secondsInt > 0) {
                    result = secondsInt + "d" + result;
                }
            }
        }
        return result;
    }

    private TimeFormatUtils() {
    }
}
