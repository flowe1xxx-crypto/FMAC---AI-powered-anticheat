package dev.famesti.fmanticheat.detection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SanctionPolicyTest {

    private final SanctionPolicy policy = new SanctionPolicy(65, 65, 70, 94, 20, 60);

    @Test
    void appliesTwentyViolationLevelsAtSixtyFivePercent() {
        SanctionPolicy.Decision decision = policy.resolve(0.65D, 0);

        assertTrue(decision.isSuspicious());
        assertEquals(20, decision.getViolationGain());
        assertEquals(20, decision.getUpdatedViolationLevel());
        assertFalse(decision.isTerminal());
        assertFalse(decision.shouldPunish());
    }

    @Test
    void appliesSixtyViolationLevelsAtSeventyPercent() {
        SanctionPolicy.Decision decision = policy.resolve(0.70D, 20);

        assertTrue(decision.isSuspicious());
        assertEquals(60, decision.getViolationGain());
        assertEquals(80, decision.getUpdatedViolationLevel());
        assertFalse(decision.isTerminal());
        assertFalse(decision.shouldPunish());
    }

    @Test
    void escalatesToBanAtNinetyFourPercent() {
        SanctionPolicy.Decision decision = policy.resolve(0.94D, 40);

        assertTrue(decision.isSuspicious());
        assertTrue(decision.isTerminal());
        assertEquals(60, decision.getViolationGain());
        assertEquals(100, decision.getUpdatedViolationLevel());
        assertTrue(decision.shouldPunish());
        assertEquals(PunishmentMode.BAN, decision.getPreferredMode());
    }
}
