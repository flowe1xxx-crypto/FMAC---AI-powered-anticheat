package dev.famesti.fmanticheat.detection;

import dev.famesti.fmanticheat.ui.InterfaceTheme;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class PerformanceValidationTest {

    @Test
    void resolvesThresholdsQuicklyUnderLoad() {
        SanctionPolicy policy = new SanctionPolicy(65, 65, 70, 94, 20, 60);

        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            for (int i = 0; i < 250_000; i++) {
                double probability = (i % 101) / 100.0D;
                policy.resolve(probability, i % 100);
            }
        });
    }

    @Test
    void rendersHudQuicklyAcrossMixedScenarios() {
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            for (int i = 0; i < 100_000; i++) {
                double probability = ((i * 7) % 101) / 100.0D;
                InterfaceTheme.formatWatcherHud("Bench", probability);
            }
        });
    }
}
