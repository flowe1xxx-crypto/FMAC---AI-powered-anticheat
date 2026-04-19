package dev.famesti.fmanticheat.detection;

import dev.famesti.fmanticheat.util.FormatUtil;

public final class SanctionMessageFormatter {

    private SanctionMessageFormatter() {
    }

    public static String format(String sanctionReason, double probability, String telemetry,
                                int violationLevel, PunishmentMode mode) {
        String safeReason = sanctionReason == null ? "" : sanctionReason;
        String safeTelemetry = telemetry == null ? "tracking" : telemetry;
        return "&#ff4d6d&lFM ANTI-CHEAT\n"
                + "&#f8f9fa&lSanction Applied\n"
                + "&8&m------------------------------\n"
                + "&f" + safeReason + "\n"
                + "&fDetection confidence: &#ff9f1c&l" + FormatUtil.percent(probability) + "%\n"
                + "&fTelemetry: &#ff758f" + safeTelemetry + "\n"
                + "&fVL: &#ff9f1c&l" + violationLevel + "/100\n"
                + "&7Punishment: &c" + mode.getId().toUpperCase() + "\n"
                + "&8&m------------------------------";
    }
}
