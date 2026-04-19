package dev.famesti.fmanticheat.detection;

public enum PunishmentMode {
    BAN("ban"),
    KICK("kick"),
    ALERT("alert");

    private final String id;

    PunishmentMode(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static PunishmentMode parse(String raw) {
        if (raw == null) {
            return BAN;
        }
        for (PunishmentMode mode : values()) {
            if (mode.id.equalsIgnoreCase(raw)) {
                return mode;
            }
        }
        return BAN;
    }
}
