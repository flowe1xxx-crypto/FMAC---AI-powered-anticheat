package dev.famesti.fmanticheat.dataset;

public enum DatasetLabel {
    LEGIT(0),
    CHEAT(1);

    private final int id;

    DatasetLabel(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static DatasetLabel parse(String raw) {
        if (raw == null) {
            return null;
        }
        String lowered = raw.trim().toLowerCase();
        if ("legit".equals(lowered) || "легит".equals(lowered) || "clean".equals(lowered)) {
            return LEGIT;
        }
        if ("cheat".equals(lowered) || "чит".equals(lowered) || "hack".equals(lowered)) {
            return CHEAT;
        }
        return null;
    }
}
