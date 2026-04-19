package dev.famesti.fmanticheat.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InterfaceThemeTest {

    @Test
    void formatsWatcherHudAcrossThresholds() {
        assertTrue(InterfaceTheme.formatWatcherHud("Flow", 0.64D).contains("CLEAR"));
        assertTrue(InterfaceTheme.formatWatcherHud("Flow", 0.65D).contains("WATCH"));
        assertTrue(InterfaceTheme.formatWatcherHud("Flow", 0.70D).contains("LOCK"));
        assertTrue(InterfaceTheme.formatWatcherHud("Flow", 0.94D).contains("BAN"));
    }

    @Test
    void formatsModelLineWithMinimalTheme() {
        String line = InterfaceTheme.formatModelLine("model", "Famesti-publik-45k");

        assertTrue(line.contains("model"));
        assertTrue(line.contains("Famesti-publik-45k"));
    }
}
