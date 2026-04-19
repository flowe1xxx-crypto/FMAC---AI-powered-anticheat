package dev.famesti.fmanticheat.ui;

import dev.famesti.fmanticheat.util.FormatUtil;

public final class InterfaceTheme {

    private InterfaceTheme() {
    }

    public static String formatModelLine(String label, String value) {
        return "&#7f8fa6" + padLabel(label) + " &8| &f" + value;
    }

    public static String formatWatcherHud(String playerName, double probability) {
        int percent = FormatUtil.percentInt(probability);
        return "&#f8f9fa&lFMAC &8| &f" + playerName
                + " &8| " + severityColor(percent) + "&l" + percent + "%"
                + " &8| " + progressMeter(percent)
                + " &8| " + severityLabel(percent);
    }

    public static String formatPunishmentAction(int progressPercent, int probabilityPercent) {
        return "&#f8f9fa&lFMAC &8| &#ff4d6dLockdown &8| "
                + progressMeter(probabilityPercent)
                + " &8| &#ffe066" + progressPercent + "%";
    }

    public static String formatAlertSuffix(int probabilityPercent, int updatedVl, int addedVl, boolean terminal) {
        String prefix = terminal ? "&#ff4d6dTERMINAL" : severityLabel(probabilityPercent);
        return prefix + " &8| &fvl=&e" + updatedVl + "/100 &8| &fdelta=&a+" + addedVl;
    }

    public static String severityLabel(int probabilityPercent) {
        if (probabilityPercent >= 94) {
            return "&#ff4d6d&lBAN";
        }
        if (probabilityPercent >= 70) {
            return "&#ff9f1c&lLOCK";
        }
        if (probabilityPercent >= 65) {
            return "&#ffd166&lWATCH";
        }
        return "&#90e0ef&lCLEAR";
    }

    private static String progressMeter(int probabilityPercent) {
        int safePercent = Math.max(0, Math.min(100, probabilityPercent));
        int filled = Math.max(1, (int) Math.round(safePercent / 20.0D));
        StringBuilder builder = new StringBuilder("&8[");
        for (int i = 0; i < 5; i++) {
            if (i < filled) {
                builder.append(severityColor(safePercent)).append("|");
            } else {
                builder.append("&7|");
            }
        }
        builder.append("&8]");
        return builder.toString();
    }

    private static String severityColor(int probabilityPercent) {
        if (probabilityPercent >= 94) {
            return "&#ff4d6d";
        }
        if (probabilityPercent >= 70) {
            return "&#ff9f1c";
        }
        if (probabilityPercent >= 65) {
            return "&#ffd166";
        }
        return "&#90e0ef";
    }

    private static String padLabel(String label) {
        String safe = label == null ? "" : label;
        StringBuilder builder = new StringBuilder(safe);
        while (builder.length() < 8) {
            builder.append(' ');
        }
        return builder.toString();
    }
}
