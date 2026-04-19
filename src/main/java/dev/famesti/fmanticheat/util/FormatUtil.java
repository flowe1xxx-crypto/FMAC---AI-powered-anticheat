package dev.famesti.fmanticheat.util;

import java.util.Locale;

public final class FormatUtil {

    private FormatUtil() {
    }

    public static String percent(double probability) {
        return String.format(Locale.US, "%.1f", probability * 100.0D);
    }

    public static int percentInt(double probability) {
        return (int) Math.round(probability * 100.0D);
    }

    public static String decimal(double value) {
        return String.format(Locale.US, "%.4f", value);
    }
}
