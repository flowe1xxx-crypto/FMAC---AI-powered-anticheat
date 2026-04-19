package dev.famesti.fmanticheat.detection;

public final class SanctionPolicy {

    private final int watchThresholdPercent;
    private final int lowThresholdPercent;
    private final int mediumThresholdPercent;
    private final int terminalThresholdPercent;
    private final int lowViolationGain;
    private final int mediumViolationGain;

    public SanctionPolicy(int watchThresholdPercent, int lowThresholdPercent, int mediumThresholdPercent,
                          int terminalThresholdPercent, int lowViolationGain, int mediumViolationGain) {
        this.watchThresholdPercent = clampPercent(watchThresholdPercent);
        this.lowThresholdPercent = clampPercent(lowThresholdPercent);
        this.mediumThresholdPercent = clampPercent(mediumThresholdPercent);
        this.terminalThresholdPercent = clampPercent(terminalThresholdPercent);
        this.lowViolationGain = clampViolation(lowViolationGain);
        this.mediumViolationGain = clampViolation(mediumViolationGain);
    }

    public Decision resolve(double probability, int currentViolationLevel) {
        int probabilityPercent = clampPercent((int) Math.round(probability * 100.0D));
        int safeCurrentVl = clampViolation(currentViolationLevel);
        boolean terminal = probabilityPercent >= terminalThresholdPercent;
        int violationGain = 0;
        if (terminal) {
            violationGain = Math.max(0, 100 - safeCurrentVl);
        } else if (probabilityPercent >= mediumThresholdPercent) {
            violationGain = mediumViolationGain;
        } else if (probabilityPercent >= lowThresholdPercent) {
            violationGain = lowViolationGain;
        }
        int updatedVl = Math.min(100, safeCurrentVl + violationGain);
        boolean suspicious = probabilityPercent >= watchThresholdPercent;
        boolean punish = terminal || updatedVl >= 100;
        PunishmentMode mode = terminal ? PunishmentMode.BAN : PunishmentMode.ALERT;
        return new Decision(probabilityPercent, violationGain, updatedVl, suspicious, terminal, punish, mode);
    }

    private int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private int clampViolation(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public static final class Decision {
        private final int probabilityPercent;
        private final int violationGain;
        private final int updatedViolationLevel;
        private final boolean suspicious;
        private final boolean terminal;
        private final boolean punish;
        private final PunishmentMode preferredMode;

        private Decision(int probabilityPercent, int violationGain, int updatedViolationLevel, boolean suspicious,
                         boolean terminal, boolean punish, PunishmentMode preferredMode) {
            this.probabilityPercent = probabilityPercent;
            this.violationGain = violationGain;
            this.updatedViolationLevel = updatedViolationLevel;
            this.suspicious = suspicious;
            this.terminal = terminal;
            this.punish = punish;
            this.preferredMode = preferredMode;
        }

        public int getProbabilityPercent() {
            return probabilityPercent;
        }

        public int getViolationGain() {
            return violationGain;
        }

        public int getUpdatedViolationLevel() {
            return updatedViolationLevel;
        }

        public boolean isSuspicious() {
            return suspicious;
        }

        public boolean isTerminal() {
            return terminal;
        }

        public boolean shouldPunish() {
            return punish;
        }

        public PunishmentMode getPreferredMode() {
            return preferredMode;
        }
    }
}
