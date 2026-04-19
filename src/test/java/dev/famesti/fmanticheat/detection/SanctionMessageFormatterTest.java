package dev.famesti.fmanticheat.detection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SanctionMessageFormatterTest {

    @Test
    void buildsMultilineKickBanReason() {
        String message = SanctionMessageFormatter.format(
                "Система подозревает вас в ЧИТАХ!\n                 Ложно? @flowe1x",
                0.943D,
                "tracking, cal=0.95",
                100,
                PunishmentMode.BAN
        );

        assertTrue(message.contains("Система подозревает вас в ЧИТАХ!"));
        assertTrue(message.contains("Ложно? @flowe1x"));
        assertTrue(message.contains("94.3%"));
        assertTrue(message.contains("tracking, cal=0.95"));
        assertTrue(message.contains("BAN"));
    }
}
